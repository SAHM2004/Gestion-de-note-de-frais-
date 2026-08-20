package com.ids.expense.common.repository;

import com.ids.expense.common.models.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByManagerId(Long managerId);
    boolean existsByManagerIdAndIdNot(Long managerId, Long id);
    Optional<Department> findByManagerId(Long managerId);
}
