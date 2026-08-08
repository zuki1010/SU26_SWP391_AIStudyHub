package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "payment_transactions",
        indexes = {
                @Index(name = "idx_payment_order_code", columnList = "order_code", unique = true),
                @Index(name = "idx_payment_user_id", columnList = "user_id")
        }
)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @Column(name = "amount", nullable = false)
    private Long amount = 0L;

    @Column(name = "display_price")
    private Long displayPrice = 99000L;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "transfer_content", length = 255)
    private String transferContent;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "account_name", length = 255)
    private String accountName;

    @Column(name = "checkout_url", columnDefinition = "TEXT")
    private String checkoutUrl;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "payment_link_id", length = 100)
    private String paymentLinkId;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING_CONFIRMATION";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "paid_at")
    private Instant paidAt;
}