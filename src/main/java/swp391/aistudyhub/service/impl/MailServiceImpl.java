package swp391.aistudyhub.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.service.MailService;

@Slf4j
@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    public MailServiceImpl(@org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${app.mail.from:noreply@aistudyhub.local}")
    private String fromAddress;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("AI Study Hub - Password Reset");
        message.setText("Use the link below to reset your password (valid for 1 hour):\n\n" + resetLink);

        if (mailSender == null) {
            log.warn("Mail is not configured. Password reset link for {}: {}", toEmail, resetLink);
            return;
        }
        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException ex) {
            log.warn("Could not send reset email to {}. Reset link: {}", toEmail, resetLink);
        }
    }
}
