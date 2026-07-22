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

    @Value("${spring.mail.username:${app.mail.from:noreply@aistudyhub.local}}")
    private String fromAddress;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("AI Study Hub - Password Reset");
        message.setText("""
                Hello,

                You requested to reset your password for AI Study Hub.

                Please use the link below to reset your password:
                %s

                This link is valid for 1 hour.

                If you did not request this, please ignore this email.

                AI Study Hub
                """.formatted(resetLink));

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

    @Override
    public void sendVerificationEmail(String toEmail, String verifyLink) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("AI Study Hub - Verify Your Email");
        message.setText("""
                Hello,

                Thank you for registering an account at AI Study Hub.

                Please click the link below to verify your email:
                %s

                This verification link is valid for 15 minutes.

                If you did not create an account, please ignore this email.

                AI Study Hub
                """.formatted(verifyLink));

        if (mailSender == null) {
            log.warn("Mail is not configured. Email verification link for {}: {}", toEmail, verifyLink);
            return;
        }

        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (MailException ex) {
            throw new RuntimeException("Could not send verification email to " + toEmail, ex);
        }
    }
}