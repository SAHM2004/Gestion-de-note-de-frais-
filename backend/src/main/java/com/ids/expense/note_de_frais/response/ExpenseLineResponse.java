package com.ids.expense.note_de_frais.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ExpenseLineResponse {
    private Long id;
    private LocalDate expenseDate;
    private Long categoryId;
    private String categoryName;
    private String description;
    private BigDecimal amount;
    private String itineraryFrom;
    private String itineraryTo;
    private List<ExpenseAttachmentResponse> attachments;
}
