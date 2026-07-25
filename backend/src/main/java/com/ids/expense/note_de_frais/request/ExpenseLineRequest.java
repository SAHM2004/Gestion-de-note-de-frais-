package com.ids.expense.note_de_frais.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseLineRequest {
    private Long id;
    
    @NotNull(message = "La date de dépense est obligatoire")
    private LocalDate expenseDate;
    
    private Long categoryId;
    
    @NotBlank(message = "La description de la ligne est obligatoire")
    private String description;
    
    @NotNull(message = "Le montant est obligatoire")
    private BigDecimal amount;
    
    private String itineraryFrom;
    private String itineraryTo;
}
