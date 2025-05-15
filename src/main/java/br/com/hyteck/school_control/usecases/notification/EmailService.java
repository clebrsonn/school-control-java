package br.com.hyteck.school_control.usecases.notification;

import br.com.hyteck.school_control.models.Notification;
import br.com.hyteck.school_control.models.auth.VerificationToken;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService implements Notifications {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(VerificationToken verificationToken) {
        String subject = "Ativação de Conta - School Control";
        final String messageBody = getBodyMessage(verificationToken);

        try {
            sendGenericNotificationEmail(verificationToken.getUser().getEmail(), subject, messageBody);

            logger.info("Email de verificação enviado para {}", verificationToken.getUser().getEmail());
        } catch (MailException e) {
            logger.error("Erro ao enviar email de verificação para {}: {}", verificationToken.getUser().getEmail(), e.getMessage());
            throw e;
        }
    }

    private String getBodyMessage(VerificationToken verificationToken) {
        String verificationUrl = appBaseUrl + "/auth/verify?token=" + verificationToken.getToken(); // Endpoint de verificação
        return """
                <html><body>
                <h2>Bem-vindo ao School Control!</h2>
                <p>Clique no link abaixo para ativar sua conta:</p>
                <p><a href="%s">Ativar Minha Conta</a></p>
                <p>Se você não se registrou, por favor ignore este email.</p>
                <p>O link expira em 24 horas.</p>
                <br/>
                <p>Atenciosamente,<br/>Equipe School Control</p>
                </body></html>
                """.formatted(verificationUrl);
    }

    // Dentro de EmailService.java (exemplo de adaptação)

// ... (código existente)

    @SneakyThrows
    @Async
    @Retryable(
            retryFor = {MailException.class, MessagingException.class},
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendGenericNotificationEmail(String toEmail, String subject, String htmlBodyContent) {
        if(toEmail.isBlank()){
            logger.warn("Email de notificação genérica não enviado. Email vazio.");
            return;
        }
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBodyContent, true); // true para HTML

            mailSender.send(mimeMessage);
            logger.info("Email de notificação genérica enviado para {}", toEmail);
        } catch (MailException | MessagingException e) {
            logger.error("Erro ao enviar email de notificação genérica para {}: {}", toEmail, e.getMessage());
            throw e;
        }
    }

    // Método para construir o corpo do email a partir de uma Notification (exemplo)
    public String buildEmailBodyFromNotification(Notification notification) {
        String title = "Nova Notificação - School Control"; // Ou baseado no notification.getType()
        String callToAction = notification.getLink() != null ?
                String.format("<p><a href=\"%s%s\">Clique aqui para ver os detalhes</a></p>", appBaseUrl, notification.getLink()) :
                "";

        return String.format("""
                <html><body>
                <h2>%s</h2>
                <p>%s</p>
                %s
                <br/>
                <p>Atenciosamente,<br/>Equipe School Control</p>
                </body></html>
                """, title, notification.getMessage(), callToAction);
    }
}
