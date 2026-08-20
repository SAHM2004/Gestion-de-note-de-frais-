package com.ids.expense.service;

import com.ids.expense.common.models.ExpenseCategory;
import com.ids.expense.common.repository.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    public Page<ExpenseCategory> getAllCategories(Pageable pageable) {
        return expenseCategoryRepository.findAll(pageable);
    }

    public ExpenseCategory getCategoryById(Long id) {
        return expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie de notes de frais non trouvée pour l'ID :: " + id));
    }

    @Transactional
    public ExpenseCategory createCategory(ExpenseCategory category) {
        validateMaxAmount(category);
        return expenseCategoryRepository.save(category);
    }

    @Transactional
    public ExpenseCategory updateCategory(Long id, ExpenseCategory categoryDetails) {
        validateMaxAmount(categoryDetails);
        ExpenseCategory category = getCategoryById(id);
        category.setName(categoryDetails.getName());
        category.setCode(categoryDetails.getCode());
        category.setDescription(categoryDetails.getDescription());
        category.setMaxAmount(categoryDetails.getMaxAmount());
        return expenseCategoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        ExpenseCategory category = getCategoryById(id);
        expenseCategoryRepository.delete(category);
    }

    private void validateMaxAmount(ExpenseCategory category) {
        if (category.getMaxAmount() == null) return; // plafond facultatif
        if (category.getMaxAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Le plafond ne peut pas être négatif.");
        }
        if (category.getMaxAmount().compareTo(new BigDecimal("50")) < 0) {
            throw new RuntimeException("Le plafond doit être d'au moins 50 FCFA.");
        }
    }
}
