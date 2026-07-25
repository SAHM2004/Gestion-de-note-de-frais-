package com.ids.expense.service;

import com.ids.expense.common.models.WorkflowTemplate;
import com.ids.expense.common.models.WorkflowStep;
import com.ids.expense.common.repository.WorkflowTemplateRepository;
import com.ids.expense.common.repository.WorkflowStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowTemplateService {

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowStepRepository stepRepository;

    public Page<WorkflowTemplate> getAllTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable);
    }

    public WorkflowTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Modèle de workflow introuvable avec l'ID: " + id));
    }

    @Transactional
    public WorkflowTemplate createTemplate(WorkflowTemplate template) {
        WorkflowTemplate savedTemplate = templateRepository.save(template);
        if (template.getSteps() != null) {
            for (WorkflowStep step : template.getSteps()) {
                step.setTemplate(savedTemplate);
                stepRepository.save(step);
            }
        }
        return savedTemplate;
    }

    @Transactional
    public WorkflowTemplate updateTemplate(Long id, WorkflowTemplate templateDetails) {
        WorkflowTemplate template = getTemplateById(id);
        template.setName(templateDetails.getName());
        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        WorkflowTemplate template = getTemplateById(id);
        templateRepository.delete(template);
    }
    
    @Transactional
    public WorkflowStep addStepToTemplate(Long templateId, WorkflowStep step) {
        WorkflowTemplate template = getTemplateById(templateId);
        step.setTemplate(template);
        return stepRepository.save(step);
    }
    
    @Transactional
    public void removeStep(Long stepId) {
        stepRepository.deleteById(stepId);
    }
}
