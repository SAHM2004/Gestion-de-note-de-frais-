package com.ids.expense.auth;

import com.ids.expense.auth.dto.AuthResponse;
import com.ids.expense.auth.dto.ChangePasswordRequest;
import com.ids.expense.auth.dto.LoginRequest;
import com.ids.expense.auth.dto.UpdateProfileRequest;
import com.ids.expense.auth.dto.UserProfileResponse;
import com.ids.expense.auth.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug-auth")
    public ResponseEntity<java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>> debugAuth() {
        return ResponseEntity.ok(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities());
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestBody ChangePasswordRequest request) {
        
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        
        authService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(authService.getProfile(currentUser.getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestBody UpdateProfileRequest request) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(authService.updateProfile(currentUser.getId(), request));
    }
}
