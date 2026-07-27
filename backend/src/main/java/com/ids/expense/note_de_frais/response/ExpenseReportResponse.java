package com.ids.expense.note_de_frais.response;

import com.ids.expense.common.models.ExpenseStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ExpenseReportResponse {
    private Long id;
    private String title;
    private String description;
    private String currency;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private ExpenseStatus status;

    private Long employeeId;
    private String employeeName;
    private String employeeDepartmentName;

    private String currentStepRole;
    private String currentStepName;

    private String rejectionReason;
    private String rejectedAtStepName;

    private Boolean isAnyLineOverCeiling;

    private List<ExpenseLineResponse> lines;
    private List<ExpenseAttachmentResponse> attachments;
}
