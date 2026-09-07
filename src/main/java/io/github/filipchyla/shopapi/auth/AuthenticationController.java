package io.github.filipchyla.shopapi.auth;

import io.github.filipchyla.shopapi.auth.dto.AuthenticationRequest;
import io.github.filipchyla.shopapi.auth.dto.AccessTokenData;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationTokensData;
import io.github.filipchyla.shopapi.auth.dto.RegisterRequest;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.shared.dto.MessageResponse;
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


@Tag(name = "Authentication", description = "Operations related to authentication and refresh tokens lifecycle")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationFacade authenticationFacade;

    @Operation(
            summary = "Register new account",
            description = "Create new account and return tokens"
    )
    @PostMapping("/register")
    public ResponseEntity<AccessTokenData> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticationTokensData data = authenticationFacade.registerUserAndReturnTokens(request);

        URI location = URI.create("/api/v1/user/me");

        return ResponseEntity.created(location)
                .header(HttpHeaders.SET_COOKIE, data.refreshTokenCookie().toString())
                .body(data.accessToken());
    }

    @Operation(
            summary = "Authenticate user"
    )
    @PostMapping("/authenticate")
    public ResponseEntity<AccessTokenData> authenticate(@Valid @RequestBody AuthenticationRequest request) {
        AuthenticationTokensData data = authenticationFacade.authenticateUserAndReturnTokens(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, data.refreshTokenCookie().toString())
                .body(data.accessToken());
    }

    @Operation(
            summary = "Refresh jwt token"
    )
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenData> refresh(
            @CookieValue(name = "${app.refresh-token.cookie-name}") String rawRefreshToken) {
        AuthenticationTokensData data = authenticationFacade.refreshToken(rawRefreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, data.refreshTokenCookie().toString())
                .body(data.accessToken());
    }

    @Operation(
            summary = "Invalidate given refresh token"
    )
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = "${app.refresh-token.cookie-name}", required = false) String rawRefreshToken) {
        ResponseCookie cookie = authenticationFacade.revokeSingleForUser(rawRefreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("User logged out successfully"));
    }

    @Operation(
            summary = "Invalidate all refresh tokens for authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout-all")
    public ResponseEntity<MessageResponse> logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        ResponseCookie cookie = authenticationFacade.revokeAllTokensForUser(principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("User logged out from all devices successfully"));
    }
}