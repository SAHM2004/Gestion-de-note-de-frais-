package com.ids.expense.config;

import com.ids.expense.common.models.Department;
import com.ids.expense.common.models.RoleType;
import com.ids.expense.common.models.User;
import com.ids.expense.common.models.WorkflowStep;
import com.ids.expense.common.models.WorkflowTemplate;
import com.ids.expense.common.models.ExpenseCategory;
import com.ids.expense.common.repository.DepartmentRepository;
import com.ids.expense.common.repository.UserRepository;
import com.ids.expense.common.repository.WorkflowStepRepository;
import com.ids.expense.common.repository.WorkflowTemplateRepository;
import com.ids.expense.common.repository.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.admin.email}")
    private String adminEmail;

    @org.springframework.beans.factory.annotation.Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Check if data already exists to avoid duplicate inserts on restart
            if (departmentRepository.count() > 0) {
                System.out.println("Données déjà initialisées.");
                return;
            }

            System.out.println("Initialisation des données par défaut...");

            // 0. Création des Catégories de Frais
            java.util.Map<String, java.math.BigDecimal> defaultMaxAmounts = java.util.Map.of(
                "Déplacement", new java.math.BigDecimal("200.00"),
                "Restauration", new java.math.BigDecimal("50.00"),
                "Hébergement", new java.math.BigDecimal("150.00"),
                "Carburant", new java.math.BigDecimal("80.00"),
                "Fournitures", new java.math.BigDecimal("100.00"),
                "Autre", new java.math.BigDecimal("100.00")
            );
            for (java.util.Map.Entry<String, java.math.BigDecimal> entry : defaultMaxAmounts.entrySet()) {
                ExpenseCategory cat = new ExpenseCategory();
                cat.setName(entry.getKey());
                cat.setCode(entry.getKey().substring(0, Math.min(3, entry.getKey().length())).toUpperCase());
                cat.setMaxAmount(entry.getValue());
                cat.setDescription("Plafond recommandé : " + entry.getValue() + " €");
                expenseCategoryRepository.save(cat);
            }

            // 2. Création des modèles de workflow
            
            // Workflow complet pour les directions techniques
            WorkflowTemplate techTemplate = new WorkflowTemplate();
            techTemplate.setName("Workflow Technique (Manager -> DT -> DG -> Comptable)");
            techTemplate = workflowTemplateRepository.save(techTemplate);

            WorkflowStep step1T = new WorkflowStep();
            step1T.setTemplate(techTemplate);
            step1T.setStepOrder(1);
            step1T.setRequiredRole(RoleType.MANAGER);
            step1T.setActionName("Validation Manager");
            workflowStepRepository.save(step1T);

            WorkflowStep step2T = new WorkflowStep();
            step2T.setTemplate(techTemplate);
            step2T.setStepOrder(2);
            step2T.setRequiredRole(RoleType.TECHNICAL_DIRECTOR);
            step2T.setActionName("Validation Directeur Technique");
            workflowStepRepository.save(step2T);

            WorkflowStep step3T = new WorkflowStep();
            step3T.setTemplate(techTemplate);
            step3T.setStepOrder(3);
            step3T.setRequiredRole(RoleType.GENERAL_DIRECTOR);
            step3T.setActionName("Validation Directeur Général");
            workflowStepRepository.save(step3T);

            WorkflowStep step4T = new WorkflowStep();
            step4T.setTemplate(techTemplate);
            step4T.setStepOrder(4);
            step4T.setRequiredRole(RoleType.ACCOUNTANT);
            step4T.setActionName("Validation Comptabilité");
            workflowStepRepository.save(step4T);

            // Workflow allégé pour les directions générales (sans DT)
            WorkflowTemplate dgTemplate = new WorkflowTemplate();
            dgTemplate.setName("Workflow Direction Générale (Manager -> DG -> Comptable)");
            dgTemplate = workflowTemplateRepository.save(dgTemplate);

            WorkflowStep step1G = new WorkflowStep();
            step1G.setTemplate(dgTemplate);
            step1G.setStepOrder(1);
            step1G.setRequiredRole(RoleType.MANAGER);
            step1G.setActionName("Validation Manager");
            workflowStepRepository.save(step1G);

            WorkflowStep step2G = new WorkflowStep();
            step2G.setTemplate(dgTemplate);
            step2G.setStepOrder(2);
            step2G.setRequiredRole(RoleType.GENERAL_DIRECTOR);
            step2G.setActionName("Validation Directeur Général");
            workflowStepRepository.save(step2G);

            WorkflowStep step3G = new WorkflowStep();
            step3G.setTemplate(dgTemplate);
            step3G.setStepOrder(3);
            step3G.setRequiredRole(RoleType.ACCOUNTANT);
            step3G.setActionName("Validation Comptabilité");
            workflowStepRepository.save(step3G);

            // 3. Création des Départements
            List<String> departementsNoms = Arrays.asList(
                    "Direction Technique - ALVANET",
                    "Direction Technique - SLF (Service Logiciel et Formation)",
                    "Direction Technique - SCR (Service Client et Réseau)",
                    "Direction Générale - Comptabilité",
                    "Direction Générale - RH",
                    "Direction Générale - Logistique",
                    "Direction Générale - Commerciaux"
            );

            for (String nom : departementsNoms) {
                Department dept = new Department();
                dept.setName(nom);
                
                // Assigner le bon workflow selon le type de direction
                if (nom.contains("Direction Technique")) {
                    dept.setDefaultWorkflowTemplate(techTemplate);
                } else {
                    dept.setDefaultWorkflowTemplate(dgTemplate);
                }
                
                departmentRepository.save(dept);
            }

            // 4. Création de quelques Utilisateurs de test
            Department deptDev = departmentRepository.findAll().stream()
                    .filter(d -> d.getName().contains("SLF"))
                    .findFirst().orElse(null);
                    
            Department deptCompta = departmentRepository.findAll().stream()
                    .filter(d -> d.getName().contains("Comptabilité"))
                    .findFirst().orElse(null);

            if (deptDev != null && deptCompta != null) {
                User employee = new User();
                employee.setName("Employé Test");
                employee.setEmail("employe@ids-technologie.com");
                employee.setPassword(passwordEncoder.encode("password")); // Hashé !
                employee.setRole(RoleType.EMPLOYEE);
                employee.setDepartment(deptDev);
                userRepository.save(employee);

                User manager = new User();
                manager.setName("Manager Test");
                manager.setEmail("manager@ids-technologie.com");
                manager.setPassword(passwordEncoder.encode("password"));
                manager.setRole(RoleType.MANAGER);
                manager.setDepartment(deptDev);
                userRepository.save(manager);
                
                // Mettre à jour le manager du département
                deptDev.setManager(manager);
                departmentRepository.save(deptDev);

                User comptable = new User();
                comptable.setName("Comptable Test");
                comptable.setEmail("comptable@ids-technologie.com");
                comptable.setPassword(passwordEncoder.encode("password"));
                comptable.setRole(RoleType.ACCOUNTANT);
                comptable.setDepartment(deptCompta);
                userRepository.save(comptable);

                User dt = new User();
                dt.setName("Directeur Technique Test");
                dt.setEmail("dt@ids-technologie.com");
                dt.setPassword(passwordEncoder.encode("password"));
                dt.setRole(RoleType.TECHNICAL_DIRECTOR);
                dt.setDepartment(deptDev);
                userRepository.save(dt);

                Department deptDg = departmentRepository.findAll().stream()
                        .filter(d -> d.getName().contains("Direction Générale"))
                        .findFirst().orElse(deptCompta);

                User dg = new User();
                dg.setName("Directeur Général Test");
                dg.setEmail("dg@ids-technologie.com");
                dg.setPassword(passwordEncoder.encode("password"));
                dg.setRole(RoleType.GENERAL_DIRECTOR);
                dg.setDepartment(deptDg);
                userRepository.save(dg);

                // 5. Création de l'Administrateur
                User admin = new User();
                admin.setName("Administrateur Système");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole(RoleType.ADMIN);
                // L'admin n'est pas forcément lié à un département technique/opérationnel
                userRepository.save(admin);
            }

            System.out.println("Initialisation des données terminée !");
        };
    }
}
