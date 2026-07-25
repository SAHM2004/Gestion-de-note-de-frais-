package com.ids.expense.note_de_frais.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExpenseAttachmentResponse {
    private Long id;
    private Long reportId;
    private Long lineId;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
}
