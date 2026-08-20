package com.ids.expense.service;

import com.ids.expense.common.models.User;
import com.ids.expense.common.models.Department;
import com.ids.expense.common.repository.UserRepository;
import com.ids.expense.common.repository.DepartmentRepository;
import com.ids.expense.common.repository.ExpenseReportRepository;
import com.ids.expense.common.repository.ExpenseApprovalHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import com.ids.expense.common.models.RoleType;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ExpenseReportRepository expenseReportRepository;
    private final ExpenseApprovalHistoryRepository expenseApprovalHistoryRepository;
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
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().replaceAll("\\s+", "").trim());
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode("password"));
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
        }
        if (user.getDepartment() != null && user.getDepartment().getId() != null) {
            Department dept = departmentRepository.findById(user.getDepartment().getId())
                    .orElseThrow(() -> new RuntimeException("Département introuvable avec l'ID: " + user.getDepartment().getId()));
            user.setDepartment(dept);
        } else {
            user.setDepartment(null);
        }
        if (user.getActive() == null) {
            user.setActive(true);
        }
        if (user.getForcePasswordChange() == null) {
            user.setForcePasswordChange(true);
        }
        if (RoleType.ADMIN.equals(user.getRole())) {
            user.setForcePasswordChange(false);
        }
        User savedUser = userRepository.save(user);

        if (RoleType.MANAGER.equals(savedUser.getRole()) && savedUser.getDepartment() != null) {
            Department targetDept = savedUser.getDepartment();
            if (targetDept.getManager() != null && !targetDept.getManager().getId().equals(savedUser.getId())) {
                User oldManager = targetDept.getManager();
                if (RoleType.MANAGER.equals(oldManager.getRole())) {
                    oldManager.setRole(RoleType.EMPLOYEE);
                    userRepository.save(oldManager);
                }
            }
            targetDept.setManager(savedUser);
            departmentRepository.save(targetDept);
        }

        return savedUser;
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        user.setName(userDetails.getName() != null ? userDetails.getName().trim() : user.getName());
        if (userDetails.getEmail() != null) {
            user.setEmail(userDetails.getEmail().replaceAll("\\s+", "").trim());
        }
        if (userDetails.getPassword() != null && !userDetails.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword().trim()));
        }
        user.setRole(userDetails.getRole());
        if (userDetails.getDepartment() != null && userDetails.getDepartment().getId() != null) {
            Department dept = departmentRepository.findById(userDetails.getDepartment().getId())
                    .orElseThrow(() -> new RuntimeException("Département introuvable avec l'ID: " + userDetails.getDepartment().getId()));
            user.setDepartment(dept);
        } else {
            user.setDepartment(null);
        }
        User savedUser = userRepository.save(user);

        if (RoleType.MANAGER.equals(savedUser.getRole())) {
            departmentRepository.findByManagerId(savedUser.getId()).ifPresent(oldDept -> {
                if (savedUser.getDepartment() == null || !oldDept.getId().equals(savedUser.getDepartment().getId())) {
                    oldDept.setManager(null);
                    departmentRepository.save(oldDept);
                }
            });

            if (savedUser.getDepartment() != null) {
                Department targetDept = departmentRepository.findById(savedUser.getDepartment().getId()).orElse(null);
                if (targetDept != null) {
                    if (targetDept.getManager() != null && !targetDept.getManager().getId().equals(savedUser.getId())) {
                        User oldManager = targetDept.getManager();
                        if (RoleType.MANAGER.equals(oldManager.getRole())) {
                            oldManager.setRole(RoleType.EMPLOYEE);
                            userRepository.save(oldManager);
                        }
                    }
                    targetDept.setManager(savedUser);
                    departmentRepository.save(targetDept);
                }
            }
        } else {
            departmentRepository.findByManagerId(savedUser.getId()).ifPresent(oldDept -> {
                oldDept.setManager(null);
                departmentRepository.save(oldDept);
            });
        }

        return savedUser;
    }

    @Transactional
    public User toggleUserActive(Long id) {
        User user = getUserById(id);
        user.setActive(!user.isActive());
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        if (expenseReportRepository.existsByEmployeeId(id)) {
            throw new RuntimeException("Cet utilisateur ne peut pas être supprimé car il possède des notes de frais. Vous pouvez plutôt le désactiver.");
        }
        if (expenseApprovalHistoryRepository.existsByApproverId(id)) {
            throw new RuntimeException("Cet utilisateur ne peut pas être supprimé car il a validé ou rejeté des notes de frais. Vous pouvez plutôt le désactiver.");
        }
        departmentRepository.findByManagerId(id).ifPresent(dept -> {
            dept.setManager(null);
            departmentRepository.save(dept);
        });
        userRepository.delete(user);
    }
}
