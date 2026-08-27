package io.github.filipchyla.shopapi.user.controller;

import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.user.dto.ChangePasswordRequest;
import io.github.filipchyla.shopapi.shared.dto.MessageResponse;
import io.github.filipchyla.shopapi.user.dto.PatchUserRequest;
import io.github.filipchyla.shopapi.user.dto.UserResponse;
import io.github.filipchyla.shopapi.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "Operations related to user account")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/user/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @Operation(
            summary = "Get authenticated user's profile information"
    )
    @GetMapping
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getUser(principal.user().getId()));
    }

    @Operation(
            summary = "Update authenticated user's profile information",
            description = "Updates only the fields that are provided in the request body"
    )
    @PatchMapping
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal UserPrincipal principal, @RequestBody @Valid PatchUserRequest request) {
        return ResponseEntity.ok(userService.patchUser(request, principal.user().getId()));
    }

    @Operation(
            summary = "Change authenticated user's password"
    )
    @PatchMapping("/password")
    public ResponseEntity<MessageResponse> updatePassword(@AuthenticationPrincipal UserPrincipal principal, @RequestBody @Valid ChangePasswordRequest request) {
        authenticationService.changePassword(principal.user().getId(), request);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }

    @Operation(
            summary = "Delete authenticated user's account. This is a soft delete"
    )
    @DeleteMapping
    public ResponseEntity<MessageResponse> deleteMe(@AuthenticationPrincipal UserPrincipal principal) {
        userService.deactivateUser(principal.user().getId());
        return ResponseEntity.ok(new MessageResponse("User deactivated successfully"));
    }
}