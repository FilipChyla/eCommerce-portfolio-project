package io.github.filipchyla.shopapi.auth.controller;

import io.github.filipchyla.shopapi.auth.RefreshTokenCookieFactory;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationRequest;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationResponse;
import io.github.filipchyla.shopapi.auth.dto.RegisterRequest;
import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.auth.service.JwtService;
import io.github.filipchyla.shopapi.auth.service.RefreshTokenService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieFactory cookieFactory;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserPrincipal userPrincipal = authenticationService.register(request);

        UUID userId = userPrincipal.user().getId();

        String accessToken = jwtService.generateToken(userPrincipal);
        String refreshToken = refreshTokenService.createNewToken(userId.toString());

        ResponseCookie cookie = cookieFactory.create(refreshToken);

        URI location = URI.create("/api/v1/users/me");

        return ResponseEntity.created(location)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthenticationResponse(accessToken));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@Valid @RequestBody AuthenticationRequest request) {
        UserPrincipal userPrincipal = authenticationService.authenticate(request);
        UUID userId = userPrincipal.user().getId();

        String accessToken = jwtService.generateToken(userPrincipal);
        String refreshToken = refreshTokenService.createNewToken(userId.toString());

        ResponseCookie cookie = cookieFactory.create(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthenticationResponse(accessToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
            @CookieValue(name = "${app.refresh-token.cookie-name}") String rawRefreshToken) {

        String newRefreshToken = refreshTokenService.rotateToken(rawRefreshToken);

        UserPrincipal userPrincipal = getPrincipalFromToken(newRefreshToken);
        String accessToken = jwtService.generateToken(userPrincipal);

        ResponseCookie cookie = cookieFactory.create(newRefreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthenticationResponse(accessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.refresh-token.cookie-name}", required = false) String rawRefreshToken) {

        if (rawRefreshToken != null) {
            refreshTokenService.revokeToken(rawRefreshToken);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createExpired().toString())
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        refreshTokenService.revokeAllForUser(principal.user().getId().toString());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createExpired().toString())
                .build();
    }

    private UserPrincipal getPrincipalFromToken(String newRefreshToken) {
        String userId = refreshTokenService.getUserIdFromToken(newRefreshToken);
        User user = userService.getUserEntity(UUID.fromString(userId));
        return new UserPrincipal(user);
    }
}