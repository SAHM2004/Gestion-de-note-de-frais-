package com.ids.expense.auth;

import com.ids.expense.auth.dto.AuthResponse;
import com.ids.expense.auth.dto.ChangePasswordRequest;
import com.ids.expense.auth.dto.LoginRequest;
import com.ids.expense.auth.dto.UpdateProfileRequest;
import com.ids.expense.auth.dto.UserProfileResponse;
import com.ids.expense.auth.security.JwtUtil;
import com.ids.expense.auth.security.UserDetailsImpl;
import com.ids.expense.common.models.User;
import com.ids.expense.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse authenticate(LoginRequest request) {
        Authentication authentication;
        String email = request.getEmail() != null ? request.getEmail().replaceAll("\\s+", "").trim() : "";
        String pass = request.getPassword() != null ? request.getPassword().trim() : "";
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, pass)
            );
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new RuntimeException("Votre compte a été désactivé. Veuillez contacter l'administrateur.");
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new RuntimeException("Identifiants incorrects. Vérifiez votre e-mail et mot de passe.");
        } catch (Exception e) {
            throw new RuntimeException("Erreur de connexion : " + (e.getMessage() != null ? e.getMessage() : "Identifiants invalides"));
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(userDetails);

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        String departmentName = user.getDepartment() != null ? user.getDepartment().getName() : null;

        return new AuthResponse(
                jwt,
                userDetails.getId(),
                userDetails.getName(),
                userDetails.getEmail(),
                userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""),
                userDetails.isForcePasswordChange(),
                departmentId,
                departmentName
        );
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("L'ancien mot de passe est incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setForcePasswordChange(false); // Le mot de passe a été changé par l'utilisateur
        userRepository.save(user);
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return toProfileResponse(user);
    }

    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        userRepository.save(user);
        return toProfileResponse(user);
    }

    private UserProfileResponse toProfileResponse(User user) {
        Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : null;
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                deptId,
                deptName
        );
    }
}
