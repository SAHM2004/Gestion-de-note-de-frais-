package com.ids.expense.common.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "historique_validations")
public class ExpenseApprovalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "report_id")
    private ExpenseReport report;
    
    @ManyToOne
    @JoinColumn(name = "step_id")
    private WorkflowStep step;
    
    @ManyToOne
    @JoinColumn(name = "approver_id")
    private User approver;
    
    private LocalDateTime approvalDate;
    
    private String decision; // e.g. APPROVED, REJECTED
    private String comment;
}
