package com.ids.expense.config;

import com.ids.expense.common.models.*;
import com.ids.expense.common.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@Order(30) // Executed after standard DataInitializer and DbFixer
@RequiredArgsConstructor
@Slf4j
public class MounasDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ExpenseReportRepository expenseReportRepository;
    private final ExpenseLineRepository expenseLineRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initializing specific test data for mounasahm39@gmail.com...");

        // 1. Find or create the user
        User user = userRepository.findByEmailIgnoreCase("mounasahm39@gmail.com")
                .orElseGet(() -> {
                    log.info("User mounasahm39@gmail.com not found, creating a new user...");
                    User newUser = new User();
                    newUser.setEmail("mounasahm39@gmail.com");
                    newUser.setName("Mouna SAHM");
                    newUser.setPassword(passwordEncoder.encode("password"));
                    newUser.setRole(RoleType.EMPLOYEE);
                    newUser.setActive(true);
                    newUser.setForcePasswordChange(false);

                    // Assign first available department
                    Department dept = departmentRepository.findAll().stream()
                            .filter(d -> d.getName().contains("SLF") || d.getName().contains("RH") || d.getName().contains("Logistique"))
                            .findFirst()
                            .orElseGet(() -> departmentRepository.findAll().stream().findFirst().orElse(null));
                    newUser.setDepartment(dept);
                    return userRepository.save(newUser);
                });

        // Ensure user is active and doesn't need to change password
        if (Boolean.TRUE.equals(user.isForcePasswordChange()) || !user.isActive()) {
            user.setForcePasswordChange(false);
            user.setActive(true);
            user = userRepository.save(user);
        }

        Long employeeId = user.getId();

        // 2. Clean up existing expenses for this user
        log.info("Cleaning up existing expenses for user ID: {}", employeeId);
        jdbcTemplate.update("DELETE FROM historique_validations WHERE report_id IN (SELECT id FROM notes_de_frais WHERE employee_id = ?)", employeeId);
        jdbcTemplate.update("DELETE FROM justificatifs_depense WHERE report_id IN (SELECT id FROM notes_de_frais WHERE employee_id = ?)", employeeId);
        jdbcTemplate.update("DELETE FROM lignes_de_frais WHERE report_id IN (SELECT id FROM notes_de_frais WHERE employee_id = ?)", employeeId);
        jdbcTemplate.update("DELETE FROM notes_de_frais WHERE employee_id = ?", employeeId);

        // 3. Retrieve categories
        List<ExpenseCategory> categories = expenseCategoryRepository.findAll();
        ExpenseCategory catFournitures = categories.stream().filter(c -> c.getName().equalsIgnoreCase("Fournitures")).findFirst().orElse(null);
        ExpenseCategory catCarburant = categories.stream().filter(c -> c.getName().equalsIgnoreCase("Carburant")).findFirst().orElse(null);
        ExpenseCategory catHebergement = categories.stream().filter(c -> c.getName().equalsIgnoreCase("Hébergement")).findFirst().orElse(null);
        ExpenseCategory catDeplacement = categories.stream().filter(c -> c.getName().equalsIgnoreCase("Déplacement")).findFirst().orElse(null);

        ExpenseCategory defaultCat = categories.isEmpty() ? null : categories.get(0);
        if (catFournitures == null) catFournitures = defaultCat;
        if (catCarburant == null) catCarburant = defaultCat;
        if (catHebergement == null) catHebergement = defaultCat;
        if (catDeplacement == null) catDeplacement = defaultCat;

        // 4. Create the 4 required types of expense reports

        // A. DRAFT
        log.info("Creating DRAFT expense report for mounasahm39@gmail.com...");
        ExpenseReport draftReport = new ExpenseReport();
        draftReport.setEmployee(user);
        draftReport.setTitle("Achat Fournitures de Bureau");
        draftReport.setDescription("Achat de stylos, classeurs et ramettes de papier pour l'équipe");
        draftReport.setCurrency("FCFA");
        draftReport.setDateFrom(LocalDate.now().minusDays(5));
        draftReport.setDateTo(LocalDate.now().minusDays(5));
        draftReport.setStatus(ExpenseStatus.DRAFT);
        draftReport = expenseReportRepository.save(draftReport);

        ExpenseLine draftLine = new ExpenseLine();
        draftLine.setReport(draftReport);
        draftLine.setCategory(catFournitures);
        draftLine.setDescription("Stylos et papier A4");
        draftLine.setAmount(new BigDecimal("15000"));
        draftLine.setExpenseDate(LocalDate.now().minusDays(5));
        expenseLineRepository.save(draftLine);

        // B. REJECTED
        log.info("Creating REJECTED expense report for mounasahm39@gmail.com...");
        ExpenseReport rejectedReport = new ExpenseReport();
        rejectedReport.setEmployee(user);
        rejectedReport.setTitle("Carburant Déplacement Client");
        rejectedReport.setDescription("Plein de carburant pour déplacement professionnel");
        rejectedReport.setCurrency("FCFA");
        rejectedReport.setDateFrom(LocalDate.now().minusDays(10));
        rejectedReport.setDateTo(LocalDate.now().minusDays(10));
        rejectedReport.setStatus(ExpenseStatus.REJECTED);
        rejectedReport.setRejectionReason("Reçu de paiement illisible et non conforme");
        rejectedReport.setRejectedAtStepName("Validation Manager");
        rejectedReport = expenseReportRepository.save(rejectedReport);

        ExpenseLine rejectedLine = new ExpenseLine();
        rejectedLine.setReport(rejectedReport);
        rejectedLine.setCategory(catCarburant);
        rejectedLine.setDescription("Carburant Super");
        rejectedLine.setAmount(new BigDecimal("30000"));
        rejectedLine.setExpenseDate(LocalDate.now().minusDays(10));
        expenseLineRepository.save(rejectedLine);

        // C. APPROVED
        log.info("Creating APPROVED expense report for mounasahm39@gmail.com...");
        ExpenseReport approvedReport = new ExpenseReport();
        approvedReport.setEmployee(user);
        approvedReport.setTitle("Mission d'hébergement SLF");
        approvedReport.setDescription("Frais d'hôtel pour formation des clients");
        approvedReport.setCurrency("FCFA");
        approvedReport.setDateFrom(LocalDate.now().minusDays(3));
        approvedReport.setDateTo(LocalDate.now().minusDays(2));
        approvedReport.setStatus(ExpenseStatus.APPROVED);
        approvedReport = expenseReportRepository.save(approvedReport);

        ExpenseLine approvedLine = new ExpenseLine();
        approvedLine.setReport(approvedReport);
        approvedLine.setCategory(catHebergement);
        approvedLine.setDescription("2 Nuits d'hôtel");
        approvedLine.setAmount(new BigDecimal("90000"));
        approvedLine.setExpenseDate(LocalDate.now().minusDays(3));
        expenseLineRepository.save(approvedLine);

        // D. PAID
        log.info("Creating PAID expense report for mounasahm39@gmail.com...");
        ExpenseReport paidReport = new ExpenseReport();
        paidReport.setEmployee(user);
        paidReport.setTitle("Déplacement Interurbain");
        paidReport.setDescription("Billets de transport aller-retour");
        paidReport.setCurrency("FCFA");
        paidReport.setDateFrom(LocalDate.now().minusDays(15));
        paidReport.setDateTo(LocalDate.now().minusDays(15));
        paidReport.setStatus(ExpenseStatus.PAID);
        paidReport = expenseReportRepository.save(paidReport);

        ExpenseLine paidLine = new ExpenseLine();
        paidLine.setReport(paidReport);
        paidLine.setCategory(catDeplacement);
        paidLine.setDescription("Frais de transport bus");
        paidLine.setAmount(new BigDecimal("25000"));
        paidLine.setExpenseDate(LocalDate.now().minusDays(15));
        expenseLineRepository.save(paidLine);

        log.info("Test data initialization for mounasahm39@gmail.com completed successfully.");
    }
}
