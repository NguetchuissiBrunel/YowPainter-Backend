package com.yowpainter.modules.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@yowpainter.com}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String to, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "Réinitialisation de votre mot de passe - YowPainter";
        String content = "<p>Bonjour,</p>"
                + "<p>Vous avez demandé la réinitialisation de votre mot de passe sur YowPainter.</p>"
                + "<p>Veuillez cliquer sur le lien ci-dessous pour changer votre mot de passe (ce lien expire dans 1 heure) :</p>"
                + "<p><a href=\"" + resetLink + "\">Changer mon mot de passe</a></p>"
                + "<p>Si vous n'avez pas demandé cette réinitialisation, ignorez cet e-mail.</p>"
                + "<p>L'équipe YowPainter</p>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("E-mail de réinitialisation envoyé à {}", to);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'e-mail à {}", to, e);
            // On log aussi le lien en cas d'échec d'envoi réel pour le dev/test
            log.info("LIEN DE SECOURS (DEBUG) : {}", resetLink);
        }
    }
}
