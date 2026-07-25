package com.ids.expense.common.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "modeles_workflow")
public class WorkflowTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    @OrderBy("stepOrder ASC")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<WorkflowStep> steps;
}
