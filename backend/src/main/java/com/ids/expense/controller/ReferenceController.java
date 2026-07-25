package com.ids.expense.controller;

import com.ids.expense.common.models.ExpenseCategory;
import com.ids.expense.common.models.WorkflowTemplate;
import com.ids.expense.common.repository.ExpenseCategoryRepository;
import com.ids.expense.common.repository.WorkflowTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/references")
@RequiredArgsConstructor
public class ReferenceController {

    private final ExpenseCategoryRepository categoryRepository;
    private final WorkflowTemplateRepository templateRepository;

    @GetMapping("/categories")
    public ResponseEntity<List<ExpenseCategory>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/workflow-templates")
    public ResponseEntity<List<WorkflowTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateRepository.findAll());
    }
}
