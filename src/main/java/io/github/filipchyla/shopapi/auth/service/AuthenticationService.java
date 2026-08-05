package io.github.filipchyla.shopapi.auth.service;

import io.github.filipchyla.shopapi.auth.dto.AuthenticationRequest;
import io.github.filipchyla.shopapi.auth.dto.RegisterRequest;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.user.dto.ChangePasswordRequest;
import io.github.filipchyla.shopapi.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public UserPrincipal register(RegisterRequest request) {
        userService.createUser(
                request.email(),
                passwordEncoder.encode(request.password())
        );

        return authenticate(new AuthenticationRequest(
                request.email(),
                request.password()
        ));
    }

    public UserPrincipal authenticate(AuthenticationRequest request) {
         Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        return (UserPrincipal) auth.getPrincipal();
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = userService.getUserEntity(id);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }
}