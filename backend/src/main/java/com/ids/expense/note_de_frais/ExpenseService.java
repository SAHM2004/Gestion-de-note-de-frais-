package com.ids.expense.note_de_frais;

import com.ids.expense.auth.security.UserDetailsImpl;
import com.ids.expense.common.models.User;
import com.ids.expense.note_de_frais.request.ExpenseLineRequest;
import com.ids.expense.note_de_frais.request.ExpenseReportRequest;
import com.ids.expense.note_de_frais.response.ExpenseLineResponse;
import com.ids.expense.note_de_frais.response.ExpenseReportResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExpenseService {
    Page<ExpenseReportResponse> getAccessibleReports(UserDetailsImpl currentUser, Pageable pageable);
    List<ExpenseReportResponse> getPendingApprovals(UserDetailsImpl currentUser);
    ExpenseReportResponse getReportById(Long reportId, UserDetailsImpl currentUser);
    ExpenseReportResponse createDraft(ExpenseReportRequest request, User employee);
    ExpenseReportResponse updateDraft(Long reportId, ExpenseReportRequest request, User employee);
    ExpenseLineResponse addLineToReport(Long reportId, ExpenseLineRequest request, User employee);
    ExpenseLineResponse updateLine(Long lineId, ExpenseLineRequest request, User employee);
    void deleteLine(Long lineId, User employee);
    ExpenseReportResponse markAsPaid(Long reportId);
}
