package com.ids.expense.ocr;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class OcrResponse {
    private BigDecimal extractedAmount;
    private LocalDate extractedDate;
    private Long suggestedCategoryId;
    private String suggestedCategoryName;
    private String merchantName;
    private String rawTextSnippet;
    private Integer confidenceScore;
}
