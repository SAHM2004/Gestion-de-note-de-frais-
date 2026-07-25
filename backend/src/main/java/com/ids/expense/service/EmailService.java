package com.ids.expense.service;

import com.ids.expense.common.models.ExpenseReport;
import com.ids.expense.common.models.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public void sendExpenseSubmitted(User employee, ExpenseReport report) {
        String subject = "[IDS] Note de frais soumise — " + report.getTitle();
        String body = String.format(
            "Bonjour %s,\n\n" +
            "Votre note de frais « %s » a été soumise avec succès et est en cours de validation.\n\n" +
            "Montant total : %s FCFA\n" +
            "Période : %s au %s\n\n" +
            "Vous recevrez une notification à chaque étape du circuit de validation.\n\n" +
            "Cordialement,\nService Notes de Frais — IDS Technologie",
            employee.getName(), report.getTitle(), formatAmount(report),
            report.getDateFrom(), report.getDateTo()
        );
        send(employee.getEmail(), subject, body);
    }

    public void sendExpenseApproved(User employee, ExpenseReport report, String stepName, String approverName, boolean isFinalApproval) {
        if (isFinalApproval) {
            String subject = "[IDS] Note de frais approuvée — Remboursement disponible";
            String body = String.format(
                "Bonjour %s,\n\n" +
                "Bonne nouvelle ! Votre note de frais « %s » a été entièrement approuvée par le service comptabilité.\n\n" +
                "Montant remboursable : %s FCFA\n\n" +
                "Conformément à la procédure IDS (standard Odoo), vous pouvez vous rendre au service comptabilité " +
                "muni de votre pièce d'identité pour retirer votre remboursement.\n\n" +
                "Horaires : du lundi au vendredi, 9h — 16h.\n\n" +
                "Cordialement,\nService Comptabilité — IDS Technologie",
                employee.getName(), report.getTitle(), formatAmount(report)
            );
            send(employee.getEmail(), subject, body);
        } else {
            String subject = "[IDS] Note de frais validée — " + stepName;
            String body = String.format(
                "Bonjour %s,\n\n" +
                "Votre note de frais « %s » a été validée à l'étape : %s.\n" +
                "Validée par : %s\n\n" +
                "Montant : %s FCFA\n" +
                "La note passe à l'étape suivante du circuit de validation.\n\n" +
                "Cordialement,\nService Notes de Frais — IDS Technologie",
                employee.getName(), report.getTitle(), stepName, approverName, formatAmount(report)
            );
            send(employee.getEmail(), subject, body);
        }
    }

    public void sendExpenseRejected(User employee, ExpenseReport report, String stepName, String rejectorName, String reason) {
        String subject = "[IDS] Note de frais refusée — " + report.getTitle();
        String body = String.format(
            "Bonjour %s,\n\n" +
            "Votre note de frais « %s » a été refusée à l'étape : %s.\n" +
            "Refusée par : %s\n\n" +
            "Motif : %s\n\n" +
            "Montant concerné : %s FCFA\n\n" +
            "Vous pouvez modifier votre note et la soumettre à nouveau depuis l'application.\n\n" +
            "Cordialement,\nService Notes de Frais — IDS Technologie",
            employee.getName(), report.getTitle(), stepName, rejectorName,
            reason != null ? reason : "Non précisé", formatAmount(report)
        );
        send(employee.getEmail(), subject, body);
    }

    public void sendValidationRequested(User approver, User employee, ExpenseReport report, String stepName) {
        if (approver == null || approver.getEmail() == null || approver.getEmail().isBlank()) return;
        String subject = "[IDS - Odoo] Action requise : Note de frais à valider — " + report.getTitle();
        String body = String.format(
            "Bonjour %s,\n\n" +
            "Une nouvelle note de frais nécessite votre validation à l'étape : %s.\n\n" +
            "Demandeur : %s\n" +
            "Intitulé : %s\n" +
            "Montant total : %s FCFA\n\n" +
            "Veuillez vous connecter à l'application Gestion Notes de Frais pour consulter et valider cette demande.\n\n" +
            "Cordialement,\nService Notes de Frais — IDS Technologie",
            approver.getName(), stepName, employee.getName(), report.getTitle(), formatAmount(report)
        );
        send(approver.getEmail(), subject, body);
    }

    private String formatAmount(ExpenseReport report) {
        if (report.getLines() == null || report.getLines().isEmpty()) return "0";
        BigDecimal total = report.getLines().stream()
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.toPlainString();
    }

    private void send(String to, String subject, String body) {
        if (!mailEnabled || fromEmail == null || fromEmail.isBlank()) {
            log.info("=== NOTIFICATION E-MAIL (simulation) ===");
            log.info("Destinataire : {}", to);
            log.info("Objet : {}", subject);
            log.info("Message :\n{}", body);
            log.info("========================================");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("E-mail envoyé à {}", to);
        } catch (Exception e) {
            log.error("Échec envoi e-mail à {} : {}", to, e.getMessage());
        }
    }
}
