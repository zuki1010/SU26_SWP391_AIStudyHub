package swp391.aistudyhub.service;

public interface MailService {

    void sendPasswordResetEmail(String toEmail, String otp);

    void sendVerificationEmail(String toEmail, String otp);

    void sendRoleChangedEmail(String toEmail, String newRole);

    void sendDocumentReviewResultEmail(
            String toEmail,
            String documentName,
            String decision
    );

    void sendDocumentReviewConfirmationEmail(
            String toEmail,
            String documentName,
            String decision
    );
}