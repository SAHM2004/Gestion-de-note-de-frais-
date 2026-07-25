package com.ids.expense.controller.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AnalyticsSummaryResponse {
    private String scope;
    private BigDecimal totalReimbursed;
    private BigDecimal totalPending;
    private BigDecimal totalSubmitted;
    private BigDecimal totalApproved;
    private BigDecimal totalPaid;
    private double rejectionRatePercent;
    private List<DepartmentExpenseStat> byDepartment;
    private List<CategoryExpenseStat> topCategories;
    private List<EmployeeExpenseStat> topEmployees;

    @Data
    public static class DepartmentExpenseStat {
        private String departmentName;
        private String shortName;
        private BigDecimal total;
        private Map<String, BigDecimal> byCategory;
    }

    @Data
    public static class CategoryExpenseStat {
        private String categoryName;
        private BigDecimal amount;
        private int percent;
    }

    @Data
    public static class EmployeeExpenseStat {
        private String employeeName;
        private String departmentName;
        private BigDecimal total;
    }
}
