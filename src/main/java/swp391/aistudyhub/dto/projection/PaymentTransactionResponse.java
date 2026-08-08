package swp391.aistudyhub.dto.projection;

import java.time.Instant;
import java.util.UUID;

public interface PaymentTransactionResponse {

    UUID getPaymentId();

    Long getOrderCode();

    Long getAmount();

    Long getDisplayPrice();

    String getStatus();

    String getDescription();

    String getTransferContent();

    Instant getCreatedAt();

    Instant getPaidAt();

    UUID getUserId();

    String getUserEmail();

    String getUserFullName();
}
