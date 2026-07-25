package com.ids.expense.service;

import com.ids.expense.common.models.User;
import com.ids.expense.common.models.Department;
import com.ids.expense.common.repository.UserRepository;
import com.ids.expense.common.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'ID: " + id));
    }

    @Transactional
    public User createUser(User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getDepartment() != null && user.getDepartment().getId() != null) {
            Department dept = departmentRepository.findById(user.getDepartment().getId())
                    .orElseThrow(() -> new RuntimeException("Département introuvable avec l'ID: " + user.getDepartment().getId()));
            user.setDepartment(dept);
        } else {
            user.setDepartment(null);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }
        user.setRole(userDetails.getRole());
        if (userDetails.getDepartment() != null && userDetails.getDepartment().getId() != null) {
            Department dept = departmentRepository.findById(userDetails.getDepartment().getId())
                    .orElseThrow(() -> new RuntimeException("Département introuvable avec l'ID: " + userDetails.getDepartment().getId()));
            user.setDepartment(dept);
        } else {
            user.setDepartment(null);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }
}
