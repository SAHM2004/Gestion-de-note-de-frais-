package com.ids.expense.common.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "departements")
public class Department {
    public Department() {}
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "manager_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"department", "password"})
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private User manager;
    
    @ManyToOne
    @JoinColumn(name = "workflow_template_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private WorkflowTemplate defaultWorkflowTemplate;
}
