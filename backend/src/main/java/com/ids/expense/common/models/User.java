package com.ids.expense.common.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "utilisateurs")
public class User {
    public User() {}
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;
    
    @Enumerated(EnumType.STRING)
    private RoleType role;
    
    @Column(columnDefinition = "boolean default true")
    private Boolean active = true;
    
    @Column(columnDefinition = "boolean default true")
    private Boolean forcePasswordChange = true;

    public boolean isActive() {
        return active == null || active;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange == null || forcePasswordChange;
    }
    

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "department_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"manager"})
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Department department;
}
