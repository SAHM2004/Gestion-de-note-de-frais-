package com.ids.expense.common.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "categories_frais")
public class ExpenseCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String code;
}
