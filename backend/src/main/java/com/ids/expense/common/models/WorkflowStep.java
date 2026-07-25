package com.ids.expense.common.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "etapes_workflow")
public class WorkflowStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "template_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private WorkflowTemplate template;
    
    private Integer stepOrder;
    
    @Enumerated(EnumType.STRING)
    private RoleType requiredRole;
    
    private String actionName;
}
