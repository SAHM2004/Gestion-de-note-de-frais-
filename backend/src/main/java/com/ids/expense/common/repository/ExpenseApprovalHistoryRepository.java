package com.ids.expense.common.repository;

import com.ids.expense.common.models.ExpenseApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseApprovalHistoryRepository extends JpaRepository<ExpenseApprovalHistory, Long> {
}
