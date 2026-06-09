package swp391.aistudyhub.service;

public interface MailService {

    void sendPasswordResetEmail(String toEmail, String resetLink);
}
