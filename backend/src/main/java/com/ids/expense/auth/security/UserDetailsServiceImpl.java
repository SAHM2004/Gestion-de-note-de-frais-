package com.ids.expense.auth.security;

import com.ids.expense.common.models.User;
import com.ids.expense.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String cleanEmail = email != null ? email.replaceAll("\\s+", "").trim() : "";
        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(email != null ? email.trim() : "")
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> u.getEmail() != null && u.getEmail().replaceAll("\\s+", "").equalsIgnoreCase(cleanEmail))
                        .findFirst()
                        .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email))));

        return UserDetailsImpl.build(user);
    }
}
