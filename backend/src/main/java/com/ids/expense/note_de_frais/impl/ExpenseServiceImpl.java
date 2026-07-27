package com.ids.expense.note_de_frais.impl;

import com.ids.expense.auth.security.UserDetailsImpl;
import com.ids.expense.common.models.*;
import com.ids.expense.common.repository.*;
import com.ids.expense.note_de_frais.ExpenseService;
import com.ids.expense.note_de_frais.request.ExpenseLineRequest;
import com.ids.expense.note_de_frais.request.ExpenseReportRequest;
import com.ids.expense.note_de_frais.response.ExpenseAttachmentResponse;
import com.ids.expense.note_de_frais.response.ExpenseLineResponse;
import com.ids.expense.note_de_frais.response.ExpenseReportResponse;
import com.ids.expense.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {
    
    private final ExpenseReportRepository reportRepository;
    private final ExpenseLineRepository lineRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService;
    
    @Override
    public Page<ExpenseReportResponse> getAccessibleReports(UserDetailsImpl currentUser, Pageable pageable) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        RoleType role = user.getRole();
        
        List<ExpenseReport> accessibleReports = new ArrayList<>();
        accessibleReports.addAll(reportRepository.findByEmployeeId(user.getId()));
        
        if (role == RoleType.MANAGER) {
            accessibleReports.addAll(reportRepository.findByCurrentStepRequiredRoleAndEmployeeDepartmentId(role, user.getDepartment().getId()));
        } else if (role == RoleType.GENERAL_DIRECTOR || role == RoleType.TECHNICAL_DIRECTOR) {
            accessibleReports.addAll(reportRepository.findByCurrentStepRequiredRole(role));
        } else if (role == RoleType.ACCOUNTANT) {
            accessibleReports.addAll(reportRepository.findByCurrentStepRequiredRole(role));
            accessibleReports.addAll(reportRepository.findByStatusIn(Arrays.asList(ExpenseStatus.APPROVED, ExpenseStatus.PAID)));
        }
        
        List<ExpenseReportResponse> allResponses = accessibleReports.stream()
                .distinct()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
        
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, allResponses.size());
        
        List<ExpenseReportResponse> pageContent = (start >= allResponses.size()) 
                ? new ArrayList<>() 
                : allResponses.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, allResponses.size());
    }

    @Override
    public List<ExpenseReportResponse> getPendingApprovals(UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return reportRepository.findAll().stream()
                .filter(r -> r.getStatus() == ExpenseStatus.IN_PROGRESS && r.getCurrentStep() != null)
                .filter(r -> canApprove(user, r))
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ExpenseReportResponse getReportById(Long reportId, UserDetailsImpl currentUser) {
        ExpenseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Note introuvable"));
        return mapToReportResponse(report);
    }
    
    @Override
    @Transactional
    public ExpenseReportResponse createDraft(ExpenseReportRequest request, User employee) {
        ExpenseReport report = new ExpenseReport();
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setCurrency(request.getCurrency() != null ? request.getCurrency() : "FCFA");
        report.setDateFrom(request.getDateFrom());
        report.setDateTo(request.getDateTo());
        report.setStatus(ExpenseStatus.DRAFT);
        report.setEmployee(employee);
        ExpenseReport savedReport = reportRepository.save(report);

        if (request.getLines() != null && !request.getLines().isEmpty()) {
            List<ExpenseLine> savedLines = new ArrayList<>();
            for (ExpenseLineRequest lineReq : request.getLines()) {
                ExpenseLine line = buildLine(lineReq, savedReport);
                savedLines.add(lineRepository.save(line));
            }
            savedReport.setLines(savedLines);
        }

        return mapToReportResponse(savedReport);
    }

    @Override
    @Transactional
    public ExpenseReportResponse updateDraft(Long reportId, ExpenseReportRequest request, User employee) {
        ExpenseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Note introuvable"));

        assertEditable(report, employee);

        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setCurrency(request.getCurrency() != null ? request.getCurrency() : "FCFA");
        report.setDateFrom(request.getDateFrom());
        report.setDateTo(request.getDateTo());

        if (report.getStatus() == ExpenseStatus.REJECTED) {
            report.setStatus(ExpenseStatus.DRAFT);
            report.setRejectionReason(null);
            report.setRejectedAtStepName(null);
            report.setCurrentStep(null);
        }

        report = reportRepository.save(report);

        if (request.getLines() != null) {
            List<ExpenseLine> existingLines = lineRepository.findByReportId(report.getId());
            List<Long> incomingIds = request.getLines().stream()
                    .filter(l -> l.getId() != null)
                    .map(ExpenseLineRequest::getId)
                    .collect(Collectors.toList());

            // Supprimer les lignes absentes de la requête
            for (ExpenseLine existing : existingLines) {
                if (!incomingIds.contains(existing.getId())) {
                    attachmentService.deleteByLineId(existing.getId());
                    lineRepository.delete(existing);
                }
            }

            // Mettre à jour ou créer
            List<ExpenseLine> updatedLines = new ArrayList<>();
            for (ExpenseLineRequest lineReq : request.getLines()) {
                if (lineReq.getId() != null) {
                    ExpenseLine existing = existingLines.stream().filter(l -> l.getId().equals(lineReq.getId())).findFirst().orElse(null);
                    if (existing != null) {
                        existing.setExpenseDate(lineReq.getExpenseDate());
                        existing.setDescription(lineReq.getDescription());
                        existing.setAmount(lineReq.getAmount());
                        existing.setItineraryFrom(lineReq.getItineraryFrom());
                        existing.setItineraryTo(lineReq.getItineraryTo());
                        if (lineReq.getCategoryId() != null) {
                            existing.setCategory(categoryRepository.findById(lineReq.getCategoryId()).orElse(existing.getCategory()));
                        }
                        updatedLines.add(lineRepository.save(existing));
                    }
                } else {
                    ExpenseLine newLine = buildLine(lineReq, report);
                    updatedLines.add(lineRepository.save(newLine));
                }
            }
            report.setLines(updatedLines);
        }

        return mapToReportResponse(report);
    }
    
    @Override
    @Transactional
    public ExpenseLineResponse addLineToReport(Long reportId, ExpenseLineRequest request, User employee) {
        ExpenseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Note introuvable"));
        assertEditable(report, employee);
        
        ExpenseLine line = buildLine(request, report);
        ExpenseLine savedLine = lineRepository.save(line);
        return mapToLineResponse(savedLine);
    }

    @Override
    @Transactional
    public ExpenseLineResponse updateLine(Long lineId, ExpenseLineRequest request, User employee) {
        ExpenseLine line = lineRepository.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Ligne introuvable"));
        assertEditable(line.getReport(), employee);

        line.setExpenseDate(request.getExpenseDate());
        line.setDescription(request.getDescription());
        line.setAmount(request.getAmount());
        line.setItineraryFrom(request.getItineraryFrom());
        line.setItineraryTo(request.getItineraryTo());

        if (request.getCategoryId() != null) {
            ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
            line.setCategory(category);
        }

        return mapToLineResponse(lineRepository.save(line));
    }
    
    @Override
    @Transactional
    public void deleteLine(Long lineId, User employee) {
        ExpenseLine line = lineRepository.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Ligne introuvable"));
        assertEditable(line.getReport(), employee);
        attachmentService.deleteByLineId(lineId);
        lineRepository.delete(line);
    }
    
    @Override
    @Transactional
    public ExpenseReportResponse markAsPaid(Long reportId) {
        ExpenseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Note introuvable"));
                
        if (report.getStatus() != ExpenseStatus.APPROVED) {
            throw new RuntimeException("Seule une note approuvée peut être marquée comme payée");
        }
        
        report.setStatus(ExpenseStatus.PAID);
        return mapToReportResponse(reportRepository.save(report));
    }

    private ExpenseLine buildLine(ExpenseLineRequest request, ExpenseReport report) {
        ExpenseLine line = new ExpenseLine();
        line.setExpenseDate(request.getExpenseDate());
        line.setDescription(request.getDescription());
        line.setAmount(request.getAmount());
        line.setItineraryFrom(request.getItineraryFrom());
        line.setItineraryTo(request.getItineraryTo());

        if (request.getCategoryId() != null) {
            ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));
            line.setCategory(category);
        }

        line.setReport(report);
        return line;
    }

    private void assertEditable(ExpenseReport report, User employee) {
        if (!report.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres notes");
        }
        assertEditableStatus(report);
    }

    private void assertEditableStatus(ExpenseReport report) {
        if (report.getStatus() != ExpenseStatus.DRAFT && report.getStatus() != ExpenseStatus.REJECTED) {
            throw new RuntimeException("Impossible de modifier une note déjà soumise");
        }
    }

    private boolean canApprove(User user, ExpenseReport report) {
        WorkflowStep step = report.getCurrentStep();
        if (step == null || user.getRole() != step.getRequiredRole()) return false;

        if (user.getRole() == RoleType.MANAGER) {
            return user.getDepartment() != null
                    && report.getEmployee().getDepartment() != null
                    && user.getDepartment().getId().equals(report.getEmployee().getDepartment().getId());
        }
        return true;
    }
    
    private ExpenseReportResponse mapToReportResponse(ExpenseReport report) {
        ExpenseReportResponse response = new ExpenseReportResponse();
        response.setId(report.getId());
        response.setTitle(report.getTitle());
        response.setDescription(report.getDescription());
        response.setCurrency(report.getCurrency());
        response.setDateFrom(report.getDateFrom());
        response.setDateTo(report.getDateTo());
        response.setStatus(report.getStatus());
        response.setRejectionReason(report.getRejectionReason());
        response.setRejectedAtStepName(report.getRejectedAtStepName());
        
        if (report.getEmployee() != null) {
            response.setEmployeeId(report.getEmployee().getId());
            response.setEmployeeName(report.getEmployee().getName());
            if (report.getEmployee().getDepartment() != null) {
                response.setEmployeeDepartmentName(report.getEmployee().getDepartment().getName());
            }
        }

        if (report.getCurrentStep() != null) {
            response.setCurrentStepRole(report.getCurrentStep().getRequiredRole().name());
            response.setCurrentStepName(report.getCurrentStep().getActionName());
        }
        
        if (report.getLines() != null) {
            List<ExpenseLineResponse> lineResponses = report.getLines().stream().map(this::mapToLineResponse).collect(Collectors.toList());
            response.setLines(lineResponses);
            boolean anyOver = lineResponses.stream().anyMatch(l -> Boolean.TRUE.equals(l.getIsOverCeiling()));
            response.setIsAnyLineOverCeiling(anyOver);
        }

        if (report.getId() != null) {
            response.setAttachments(attachmentService.listByReportIdInternal(report.getId()));
        }
        
        return response;
    }
    
    private ExpenseLineResponse mapToLineResponse(ExpenseLine line) {
        ExpenseLineResponse response = new ExpenseLineResponse();
        response.setId(line.getId());
        response.setExpenseDate(line.getExpenseDate());
        response.setDescription(line.getDescription());
        response.setAmount(line.getAmount());
        response.setItineraryFrom(line.getItineraryFrom());
        response.setItineraryTo(line.getItineraryTo());
        
        if (line.getCategory() != null) {
            response.setCategoryId(line.getCategory().getId());
            response.setCategoryName(line.getCategory().getName());
            response.setCategoryMaxAmount(line.getCategory().getMaxAmount());
            if (line.getCategory().getMaxAmount() != null && line.getAmount() != null) {
                boolean over = line.getAmount().compareTo(line.getCategory().getMaxAmount()) > 0;
                response.setIsOverCeiling(over);
                if (over) {
                    response.setCeilingWarningMessage("Plafond recommandé de " + line.getCategory().getMaxAmount() + " € dépassé (" + line.getAmount() + " €)");
                }
            } else {
                response.setIsOverCeiling(false);
            }
        }

        if (line.getId() != null) {
            response.setAttachments(attachmentService.listByLineIdInternal(line.getId()));
        }
        
        return response;
    }
}
