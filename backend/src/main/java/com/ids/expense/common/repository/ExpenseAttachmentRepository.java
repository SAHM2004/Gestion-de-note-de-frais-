package com.ids.expense.common.repository;

import com.ids.expense.common.models.ExpenseAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseAttachmentRepository extends JpaRepository<ExpenseAttachment, Long> {
    List<ExpenseAttachment> findByReportId(Long reportId);
    List<ExpenseAttachment> findByLineId(Long lineId);
    void deleteByReportId(Long reportId);
}
