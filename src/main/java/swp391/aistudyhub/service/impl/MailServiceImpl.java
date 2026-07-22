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
public void sendPasswordResetEmail(String toEmail, String otp) {
    SimpleMailMessage message = new SimpleMailMessage();

    message.setFrom(fromAddress);
    message.setTo(toEmail);
    message.setSubject("AI Study Hub - Password Reset OTP");
    message.setText("""
            Hello,

            Your AI Study Hub password reset code is:

            %s

            This code is valid for 15 minutes.

            If you did not request a password reset, please ignore this email.

            AI Study Hub
            """.formatted(otp));

    if (mailSender == null) {
        log.warn("Mail is not configured. Password reset OTP for {}: {}", toEmail, otp);
        return;
    }

    try {
        mailSender.send(message);
        log.info("Password reset OTP sent to {}", toEmail);
    } catch (MailException ex) {
        log.error("Could not send password reset OTP to {}", toEmail, ex);
        throw new RuntimeException(
                "Could not send password reset OTP to " + toEmail + ": " + ex.getMessage(),
                ex
        );
    }
}

  @Override
public void sendVerificationEmail(String toEmail, String otp) {
    SimpleMailMessage message = new SimpleMailMessage();

    message.setFrom(fromAddress);
    message.setTo(toEmail);
    message.setSubject("AI Study Hub - Email Verification");
    message.setText("""
            Hello,

            Your AI Study Hub verification code is:

            %s

            This code is valid for 15 minutes.

            If you did not create an account, please ignore this email.

            AI Study Hub
            """.formatted(otp));

    if (mailSender == null) {
        log.warn("Mail is not configured. Email verification OTP for {}: {}", toEmail, otp);
        return;
    }

    try {
        mailSender.send(message);
        log.info("Verification OTP sent to {}", toEmail);
    } catch (MailException ex) {
        throw new RuntimeException("Could not send verification email to " + toEmail, ex);
    }
}

  @Override
  public void sendRoleChangedEmail(String toEmail, String newRole) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'sendRoleChangedEmail'");
  }

  @Override
  public void sendDocumentReviewResultEmail(String toEmail, String documentName, String decision) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'sendDocumentReviewResultEmail'");
  }

  @Override
  public void sendDocumentReviewConfirmationEmail(String toEmail, String documentName, String decision) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'sendDocumentReviewConfirmationEmail'");
  }

  
}