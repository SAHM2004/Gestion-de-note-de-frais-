package com.ids.expense.note_de_frais;

import com.ids.expense.auth.security.UserDetailsImpl;
import com.ids.expense.common.models.ExpenseReport;
import com.ids.expense.common.models.User;
import com.ids.expense.common.repository.UserRepository;
import com.ids.expense.note_de_frais.request.ExpenseLineRequest;
import com.ids.expense.note_de_frais.request.ExpenseReportRequest;
import com.ids.expense.note_de_frais.response.ExpenseAttachmentResponse;
import com.ids.expense.note_de_frais.response.ExpenseLineResponse;
import com.ids.expense.note_de_frais.response.ExpenseReportResponse;
import com.ids.expense.service.AttachmentService;
import com.ids.expense.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final WorkflowService workflowService;
    private final AttachmentService attachmentService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<ExpenseReportResponse>> getMyReports(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(expenseService.getAccessibleReports(currentUser, PageRequest.of(page, size)));
    }

    @GetMapping("/pending-approvals")
    public ResponseEntity<List<ExpenseReportResponse>> getPendingApprovals(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(expenseService.getPendingApprovals(currentUser));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ExpenseReportResponse> getReportById(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long reportId) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(expenseService.getReportById(reportId, currentUser));
    }

    @PostMapping("/draft")
    public ResponseEntity<ExpenseReportResponse> createDraft(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestBody ExpenseReportRequest request) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User employee = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(expenseService.createDraft(request, employee));
    }

    @PutMapping("/{reportId}")
    public ResponseEntity<ExpenseReportResponse> updateDraft(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long reportId,
            @RequestBody ExpenseReportRequest request) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User employee = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(expenseService.updateDraft(reportId, request, employee));
    }

    @PostMapping("/{reportId}/lines")
    public ResponseEntity<ExpenseLineResponse> addLine(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long reportId,
            @RequestBody ExpenseLineRequest line) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User employee = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(expenseService.addLineToReport(reportId, line, employee));
    }

    @PutMapping("/lines/{lineId}")
    public ResponseEntity<ExpenseLineResponse> updateLine(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long lineId,
            @RequestBody ExpenseLineRequest line) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User employee = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(expenseService.updateLine(lineId, line, employee));
    }

    @DeleteMapping("/lines/{lineId}")
    public ResponseEntity<Void> deleteLine(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long lineId) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User employee = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        expenseService.deleteLine(lineId, employee);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reportId}/submit")
    public ResponseEntity<ExpenseReportResponse> submitReport(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long reportId) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User employee = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        workflowService.submitReport(reportId, employee);
        return ResponseEntity.ok(expenseService.getReportById(reportId, currentUser));
    }

    @PostMapping("/{reportId}/approve")
    public ResponseEntity<ExpenseReportResponse> approveStep(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long reportId,
            @RequestParam(required = false) String comment) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User approver = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        workflowService.approveStep(reportId, approver, comment);
        return ResponseEntity.ok(expenseService.getReportById(reportId, currentUser));
    }

    @PostMapping("/{reportId}/reject")
    public ResponseEntity<ExpenseReportResponse> rejectStep(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long reportId,
            @RequestParam(required = false) String comment) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User rejector = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        workflowService.rejectStep(reportId, rejector, comment);
        return ResponseEntity.ok(expenseService.getReportById(reportId, currentUser));
    }

    @PostMapping("/{reportId}/mark-paid")
    public ResponseEntity<ExpenseReportResponse> markAsPaid(@PathVariable Long reportId) {
        return ResponseEntity.ok(expenseService.markAsPaid(reportId));
    }

    // --- Justificatifs (multipart/form-data : champs "file" + "lineId" optionnel) ---

    @GetMapping("/{reportId}/attachments")
    public ResponseEntity<List<ExpenseAttachmentResponse>> listAttachments(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long reportId) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(attachmentService.listByReport(reportId, currentUser.getId()));
    }

    @PostMapping(value = "/{reportId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExpenseAttachmentResponse> uploadAttachment(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long reportId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lineId", required = false) Long lineId) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(attachmentService.upload(reportId, lineId, file, currentUser.getId()));
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<ExpenseAttachmentResponse> getAttachment(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long attachmentId) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(attachmentService.getById(attachmentId, currentUser.getId()));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long attachmentId) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        AttachmentService.DownloadPayload payload =
                attachmentService.loadForDownload(attachmentId, currentUser.getId());
        String encodedName = AttachmentService.encodeFilename(payload.originalFileName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + payload.originalFileName() + "\"; filename*=UTF-8''" + encodedName)
                .body(payload.resource());
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long attachmentId) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        attachmentService.delete(attachmentId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
