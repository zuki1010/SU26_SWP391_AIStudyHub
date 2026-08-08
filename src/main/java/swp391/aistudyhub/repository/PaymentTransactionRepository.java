package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.PaymentTransaction;
import swp391.aistudyhub.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByOrderCode(Long orderCode);

    List<PaymentTransaction> findByUserOrderByCreatedAtDesc(User user);
}