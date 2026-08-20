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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @GetMapping("/fix-db")
    public ResponseEntity<String> fixDb() {
        StringBuilder result = new StringBuilder();
        try {
            jdbcTemplate.execute("ALTER TABLE utilisateurs ADD COLUMN active BOOLEAN DEFAULT true");
            result.append("Colonne 'active' ajoutée.\n");
        } catch (Exception e) {
            result.append("Colonne 'active' déjà présente.\n");
        }
        try {
            jdbcTemplate.execute("ALTER TABLE utilisateurs ADD COLUMN force_password_change BOOLEAN DEFAULT true");
            result.append("Colonne 'force_password_change' ajoutée.\n");
        } catch (Exception e) {
            result.append("Colonne 'force_password_change' déjà présente.\n");
        }
        try {
            jdbcTemplate.execute("UPDATE departements SET workflow_template_id = (SELECT id FROM modeles_workflow WHERE name LIKE '%Technique%' LIMIT 1) WHERE name LIKE '%ALVANET%' OR name LIKE '%SLF%' OR name LIKE '%SCR%'");
            jdbcTemplate.execute("UPDATE departements SET workflow_template_id = (SELECT id FROM modeles_workflow WHERE name LIKE '%Générale%' LIMIT 1) WHERE name NOT LIKE '%ALVANET%' AND name NOT LIKE '%SLF%' AND name NOT LIKE '%SCR%'");
            result.append("Workflows techniques (avec DT) et généraux (sans DT) mis à jour.\n");
        } catch (Exception e) {
            result.append("Erreur workflow: ").append(e.getMessage()).append("\n");
        }
        try {
            jdbcTemplate.execute("UPDATE departements SET name = REPLACE(name, 'Direction Technique - ', '')");
            jdbcTemplate.execute("UPDATE departements SET name = REPLACE(name, 'Direction Générale - ', '')");
            jdbcTemplate.execute("UPDATE departements SET name = REPLACE(name, 'Direction technique - ', '')");
            jdbcTemplate.execute("UPDATE departements SET name = REPLACE(name, 'Direction générale - ', '')");
            jdbcTemplate.execute("UPDATE departements SET name = 'RH (Ressources Humaines)' WHERE name = 'RH'");
            result.append("Noms des départements nettoyés.\n");
            
            java.util.List<String> deps = jdbcTemplate.queryForList("SELECT name FROM departements", String.class);
            result.append("Départements actuels: ").append(deps.toString()).append("\n");
        } catch (Exception e) {
            result.append("Erreur renommer: ").append(e.getMessage()).append("\n");
        }
        return ResponseEntity.ok(result.toString());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse response = authService.authenticate(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
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
