package com.ids.expense.service;

import com.ids.expense.common.models.*;
import com.ids.expense.common.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {
    
    private final ExpenseReportRepository reportRepository;
    private final WorkflowStepRepository stepRepository;
    private final ExpenseApprovalHistoryRepository historyRepository;
    private final com.ids.expense.common.repository.WorkflowTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    @Transactional
    public ExpenseReport submitReport(Long reportId, User employee) {
        ExpenseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Note de frais introuvable"));
                
        if (report.getStatus() != ExpenseStatus.DRAFT && report.getStatus() != ExpenseStatus.REJECTED) {
            throw new RuntimeException("La note doit être en brouillon ou rejetée pour être soumise");
        }
        
        WorkflowTemplate template = null;
        if (employee.getDepartment() != null) {
            template = employee.getDepartment().getDefaultWorkflowTemplate();
        }
        
        // Fallback to first available template if none assigned or no department
        if (template == null) {
            List<WorkflowTemplate> allTemplates = templateRepository.findAll();
            if (!allTemplates.isEmpty()) {
                template = allTemplates.get(0);
            }
        }
        
        if (template == null || template.getSteps().isEmpty()) {
            throw new RuntimeException("Aucun circuit de validation défini pour ce département");
        }
        
        // Get the first step (order 1)
        WorkflowStep currentStep = template.getSteps().stream()
                .filter(s -> s.getStepOrder() == 1)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Étape initiale introuvable"));
                
        report.setStatus(ExpenseStatus.IN_PROGRESS);

        // Auto-validation pour le manager, DT ou DG
        while (currentStep != null && canAutoApprove(employee, currentStep.getRequiredRole())) {
            ExpenseApprovalHistory history = new ExpenseApprovalHistory();
            history.setReport(report);
            history.setStep(currentStep);
            history.setApprover(employee);
            history.setApprovalDate(LocalDateTime.now());
            history.setDecision("APPROVED");
            history.setComment("Auto-validation (soumis par " + employee.getRole() + ")");
            historyRepository.save(history);

            int nextStepOrder = currentStep.getStepOrder() + 1;
            currentStep = stepRepository.findByTemplateIdAndStepOrder(template.getId(), nextStepOrder).orElse(null);
        }

        report.setCurrentStep(currentStep);
        if (currentStep == null) {
            report.setStatus(ExpenseStatus.APPROVED);
        }
        
        ExpenseReport saved = reportRepository.save(report);
        emailService.sendExpenseSubmitted(employee, saved);
        if (saved.getCurrentStep() != null) {
            notifyNextApprovers(saved, saved.getCurrentStep());
        }
        return saved;
    }

    private boolean canAutoApprove(User employee, RoleType requiredRole) {
        if (employee.getRole() == RoleType.GENERAL_DIRECTOR) {
            return requiredRole == RoleType.MANAGER || requiredRole == RoleType.TECHNICAL_DIRECTOR || requiredRole == RoleType.GENERAL_DIRECTOR;
        }
        if (employee.getRole() == RoleType.TECHNICAL_DIRECTOR) {
            return requiredRole == RoleType.MANAGER || requiredRole == RoleType.TECHNICAL_DIRECTOR;
        }
        if (employee.getRole() == RoleType.MANAGER) {
            return requiredRole == RoleType.MANAGER;
        }
        return false;
    }
    
    @Transactional
    public ExpenseReport approveStep(Long reportId, User approver, String comment) {
        ExpenseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Note de frais introuvable"));
                
        if (report.getStatus() != ExpenseStatus.IN_PROGRESS) {
            throw new RuntimeException("La note n'est pas en attente de validation");
        }
        
        WorkflowStep currentStep = report.getCurrentStep();
        if (currentStep == null) {
            throw new RuntimeException("Cette note n'est plus en attente de validation.");
        }
        
        // Basic check if approver has the required role
        if (approver.getRole() != currentStep.getRequiredRole()) {
            throw new RuntimeException("Vous n'avez pas le rôle requis pour valider cette étape");
        }
        
        // Check department for Manager
        if (approver.getRole() == RoleType.MANAGER) {
            Department approverDept = approver.getDepartment();
            Department employeeDept = report.getEmployee().getDepartment();
            
            if (approverDept == null) {
                throw new RuntimeException("Vous n'êtes assigné à aucun département.");
            }
            if (employeeDept == null || !approverDept.getId().equals(employeeDept.getId())) {
                throw new RuntimeException("Vous ne pouvez valider que les notes de votre propre département");
            }
        }
        
        // Record history
        ExpenseApprovalHistory history = new ExpenseApprovalHistory();
        history.setReport(report);
        history.setStep(currentStep);
        history.setApprover(approver);
        history.setApprovalDate(LocalDateTime.now());
        history.setDecision("APPROVED");
        history.setComment(comment);
        historyRepository.save(history);
        
        // Find next step
        WorkflowTemplate template = currentStep.getTemplate();
        if (template == null) {
            throw new RuntimeException("Erreur configuration: l'étape actuelle n'a pas de modèle associé.");
        }
        int nextStepOrder = currentStep.getStepOrder() + 1;
        WorkflowStep nextStep = stepRepository.findByTemplateIdAndStepOrder(
                template.getId(), nextStepOrder).orElse(null);
                
        if (nextStep != null) {
            report.setCurrentStep(nextStep);
        } else {
            report.setCurrentStep(null);
            report.setStatus(ExpenseStatus.APPROVED);
        }
        
        ExpenseReport saved = reportRepository.save(report);
        boolean isAccountantFinal = currentStep.getRequiredRole() == RoleType.ACCOUNTANT && nextStep == null;
        emailService.sendExpenseApproved(
                saved.getEmployee(),
                saved,
                currentStep.getActionName(),
                approver.getName(),
                isAccountantFinal
        );
        if (nextStep != null) {
            notifyNextApprovers(saved, nextStep);
        }
        return saved;
    }
    
    @Transactional
    public ExpenseReport rejectStep(Long reportId, User rejector, String comment) {
        ExpenseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Note de frais introuvable"));
                
        WorkflowStep currentStep = report.getCurrentStep();
        if (currentStep == null) {
            throw new RuntimeException("Cette note n'est plus en attente de validation.");
        }
        
        // Basic check if rejector has the required role
        if (rejector.getRole() != currentStep.getRequiredRole()) {
            throw new RuntimeException("Vous n'avez pas le rôle requis pour rejeter cette étape");
        }
        
        // Check department for Manager
        if (rejector.getRole() == RoleType.MANAGER) {
            Department rejectorDept = rejector.getDepartment();
            Department employeeDept = report.getEmployee().getDepartment();
            
            if (rejectorDept == null) {
                throw new RuntimeException("Vous n'êtes assigné à aucun département.");
            }
            if (employeeDept == null || !rejectorDept.getId().equals(employeeDept.getId())) {
                throw new RuntimeException("Vous ne pouvez rejeter que les notes de votre propre département");
            }
        }
        
        ExpenseApprovalHistory history = new ExpenseApprovalHistory();
        history.setReport(report);
        history.setStep(currentStep);
        history.setApprover(rejector);
        history.setApprovalDate(LocalDateTime.now());
        history.setDecision("REJECTED");
        history.setComment(comment);
        historyRepository.save(history);

        report.setCurrentStep(null);
        report.setStatus(ExpenseStatus.REJECTED);
        report.setRejectionReason(comment);
        report.setRejectedAtStepName(currentStep.getActionName());
        ExpenseReport saved = reportRepository.save(report);
        emailService.sendExpenseRejected(
                saved.getEmployee(),
                saved,
                currentStep.getActionName(),
                rejector.getName(),
                comment
        );
        return saved;
    }

    private void notifyNextApprovers(ExpenseReport report, WorkflowStep step) {
        if (step == null || report == null) return;
        
        RoleType requiredRole = step.getRequiredRole();
        if (requiredRole == RoleType.MANAGER) {
            Department dept = report.getEmployee() != null ? report.getEmployee().getDepartment() : null;
            if (dept != null && dept.getManager() != null) {
                emailService.sendValidationRequested(dept.getManager(), report.getEmployee(), report, step.getActionName());
            }
        } else {
            List<User> approvers = userRepository.findByRole(requiredRole);
            for (User approver : approvers) {
                emailService.sendValidationRequested(approver, report.getEmployee(), report, step.getActionName());
            }
        }
    }
}
