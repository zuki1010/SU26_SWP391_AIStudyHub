package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import swp391.aistudyhub.enums.MemberStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "user_member_subcription")
public class UserMemberSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan subscriptionPlan;

    private Instant startDate;
    private Instant endDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MemberStatus status;
}
