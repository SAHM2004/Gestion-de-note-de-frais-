package com.ids.expense.common.repository;

import com.ids.expense.common.models.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    Optional<WorkflowStep> findByTemplateIdAndStepOrder(Long templateId, Integer stepOrder);
}
