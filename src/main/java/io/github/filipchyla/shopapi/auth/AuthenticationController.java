package io.github.filipchyla.shopapi.auth;

import io.github.filipchyla.shopapi.auth.dto.AuthenticationRequest;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationResponse;
import io.github.filipchyla.shopapi.auth.dto.RegisterRequest;
import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.auth.service.JwtService;
import io.github.filipchyla.shopapi.auth.service.RefreshTokenService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.shared.dto.MessageResponse;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;



@Tag(name = "Authentication", description = "Operations related to authentication and refresh tokens lifecycle")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieFactory cookieFactory;
    private final UserService userService;

    @Operation(
            summary = "Register new account",
            description = "Create new account and return tokens"
    )
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserPrincipal userPrincipal = authenticationService.register(request);

        UUID userId = userPrincipal.user().getId();

        String accessToken = jwtService.generateToken(userPrincipal);
        String refreshToken = refreshTokenService.createNewToken(userId.toString());

        ResponseCookie cookie = cookieFactory.create(refreshToken);

        URI location = URI.create("/api/v1/user/me");

        return ResponseEntity.created(location)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthenticationResponse(accessToken));
    }

    @Operation(
            summary = "Authenticate user"
    )
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

    @Operation(
            summary = "Refresh jwt token"
    )
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

    @Operation(
            summary = "Invalidate given refresh token"
    )
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = "${app.refresh-token.cookie-name}", required = false) String rawRefreshToken) {

        if (rawRefreshToken != null) {
            refreshTokenService.revokeToken(rawRefreshToken);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createExpired().toString())
                .body(new MessageResponse("User logged out successfully"));
    }

    @Operation(
            summary = "Invalidate all refresh tokens for authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout-all")
    public ResponseEntity<MessageResponse> logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        refreshTokenService.revokeAllForUser(principal.user().getId().toString());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createExpired().toString())
                .body(new MessageResponse("User logged out from all devices successfully"));
    }

    private UserPrincipal getPrincipalFromToken(String newRefreshToken) {
        String userId = refreshTokenService.getUserIdFromToken(newRefreshToken);
        User user = userService.getUserEntity(UUID.fromString(userId));
        return new UserPrincipal(user);
    }
}