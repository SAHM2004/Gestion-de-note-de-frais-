package com.ids.expense.service;

import com.ids.expense.common.models.Department;
import com.ids.expense.common.models.RoleType;
import com.ids.expense.common.models.User;
import com.ids.expense.common.models.WorkflowTemplate;
import com.ids.expense.common.repository.DepartmentRepository;
import com.ids.expense.common.repository.UserRepository;
import com.ids.expense.common.repository.WorkflowTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final WorkflowTemplateRepository workflowTemplateRepository;

    public Page<Department> getAllDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable);
    }

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Département introuvable avec l'ID: " + id));
    }

    @Transactional
    public Department createDepartment(Department department) {
        User manager = null;
        if (department.getManager() != null && department.getManager().getId() != null) {
            manager = userRepository.findById(department.getManager().getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur (manager) introuvable avec l'ID: " + department.getManager().getId()));

            departmentRepository.findByManagerId(manager.getId()).ifPresent(otherDept -> {
                otherDept.setManager(null);
                departmentRepository.save(otherDept);
            });
            manager.setRole(RoleType.MANAGER);
        }
        department.setManager(null);

        if (department.getDefaultWorkflowTemplate() != null && department.getDefaultWorkflowTemplate().getId() != null) {
            WorkflowTemplate template = workflowTemplateRepository.findById(department.getDefaultWorkflowTemplate().getId())
                    .orElseThrow(() -> new RuntimeException("WorkflowTemplate introuvable avec l'ID: " + department.getDefaultWorkflowTemplate().getId()));
            department.setDefaultWorkflowTemplate(template);
        } else {
            department.setDefaultWorkflowTemplate(null);
        }

        Department savedDepartment = departmentRepository.save(department);
        if (manager != null) {
            savedDepartment.setManager(manager);
            manager.setDepartment(savedDepartment);
            userRepository.save(manager);
            savedDepartment = departmentRepository.save(savedDepartment);
        }
        return savedDepartment;
    }

    @Transactional
    public Department updateDepartment(Long id, Department departmentDetails) {
        Department department = getDepartmentById(id);
        User oldManager = department.getManager();

        if (departmentDetails.getName() != null) {
            department.setName(departmentDetails.getName());
        }

        if (departmentDetails.getManager() != null && departmentDetails.getManager().getId() != null) {
            User newManager = userRepository.findById(departmentDetails.getManager().getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur (manager) introuvable avec l'ID: " + departmentDetails.getManager().getId()));

            if (oldManager != null && !oldManager.getId().equals(newManager.getId())) {
                if (oldManager.getRole() == RoleType.MANAGER) {
                    oldManager.setRole(RoleType.EMPLOYEE);
                    userRepository.save(oldManager);
                }
            }

            departmentRepository.findByManagerId(newManager.getId()).ifPresent(otherDept -> {
                if (!otherDept.getId().equals(id)) {
                    otherDept.setManager(null);
                    departmentRepository.save(otherDept);
                }
            });

            newManager.setRole(RoleType.MANAGER);
            newManager.setDepartment(department);
            userRepository.save(newManager);

            department.setManager(newManager);
        } else {
            if (oldManager != null && oldManager.getRole() == RoleType.MANAGER) {
                oldManager.setRole(RoleType.EMPLOYEE);
                userRepository.save(oldManager);
            }
            department.setManager(null);
        }

        if (departmentDetails.getDefaultWorkflowTemplate() != null && departmentDetails.getDefaultWorkflowTemplate().getId() != null) {
            WorkflowTemplate template = workflowTemplateRepository.findById(departmentDetails.getDefaultWorkflowTemplate().getId())
                    .orElseThrow(() -> new RuntimeException("WorkflowTemplate introuvable avec l'ID: " + departmentDetails.getDefaultWorkflowTemplate().getId()));
            department.setDefaultWorkflowTemplate(template);
        } else {
            department.setDefaultWorkflowTemplate(null);
        }

        return departmentRepository.save(department);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department department = getDepartmentById(id);

        List<User> members = userRepository.findByDepartmentId(id);
        for (User u : members) {
            if (u.getRole() == RoleType.MANAGER) {
                u.setRole(RoleType.EMPLOYEE);
            }
            u.setDepartment(null);
            userRepository.save(u);
        }

        department.setManager(null);
        departmentRepository.save(department);

        departmentRepository.delete(department);
    }
}
