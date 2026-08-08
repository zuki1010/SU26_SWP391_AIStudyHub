package swp391.aistudyhub.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class PaymentResponseDTO {

    private Long orderCode;

    private Long amount;

    private Long displayPrice;

    private String status;

    private String checkoutUrl;

    private String qrCode;

    private String paymentLinkId;

    private String description;

    private String transferContent;

    private String bankCode;

    private String bankName;

    private String accountNumber;

    private String accountName;

    private Boolean isPremium;

    private String message;

    private Instant createdAt;

    private Instant paidAt;
}