package com.ids.expense.service;

import com.ids.expense.auth.security.UserDetailsImpl;
import com.ids.expense.common.models.*;
import com.ids.expense.common.repository.ExpenseReportRepository;
import com.ids.expense.common.repository.UserRepository;
import com.ids.expense.controller.dto.AnalyticsSummaryResponse;
import com.ids.expense.controller.dto.AnalyticsSummaryResponse.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseReportRepository reportRepository;
    private final UserRepository userRepository;

    public AnalyticsSummaryResponse getSummary(UserDetailsImpl currentUser, Long departmentIdFilter, Integer year, Integer month) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<ExpenseReport> allReports = reportRepository.findAll();
        List<ExpenseReport> scoped = filterByRole(allReports, user, departmentIdFilter);

        // Appliquer les filtres d'année et de mois
        if (year != null || month != null) {
            final Integer yFilter = year;
            final Integer mFilter = month;
            scoped = scoped.stream()
                    .filter(r -> {
                        if (r.getDateFrom() == null) return false;
                        boolean matchYear = (yFilter == null || r.getDateFrom().getYear() == yFilter);
                        boolean matchMonth = (mFilter == null || r.getDateFrom().getMonthValue() == mFilter);
                        return matchYear && matchMonth;
                    })
                    .collect(Collectors.toList());
        }

        AnalyticsSummaryResponse response = new AnalyticsSummaryResponse();
        response.setScope(buildScopeLabel(user, departmentIdFilter));

        BigDecimal reimbursed = sumByStatus(scoped, ExpenseStatus.APPROVED, ExpenseStatus.PAID);
        BigDecimal pending = calculatePendingForUser(scoped, user);
        BigDecimal submitted = sumByStatus(scoped, ExpenseStatus.IN_PROGRESS, ExpenseStatus.APPROVED, ExpenseStatus.PAID, ExpenseStatus.REJECTED);
        BigDecimal approved = sumByStatus(scoped, ExpenseStatus.APPROVED);
        BigDecimal paid = sumByStatus(scoped, ExpenseStatus.PAID);

        response.setTotalReimbursed(reimbursed);
        response.setTotalPending(pending);
        response.setTotalSubmitted(submitted);
        response.setTotalApproved(approved);
        response.setTotalPaid(paid);
        response.setRejectionRatePercent(calculateRejectionRate(scoped));
        response.setByDepartment(buildDepartmentStats(scoped));
        response.setTopCategories(buildTopCategories(scoped));
        response.setTopEmployees(buildTopEmployees(scoped));

        return response;
    }

    /**
     * Statistiques personnelles : uniquement les notes soumises PAR l'utilisateur connecté,
     * quel que soit son rôle (Manager, DT, DG, Comptable…).
     */
    public AnalyticsSummaryResponse getPersonalSummary(UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Filtrer uniquement les notes dont l'employé est l'utilisateur lui-même
        List<ExpenseReport> myReports = reportRepository.findAll().stream()
                .filter(r -> r.getEmployee().getId().equals(user.getId()))
                .collect(Collectors.toList());

        AnalyticsSummaryResponse response = new AnalyticsSummaryResponse();
        response.setScope("Mes notes de frais");

        response.setTotalSubmitted(sumByStatus(myReports,
                ExpenseStatus.IN_PROGRESS, ExpenseStatus.APPROVED, ExpenseStatus.PAID, ExpenseStatus.REJECTED));
        response.setTotalPending(sumByStatus(myReports, ExpenseStatus.IN_PROGRESS));
        response.setTotalApproved(sumByStatus(myReports, ExpenseStatus.APPROVED));
        response.setTotalPaid(sumByStatus(myReports, ExpenseStatus.PAID));
        response.setTotalRejected(sumByStatus(myReports, ExpenseStatus.REJECTED));
        response.setRejectionRatePercent(calculateRejectionRate(myReports));
        response.setTopCategories(buildTopCategories(myReports));

        return response;
    }

    private boolean isTechnicalDepartment(Department dept) {
        if (dept == null || dept.getName() == null) return false;
        String name = dept.getName().toUpperCase();
        return name.contains("SLF") || name.contains("SCR") || name.contains("ALVANET");
    }

    private List<ExpenseReport> filterByRole(List<ExpenseReport> all, User user, Long departmentIdFilter) {
        RoleType role = user.getRole();
        
        if (role == RoleType.GENERAL_DIRECTOR) {
            if (departmentIdFilter != null) {
                return all.stream()
                        .filter(r -> r.getEmployee().getDepartment() != null
                                && departmentIdFilter.equals(r.getEmployee().getDepartment().getId()))
                        .collect(Collectors.toList());
            }
            return all;
        }
        
        if (role == RoleType.TECHNICAL_DIRECTOR) {
            return all.stream()
                    .filter(r -> r.getEmployee().getDepartment() != null && isTechnicalDepartment(r.getEmployee().getDepartment()))
                    .filter(r -> {
                        if (departmentIdFilter != null) {
                            return departmentIdFilter.equals(r.getEmployee().getDepartment().getId());
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        
        if (role == RoleType.MANAGER && user.getDepartment() != null) {
            Long deptId = user.getDepartment().getId();
            return all.stream()
                    .filter(r -> r.getEmployee().getDepartment() != null
                            && deptId.equals(r.getEmployee().getDepartment().getId()))
                    .collect(Collectors.toList());
        }
        return all.stream()
                .filter(r -> r.getEmployee().getId().equals(user.getId()))
                .collect(Collectors.toList());
    }

    private String buildScopeLabel(User user, Long departmentIdFilter) {
        if (user.getRole() == RoleType.MANAGER) {
            return user.getDepartment() != null ? user.getDepartment().getName() : "Mon département";
        }
        return departmentIdFilter != null ? "Département filtré" : "Tous les services";
    }

    private BigDecimal sumByStatus(List<ExpenseReport> reports, ExpenseStatus... statuses) {
        Set<ExpenseStatus> allowed = Set.of(statuses);
        return reports.stream()
                .filter(r -> allowed.contains(r.getStatus()))
                .map(this::reportTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double calculateRejectionRate(List<ExpenseReport> reports) {
        long submitted = reports.stream().filter(r -> r.getStatus() != ExpenseStatus.DRAFT).count();
        if (submitted == 0) return 0;
        long rejected = reports.stream().filter(r -> r.getStatus() == ExpenseStatus.REJECTED).count();
        return Math.round((rejected * 1000.0 / submitted)) / 10.0;
    }

    private List<DepartmentExpenseStat> buildDepartmentStats(List<ExpenseReport> reports) {
        Map<String, DepartmentExpenseStat> map = new LinkedHashMap<>();
        for (ExpenseReport r : reports) {
            if (r.getStatus() == ExpenseStatus.DRAFT || r.getStatus() == ExpenseStatus.REJECTED) continue;
            User emp = userRepository.findById(r.getEmployee().getId()).orElse(r.getEmployee());
            String deptName = emp.getDepartment() != null ? emp.getDepartment().getName() : "Non assigné";
            DepartmentExpenseStat stat = map.computeIfAbsent(deptName, k -> {
                DepartmentExpenseStat s = new DepartmentExpenseStat();
                s.setDepartmentName(deptName);
                s.setShortName(shortDeptName(deptName));
                s.setTotal(BigDecimal.ZERO);
                s.setByCategory(new HashMap<>());
                return s;
            });
            BigDecimal amount = reportTotal(r);
            stat.setTotal(stat.getTotal().add(amount));
            if (r.getLines() != null) {
                for (ExpenseLine line : r.getLines()) {
                    String cat = line.getCategory() != null ? line.getCategory().getName() : "Autre";
                    BigDecimal lineAmount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
                    stat.getByCategory().merge(cat, lineAmount, BigDecimal::add);
                }
            }
        }
        return map.values().stream()
                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                .collect(Collectors.toList());
    }

    private List<CategoryExpenseStat> buildTopCategories(List<ExpenseReport> reports) {
        Map<String, BigDecimal> totals = new HashMap<>();
        for (ExpenseReport r : reports) {
            if (r.getStatus() == ExpenseStatus.DRAFT || r.getStatus() == ExpenseStatus.REJECTED) continue;
            if (r.getLines() == null) continue;
            for (ExpenseLine line : r.getLines()) {
                String cat = line.getCategory() != null ? line.getCategory().getName() : "Autre";
                BigDecimal amt = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
                totals.merge(cat, amt, BigDecimal::add);
            }
        }
        BigDecimal grand = totals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (grand.compareTo(BigDecimal.ZERO) == 0) return List.of();

        return totals.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> {
                    CategoryExpenseStat stat = new CategoryExpenseStat();
                    stat.setCategoryName(e.getKey());
                    stat.setAmount(e.getValue());
                    stat.setPercent(e.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(grand, 0, RoundingMode.HALF_UP).intValue());
                    return stat;
                })
                .collect(Collectors.toList());
    }

    private List<EmployeeExpenseStat> buildTopEmployees(List<ExpenseReport> reports) {
        Map<String, EmployeeExpenseStat> map = new HashMap<>();
        for (ExpenseReport r : reports) {
            if (r.getStatus() == ExpenseStatus.DRAFT || r.getStatus() == ExpenseStatus.REJECTED) continue;
            User emp = userRepository.findById(r.getEmployee().getId()).orElse(r.getEmployee());
            String key = emp.getEmail();
            EmployeeExpenseStat stat = map.computeIfAbsent(key, k -> {
                EmployeeExpenseStat s = new EmployeeExpenseStat();
                s.setEmployeeName(emp.getName());
                s.setDepartmentName(emp.getDepartment() != null
                        ? shortDeptName(emp.getDepartment().getName()) : "-");
                s.setTotal(BigDecimal.ZERO);
                return s;
            });
            stat.setTotal(stat.getTotal().add(reportTotal(r)));
        }
        return map.values().stream()
                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private BigDecimal reportTotal(ExpenseReport report) {
        if (report.getLines() == null) return BigDecimal.ZERO;
        return report.getLines().stream()
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String shortDeptName(String name) {
        if (name == null) return "Non assigné";
        if (name.contains("ALVANET")) return "ALVANET";
        if (name.contains("SLF")) return "SLF";
        if (name.contains("SCR")) return "SCR";
        if (name.contains("Comptabilité")) return "Comptabilité";
        if (name.contains("RH")) return "RH";
        if (name.contains("Logistique")) return "Logistique";
        if (name.contains("Commerciaux")) return "Commerciaux";
        return name.length() > 25 ? name.substring(0, 23) + "…" : name;
    }

    private BigDecimal calculatePendingForUser(List<ExpenseReport> scoped, User user) {
        RoleType role = user.getRole();
        if (role == RoleType.EMPLOYEE || role == RoleType.ADMIN) {
            return scoped.stream()
                    .filter(r -> r.getStatus() == ExpenseStatus.IN_PROGRESS)
                    .map(this::reportTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            return scoped.stream()
                    .filter(r -> r.getStatus() == ExpenseStatus.IN_PROGRESS && r.getCurrentStep() != null)
                    .filter(r -> {
                        WorkflowStep step = r.getCurrentStep();
                        if (step.getRequiredRole() != role) return false;
                        if (role == RoleType.MANAGER) {
                            return user.getDepartment() != null
                                    && r.getEmployee().getDepartment() != null
                                    && user.getDepartment().getId().equals(r.getEmployee().getDepartment().getId());
                        }
                        return true;
                    })
                    .map(this::reportTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
