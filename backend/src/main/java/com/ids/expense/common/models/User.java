package com.ids.expense.common.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "utilisateurs")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;
    
    @Enumerated(EnumType.STRING)
    private RoleType role;
    

    @ManyToOne
    @JoinColumn(name = "department_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Department department;
}
