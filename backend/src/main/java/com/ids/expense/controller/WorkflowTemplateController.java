package com.ids.expense.controller;

import com.ids.expense.common.models.WorkflowTemplate;
import com.ids.expense.common.models.WorkflowStep;
import com.ids.expense.service.WorkflowTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@RestController
@RequestMapping("/api/workflow-templates")
@RequiredArgsConstructor
public class WorkflowTemplateController {

    private final WorkflowTemplateService templateService;

    @GetMapping
    public ResponseEntity<Page<WorkflowTemplate>> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(templateService.getAllTemplates(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowTemplate> getTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @PostMapping
    public ResponseEntity<WorkflowTemplate> createTemplate(@RequestBody WorkflowTemplate template) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowTemplate> updateTemplate(@PathVariable Long id, @RequestBody WorkflowTemplate templateDetails) {
        return ResponseEntity.ok(templateService.updateTemplate(id, templateDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/steps")
    public ResponseEntity<WorkflowStep> addStepToTemplate(@PathVariable Long id, @RequestBody WorkflowStep step) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.addStepToTemplate(id, step));
    }
    
    @DeleteMapping("/steps/{stepId}")
    public ResponseEntity<Void> removeStep(@PathVariable Long stepId) {
        templateService.removeStep(stepId);
        return ResponseEntity.noContent().build();
    }
}
