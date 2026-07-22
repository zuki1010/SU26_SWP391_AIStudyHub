package swp391.aistudyhub.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.service.EmailService;

import jakarta.mail.internet.MimeMessage;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void sendVerificationEmail(String to, String token) {
        String verifyLink = frontendUrl + "/verify-email?token=" + token;

        String subject = "AI Study Hub - Email Verification";

        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #2563eb;">AI Study Hub</h2>
                    <p>Hello,</p>
                    <p>Thank you for registering an account at AI Study Hub.</p>
                    <p>Please click the button below to verify your email address:</p>
                    <p>
                        <a href="%s"
                           style="display:inline-block;padding:12px 18px;background:#2563eb;color:white;
                                  text-decoration:none;border-radius:8px;">
                            Verify Email
                        </a>
                    </p>
                    <p>If the button does not work, copy this link:</p>
                    <p>%s</p>
                    <p>This link will expire soon.</p>
                </div>
                """.formatted(verifyLink, verifyLink);

        sendHtmlEmail(to, subject, html);
    }

    @Override
    public void sendDocumentApprovedEmail(String to, String documentName) {
        String subject = "AI Study Hub - Document Approved";

        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #16a34a;">Document Approved</h2>
                    <p>Your document has been approved and is now public.</p>
                    <p><b>Document:</b> %s</p>
                    <p>Thank you for sharing learning materials with the community.</p>
                </div>
                """.formatted(documentName);

        sendHtmlEmail(to, subject, html);
    }

    @Override
    public void sendDocumentDeniedEmail(String to, String documentName) {
        String subject = "AI Study Hub - Document Denied";

        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #dc2626;">Document Denied</h2>
                    <p>Your document was not approved for public sharing.</p>
                    <p><b>Document:</b> %s</p>
                    <p>You can review the document and submit another request later.</p>
                </div>
                """.formatted(documentName);

        sendHtmlEmail(to, subject, html);
    }

    @Override
    public void sendPaymentSuccessEmail(String to, String planName, Long amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String amountText = formatter.format(amount);

        String subject = "AI Study Hub - Payment Successful";

        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #16a34a;">Payment Successful</h2>
                    <p>Your payment has been completed successfully.</p>
                    <p><b>Plan:</b> %s</p>
                    <p><b>Amount:</b> %s</p>
                    <p>Thank you for using AI Study Hub.</p>
                </div>
                """.formatted(planName, amountText);

        sendHtmlEmail(to, subject, html);
    }

    @Override
    public void sendPaymentFailedEmail(String to, String planName) {
        String subject = "AI Study Hub - Payment Failed";

        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #dc2626;">Payment Failed</h2>
                    <p>Your payment could not be completed.</p>
                    <p><b>Plan:</b> %s</p>
                    <p>Please try again later.</p>
                </div>
                """.formatted(planName);

        sendHtmlEmail(to, subject, html);
    }

    @Override
    public void sendTestEmail(String to) {
        String subject = "AI Study Hub - Test Email";

        String html = """
                <div style="font-family: Arial, sans-serif;">
                    <h2>AI Study Hub</h2>
                    <p>This is a test email from AI Study Hub.</p>
                </div>
                """;

        sendHtmlEmail(to, subject, html);
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Send email failed: " + e.getMessage(), e);
        }
    }
}