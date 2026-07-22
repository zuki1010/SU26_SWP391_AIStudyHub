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

        send(message, "Password reset OTP", toEmail);
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

        send(message, "Email verification OTP", toEmail);
    }

    @Override
    public void sendRoleChangedEmail(String toEmail, String newRole) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(toEmail);

        if ("MODERATOR".equalsIgnoreCase(newRole)) {
            message.setSubject("AI Study Hub - Tài khoản được nâng quyền");
            message.setText("""
                    Xin chào,

                    Tài khoản của bạn đã được nâng quyền lên Kiểm duyệt viên trên AI Study Hub.

                    Từ bây giờ, bạn có thể truy cập khu vực kiểm duyệt tài liệu và xử lý các yêu cầu công khai tài liệu.

                    AI Study Hub
                    """);
        } else if ("CUSTOMER".equalsIgnoreCase(newRole)) {
            message.setSubject("AI Study Hub - Tài khoản được cập nhật quyền");
            message.setText("""
                    Xin chào,

                    Tài khoản của bạn đã được hạ quyền về Người dùng trên AI Study Hub.

                    Bạn vẫn có thể sử dụng các chức năng học tập, quản lý tài liệu và chatbot như bình thường.

                    AI Study Hub
                    """);
        } else {
            message.setSubject("AI Study Hub - Cập nhật quyền tài khoản");
            message.setText("""
                    Xin chào,

                    Quyền tài khoản của bạn đã được cập nhật trên AI Study Hub.

                    AI Study Hub
                    """);
        }

        send(message, "Role changed email", toEmail);
    }

    @Override
    public void sendDocumentReviewResultEmail(
            String toEmail,
            String documentName,
            String decision
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(toEmail);

        if ("ACCEPT".equalsIgnoreCase(decision)) {
            message.setSubject("AI Study Hub - Tài liệu đã được duyệt");
            message.setText("""
                    Xin chào,

                    Tài liệu "%s" của bạn đã được duyệt và hiển thị công khai trên AI Study Hub.

                    AI Study Hub
                    """.formatted(documentName));
        } else {
            message.setSubject("AI Study Hub - Tài liệu bị từ chối");
            message.setText("""
                    Xin chào,

                    Tài liệu "%s" của bạn đã bị từ chối hiển thị công khai.

                    AI Study Hub
                    """.formatted(documentName));
        }

        send(message, "Document review result email", toEmail);
    }

    @Override
    public void sendDocumentReviewConfirmationEmail(
            String toEmail,
            String documentName,
            String decision
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(toEmail);

        if ("ACCEPT".equalsIgnoreCase(decision)) {
            message.setSubject("AI Study Hub - Xác nhận duyệt tài liệu");
            message.setText("""
                    Xin chào,

                    Bạn đã duyệt thành công tài liệu "%s".

                    AI Study Hub
                    """.formatted(documentName));
        } else {
            message.setSubject("AI Study Hub - Xác nhận từ chối tài liệu");
            message.setText("""
                    Xin chào,

                    Bạn đã từ chối tài liệu "%s".

                    AI Study Hub
                    """.formatted(documentName));
        }

        send(message, "Document review confirmation email", toEmail);
    }

    private void send(SimpleMailMessage message, String type, String toEmail) {
        if (mailSender == null) {
            log.warn("Mail is not configured. {} not sent to {}", type, toEmail);
            return;
        }

        try {
            mailSender.send(message);
            log.info("{} sent to {}", type, toEmail);
        } catch (MailException ex) {
            log.error("Could not send {} to {}", type, toEmail, ex);
            throw new RuntimeException(
                    "Could not send " + type + " to " + toEmail + ": " + ex.getMessage(),
                    ex
            );
        }
    }
}