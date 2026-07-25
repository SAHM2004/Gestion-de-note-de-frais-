package com.ids.expense.common.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "lignes_de_frais")
public class ExpenseLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "report_id")
    private ExpenseReport report;
    
    @NotNull(message = "La date de dépense est obligatoire")
    private LocalDate expenseDate;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private ExpenseCategory category;
    
    @NotBlank(message = "La description de la ligne est obligatoire")
    private String description;
    
    @NotNull(message = "Le montant est obligatoire")
    private BigDecimal amount;
    
    private String itineraryFrom;
    private String itineraryTo;
}
