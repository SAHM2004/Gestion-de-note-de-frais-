package com.ids.expense.common.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "justificatifs_depense")
public class ExpenseAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "report_id")
    private ExpenseReport report;

    /** Ligne de dépense associée (optionnel — justificatif global si null) */
    @ManyToOne
    @JoinColumn(name = "line_id")
    private ExpenseLine line;

    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private Long fileSize;

    private LocalDateTime uploadedAt;
}
