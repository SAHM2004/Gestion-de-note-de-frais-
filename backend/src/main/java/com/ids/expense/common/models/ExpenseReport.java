package com.ids.expense.common.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "notes_de_frais")
public class ExpenseReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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
    
    @Enumerated(EnumType.STRING)
    private ExpenseStatus status;
    
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private User employee;
    

    @ManyToOne
    @JoinColumn(name = "current_step_id")
    private WorkflowStep currentStep;

    private String rejectionReason;
    private String rejectedAtStepName;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL)
    private List<ExpenseLine> lines;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseAttachment> attachments;
}
