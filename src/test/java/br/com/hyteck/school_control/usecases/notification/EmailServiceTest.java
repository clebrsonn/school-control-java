package br.com.hyteck.school_control.usecases.notification;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnableAsync
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void testSendGenericNotificationEmail() throws Exception {
        // Configura o mock do JavaMailSender
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Executa o método assíncrono
        emailService.sendGenericNotificationEmail("test@example.com", "Test Subject", "<p>Test Body</p>");

        // Aguarda um tempo para garantir a execução assíncrona
        Thread.sleep(2000);

        // Verifica se o método de envio foi chamado
        verify(mailSender, times(1)).send(mimeMessage);

        // Captura o conteúdo do e-mail enviado
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sentMessage = captor.getValue();

        // Valida o destinatário e o assunto
        assertEquals("test@example.com", sentMessage.getAllRecipients()[0].toString());
    }
}