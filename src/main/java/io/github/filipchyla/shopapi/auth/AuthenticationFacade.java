package io.github.filipchyla.shopapi.auth;

import io.github.filipchyla.shopapi.auth.dto.AccessTokenData;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationRequest;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationTokensData;
import io.github.filipchyla.shopapi.auth.dto.RegisterRequest;
import io.github.filipchyla.shopapi.auth.exception.UserDisabledException;
import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.auth.service.JwtService;
import io.github.filipchyla.shopapi.auth.service.RefreshTokenService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class AuthenticationFacade {
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieFactory cookieFactory;
    private final UserService userService;

    public AuthenticationTokensData registerUserAndReturnTokens(RegisterRequest request){
        UserPrincipal userPrincipal = authenticationService.register(request);

        return generateTokensForUser(userPrincipal);
    }

    public AuthenticationTokensData authenticateUserAndReturnTokens(AuthenticationRequest request) {
        UserPrincipal userPrincipal = authenticationService.authenticate(request);

        return generateTokensForUser(userPrincipal);
    }

    public AuthenticationTokensData refreshToken(String rawRefreshToken) {
        UserPrincipal userPrincipal = getPrincipalFromToken(rawRefreshToken);
        if (!userPrincipal.isEnabled()) throw new UserDisabledException("Account is deactivated");

        String newRefreshToken = refreshTokenService.rotateToken(rawRefreshToken);
        String accessToken = jwtService.generateToken(userPrincipal);

        ResponseCookie cookie = cookieFactory.create(newRefreshToken);
        AccessTokenData accessTokenData = new AccessTokenData(accessToken);

        return new AuthenticationTokensData(accessTokenData, cookie);
    }

    public ResponseCookie revokeSingleForUser(String rawRefreshToken) {
        if (rawRefreshToken != null) {
            refreshTokenService.revokeToken(rawRefreshToken);
        }

        return cookieFactory.createExpired();
    }

    public ResponseCookie revokeAllTokensForUser(UserPrincipal principal) {
        refreshTokenService.revokeAllForUser(principal.user().getId().toString());

        return cookieFactory.createExpired();
    }

    private UserPrincipal getPrincipalFromToken(String newRefreshToken) {
        String userId = refreshTokenService.getUserIdFromToken(newRefreshToken);
        User user = userService.getUserEntity(UUID.fromString(userId));
        return new UserPrincipal(user);
    }

    private AuthenticationTokensData generateTokensForUser(UserPrincipal principal){
        UUID userId = principal.user().getId();

        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.createNewToken(userId.toString());

        AccessTokenData accessTokenData = new AccessTokenData(accessToken);
        ResponseCookie cookie = cookieFactory.create(refreshToken);

        return new AuthenticationTokensData(accessTokenData, cookie);
    }
}
