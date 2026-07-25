package com.ids.expense.service;

import com.ids.expense.common.models.Department;
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
        if (department.getManager() != null && department.getManager().getId() != null) {
            User manager = userRepository.findById(department.getManager().getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur (manager) introuvable avec l'ID: " + department.getManager().getId()));
            department.setManager(manager);
        } else {
            department.setManager(null);
        }

        if (department.getDefaultWorkflowTemplate() != null && department.getDefaultWorkflowTemplate().getId() != null) {
            WorkflowTemplate template = workflowTemplateRepository.findById(department.getDefaultWorkflowTemplate().getId())
                    .orElseThrow(() -> new RuntimeException("WorkflowTemplate introuvable avec l'ID: " + department.getDefaultWorkflowTemplate().getId()));
            department.setDefaultWorkflowTemplate(template);
        } else {
            department.setDefaultWorkflowTemplate(null);
        }

        return departmentRepository.save(department);
    }

    @Transactional
    public Department updateDepartment(Long id, Department departmentDetails) {
        Department department = getDepartmentById(id);
        if (departmentDetails.getName() != null) {
            department.setName(departmentDetails.getName());
        }

        if (departmentDetails.getManager() != null && departmentDetails.getManager().getId() != null) {
            User manager = userRepository.findById(departmentDetails.getManager().getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur (manager) introuvable avec l'ID: " + departmentDetails.getManager().getId()));
            department.setManager(manager);
        } else {
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
        departmentRepository.delete(department);
    }
}
