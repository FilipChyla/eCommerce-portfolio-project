package io.github.filipchyla.shopapi.user.controller;

import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.user.dto.ChangePasswordRequest;
import io.github.filipchyla.shopapi.user.dto.MessageResponse;
import io.github.filipchyla.shopapi.user.dto.PatchUserRequest;
import io.github.filipchyla.shopapi.user.dto.UserResponse;
import io.github.filipchyla.shopapi.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @GetMapping
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getUser(principal.user().getId()));
    }

    @PatchMapping
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal UserPrincipal principal, @RequestBody @Valid PatchUserRequest request) {
        return ResponseEntity.ok(userService.patchUser(request, principal.user().getId()));
    }

    @PatchMapping("/password")
    public ResponseEntity<MessageResponse> updatePassword(@AuthenticationPrincipal UserPrincipal principal, @RequestBody @Valid ChangePasswordRequest request) {
        authenticationService.changePassword(principal.user().getId(), request);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }

    @DeleteMapping
    public ResponseEntity<MessageResponse> deleteMe(@AuthenticationPrincipal UserPrincipal principal) {
        userService.deactivateUser(principal.user().getId());
        return ResponseEntity.ok(new MessageResponse("User deactivated successfully"));
    }
}