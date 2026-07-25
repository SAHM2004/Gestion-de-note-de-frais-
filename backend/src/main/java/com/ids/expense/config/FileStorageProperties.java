package com.ids.expense.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.storage")
public class FileStorageProperties {
    private String uploadDir = "uploads/justificatifs";
    private long maxFileSizeBytes = 10 * 1024 * 1024; // 10 Mo
    private List<String> allowedContentTypes = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );
}
