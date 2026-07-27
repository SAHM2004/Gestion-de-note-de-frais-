package com.ids.expense.report;

import com.ids.expense.auth.security.UserDetailsImpl;
import com.ids.expense.note_de_frais.ExpenseService;
import com.ids.expense.note_de_frais.response.ExpenseReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ExpenseService expenseService;

    @GetMapping("/expenses/{id}/pdf")
    public ResponseEntity<InputStreamResource> exportExpensePdf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if (userDetails == null) return ResponseEntity.status(401).build();

        ByteArrayInputStream pdfStream = reportService.generateExpensePdf(id, userDetails);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=Note_de_frais_" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }

    @GetMapping("/expenses/export/csv")
    public ResponseEntity<InputStreamResource> exportExpensesCsv(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if (userDetails == null) return ResponseEntity.status(401).build();

        Page<ExpenseReportResponse> page = expenseService.getAccessibleReports(userDetails, PageRequest.of(0, 1000));
        ByteArrayInputStream csvStream = reportService.generateExpensesCsv(page.getContent());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Notes_de_frais_export.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(csvStream));
    }
}
