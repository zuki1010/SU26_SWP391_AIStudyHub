package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.dto.projection.PaymentTransactionResponse;
import swp391.aistudyhub.entity.PaymentTransaction;
import swp391.aistudyhub.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByOrderCode(Long orderCode);

    List<PaymentTransaction> findByUserOrderByCreatedAtDesc(User user);

    Optional<PaymentTransaction> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, String status);

    @Query(
            value = """
                    SELECT
                        pt.payment_id AS paymentId,
                        pt.order_code AS orderCode,
                        pt.amount AS amount,
                        pt.display_price AS displayPrice,
                        pt.status AS status,
                        pt.description AS description,
                        pt.transfer_content AS transferContent,
                        pt.created_at AS createdAt,
                        pt.paid_at AS paidAt,

                        u.user_id AS userId,
                        u.email AS userEmail,

                        COALESCE(
                            cp.full_name,
                            mp.full_name,
                            ap.full_name,
                            u.email
                        ) AS userFullName
                    FROM payment_transactions pt
                    LEFT JOIN users u ON u.user_id = pt.user_id
                    LEFT JOIN customer_profiles cp ON cp.user_id = u.user_id
                    LEFT JOIN moderator_profiles mp ON mp.user_id = u.user_id
                    LEFT JOIN admin_profiles ap ON ap.user_id = u.user_id
                    WHERE (CAST(:status AS varchar) IS NULL OR pt.status = CAST(:status AS varchar))
                    ORDER BY pt.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM payment_transactions pt
                    WHERE (CAST(:status AS varchar) IS NULL OR pt.status = CAST(:status AS varchar))
                    """,
            nativeQuery = true
    )
    Page<PaymentTransactionResponse> findAllAdminPayments(
            @Param("status") String status,
            Pageable pageable
    );
}