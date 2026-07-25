package com.ids.expense.controller;

import com.ids.expense.auth.security.UserDetailsImpl;
import com.ids.expense.controller.dto.AnalyticsSummaryResponse;
import com.ids.expense.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) Long departmentId) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(analyticsService.getSummary(currentUser, departmentId));
    }
}
