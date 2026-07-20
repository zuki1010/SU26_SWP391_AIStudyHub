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
@Table(name = "user_member_subscription")
public class UserMemberSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "mem_id", updatable = false, nullable = false)
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
