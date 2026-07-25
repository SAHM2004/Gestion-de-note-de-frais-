package com.ids.expense.common.repository;

import com.ids.expense.common.models.ExpenseLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseLineRepository extends JpaRepository<ExpenseLine, Long> {
    java.util.List<ExpenseLine> findByReportId(Long reportId);
}
