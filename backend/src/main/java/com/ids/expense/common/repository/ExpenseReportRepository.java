package com.ids.expense.common.repository;

import com.ids.expense.common.models.ExpenseReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseReportRepository extends JpaRepository<ExpenseReport, Long> {
    List<ExpenseReport> findByEmployeeId(Long employeeId);
    boolean existsByEmployeeId(Long employeeId);
    
    // Pour Manager et DT (filtre sur leur département)
    List<ExpenseReport> findByCurrentStepRequiredRoleAndEmployeeDepartmentId(com.ids.expense.common.models.RoleType role, Long departmentId);
    
    // Pour DG et Comptable (vue globale sur l'entreprise)
    List<ExpenseReport> findByCurrentStepRequiredRole(com.ids.expense.common.models.RoleType role);
    
    // Pour Comptable (voir les notes approuvées/payées)
    List<ExpenseReport> findByStatusIn(List<com.ids.expense.common.models.ExpenseStatus> statuses);
}
