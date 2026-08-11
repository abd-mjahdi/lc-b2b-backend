package com.lesieurcristal.b2bportal.notification;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Override
    public void sendAccountActivationEmail(AccountActivationEmail email) {
        log.info(
                """
                ==========================================================
                [ACTIVATION EMAIL GENERATED]
                To: {}
                Login: {}
                Activation URL: {}
                Expires At: {}
                ==========================================================
                """,
                email.recipientEmail(),
                email.login(),
                email.activationUrl(),
                email.expiresAt()
        );

        if (mailFrom == null || mailFrom.isBlank()) {
            log.info("[EMAIL SERVICE] MAIL_USERNAME not set. Email delivery simulated via log above.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(mailFrom);
            helper.setTo(email.recipientEmail());
            helper.setSubject("Lesieur Cristal B2B - Activation de votre compte entreprise");

            String htmlBody = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
                  <div style="background-color: #7b1c1c; padding: 24px; text-align: center;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 24px;">Lesieur Cristal B2B</h1>
                    <p style="color: #f1f5f9; margin: 4px 0 0 0; font-size: 14px;">Activation de votre compte professionnel</p>
                  </div>
                  <div style="padding: 32px; background-color: #ffffff;">
                    <h2 style="color: #1e293b; margin-top: 0; font-size: 20px;">Bonjour %s,</h2>
                    <p style="color: #475569; font-size: 15px; line-height: 1.6;">
                      Un compte entreprise Lesieur Cristal a été créé pour vous avec l'identifiant <strong>%s</strong>.
                    </p>
                    <p style="color: #475569; font-size: 15px; line-height: 1.6;">
                      Veuillez cliquer sur le bouton ci-dessous pour configurer votre mot de passe et activer votre accès au Portail B2B :
                    </p>
                    <div style="text-align: center; margin: 32px 0;">
                      <a href="%s" style="background-color: #7b1c1c; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block;">Activer mon compte</a>
                    </div>
                    <p style="color: #64748b; font-size: 13px; line-height: 1.5;">
                      Ou copiez et collez ce lien dans votre navigateur :<br/>
                      <a href="%s" style="color: #0284c7;">%s</a>
                    </p>
                    <p style="color: #94a3b8; font-size: 12px; margin-top: 24px;">
                      Ce lien expire le %s. Si vous n'avez pas demandé ce compte, vous pouvez ignorer cet e-mail.
                    </p>
                  </div>
                  <div style="background-color: #f8fafc; padding: 16px; text-align: center; border-top: 1px solid #e2e8f0;">
                    <p style="color: #64748b; font-size: 12px; margin: 0;">&copy; Lesieur Cristal Avril. Tous droits réservés.</p>
                  </div>
                </div>
                """.formatted(
                    email.recipientFirstName() != null ? email.recipientFirstName() : email.login(),
                    email.login(),
                    email.activationUrl(),
                    email.activationUrl(),
                    email.activationUrl(),
                    email.expiresAt()
            );

            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("[EMAIL SERVICE] Activation email sent successfully to {}", email.recipientEmail());

        } catch (Exception ex) {
            log.error("[EMAIL SERVICE] Failed to send email to {}: {}", email.recipientEmail(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to send activation email", ex);
        }
    }
}
