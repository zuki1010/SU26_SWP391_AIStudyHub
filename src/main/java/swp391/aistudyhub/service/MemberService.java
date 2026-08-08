package swp391.aistudyhub.service;

import swp391.aistudyhub.dto.response.MemberDetailResponseDTO;
import swp391.aistudyhub.dto.response.PaymentResponseDTO;

import java.util.List;
import java.util.Map;

public interface MemberService {

    PaymentResponseDTO createPremiumPayment();

    PaymentResponseDTO confirmPremiumPayment(Long orderCode);

    void cancelPremiumPayment(Long orderCode);

    void handlePaymentWebhook(Map<String, Object> payload);

    void registerMember();

    MemberDetailResponseDTO getMemberDetail();

    List<PaymentResponseDTO> getPaymentHistory();
}