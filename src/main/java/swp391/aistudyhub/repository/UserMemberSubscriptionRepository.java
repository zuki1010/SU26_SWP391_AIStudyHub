package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.entity.UserMemberSubscription;

import java.util.Optional;
import java.util.UUID;

public interface UserMemberSubscriptionRepository extends JpaRepository<UserMemberSubscription, UUID> {
    Optional<UserMemberSubscription> findByUser(User user);
}