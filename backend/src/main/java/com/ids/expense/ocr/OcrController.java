package com.ids.expense.ocr;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/scan")
    public ResponseEntity<OcrResponse> scanReceipt(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ocrService.scanReceipt(file));
    }
}
