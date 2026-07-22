package swp391.aistudyhub.service;

public interface EmailService {

    void sendVerificationEmail(String to, String token);

    void sendDocumentApprovedEmail(String to, String documentName);

    void sendDocumentDeniedEmail(String to, String documentName);

    void sendPaymentSuccessEmail(String to, String planName, Long amount);

    void sendPaymentFailedEmail(String to, String planName);

    void sendTestEmail(String to);
}