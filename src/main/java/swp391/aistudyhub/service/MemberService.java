package swp391.aistudyhub.service;

import swp391.aistudyhub.dto.response.PaymentResponseDTO;
import swp391.aistudyhub.entity.UserMemberSubscription;

import java.util.Map;

public interface MemberService {

    PaymentResponseDTO createPremiumPayment();

    PaymentResponseDTO confirmPremiumPayment(Long orderCode);

    void handlePaymentWebhook(Map<String, Object> payload);

    void registerMember();

    UserMemberSubscription getMemberDetail();
}