package com.ids.expense.note_de_frais.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ExpenseReportRequest {
    @NotBlank(message = "Le titre est obligatoire")
    private String title;
    
    @NotBlank(message = "La description est obligatoire")
    private String description;
    
    @NotBlank(message = "La devise (currency) est obligatoire")
    private String currency;
    
    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateFrom;
    
    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateTo;
    
    private java.util.List<ExpenseLineRequest> lines;
}
