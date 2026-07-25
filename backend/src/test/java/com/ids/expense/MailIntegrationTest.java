package com.ids.expense;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
public class MailIntegrationTest {

    @Autowired
    private JavaMailSender mailSender;

    @Test
    public void testSendEmailReal() {
        System.out.println("==================================================");
        System.out.println("LANCEMENT DU TEST DE TRANSMISSION E-MAIL...");
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@ids.tg");
            message.setTo("mounasahm39@gmail.com");
            message.setSubject("[TEST DIREKT] Verification SMTP IDS");
            message.setText("Bonjour Mouna,\n\nCeci est un e-mail de confirmation envoye directement par le test Spring Boot pour valider la connexion SMTP mail.ids.tg !\n\nSucces garanti !");

            mailSender.send(message);
            System.out.println("SUCCESS: E-MAIL ENVOYE AVEC SUCCES A mounasahm39@gmail.com !");
            System.out.println("==================================================");
        } catch (Exception e) {
            System.err.println("FAILURE: ERREUR LORS DE L'ENVOI :");
            e.printStackTrace();
            System.out.println("==================================================");
            throw e;
        }
    }
}
