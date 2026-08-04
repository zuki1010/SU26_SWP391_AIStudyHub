package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.entity.SubscriptionPlan;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.entity.UserMemberSubscription;
import swp391.aistudyhub.enums.MemberStatus;
import swp391.aistudyhub.repository.SubscriptionPlanRepository;
import swp391.aistudyhub.repository.UserMemberSubscriptionRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.EmailService;
import swp391.aistudyhub.service.MemberService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMemberSubscriptionRepository userMemberSubscriptionRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private EmailService emailService;

    @Override
    @Transactional
    public void registerMember() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new RuntimeException("You are not login yet!");
        }

        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("This subscription is not available"));

        UserMemberSubscription userMemberSubscription =
                userMemberSubscriptionRepository.findByUser(user).orElse(null);

        Instant now = Instant.now();

        if (userMemberSubscription == null) {
            userMemberSubscription = new UserMemberSubscription();

            userMemberSubscription.setUser(user);
            userMemberSubscription.setSubscriptionPlan(subscriptionPlan);
            userMemberSubscription.setStartDate(now);
            userMemberSubscription.setEndDate(now.plus(30, ChronoUnit.DAYS));
            userMemberSubscription.setStatus(MemberStatus.ACTIVE);
        } else {
            userMemberSubscription.setSubscriptionPlan(subscriptionPlan);

            boolean stillActive =
                    userMemberSubscription.getStatus() == MemberStatus.ACTIVE
                            && userMemberSubscription.getEndDate() != null
                            && userMemberSubscription.getEndDate().isAfter(now);

            if (stillActive) {
                userMemberSubscription.setEndDate(
                        userMemberSubscription.getEndDate().plus(30, ChronoUnit.DAYS)
                );
            } else {
                userMemberSubscription.setStartDate(now);
                userMemberSubscription.setEndDate(now.plus(30, ChronoUnit.DAYS));
                userMemberSubscription.setStatus(MemberStatus.ACTIVE);
            }
        }

        userMemberSubscriptionRepository.save(userMemberSubscription);

        /*
         * Gửi hóa đơn email.
         * Nếu gửi mail lỗi thì vẫn giữ Premium đã kích hoạt,
         * tránh trường hợp thanh toán thành công nhưng tài khoản không được nâng cấp.
         */
        try {
            emailService.sendPaymentSuccessEmail(
                    user.getEmail(),
                    resolvePlanName(subscriptionPlan),
                    resolveAmount(subscriptionPlan)
            );
        } catch (Exception emailError) {
            System.out.println("Send payment invoice email failed: " + emailError.getMessage());
        }
    }

    @Override
    public UserMemberSubscription getMemberDetail() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new RuntimeException("You are not login yet!");
        }

        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("This subscription is not available"));

        UserMemberSubscription userMemberSubscription =
                userMemberSubscriptionRepository.findByUser(user).orElse(null);

        if(userMemberSubscription != null) {
            return userMemberSubscription;
        }
        return null;
    }

    private String resolvePlanName(SubscriptionPlan subscriptionPlan) {
        if (subscriptionPlan == null
                || subscriptionPlan.getPlanName() == null
                || subscriptionPlan.getPlanName().isBlank()) {
            return "Premium";
        }

        return subscriptionPlan.getPlanName();
    }

    private Long resolveAmount(SubscriptionPlan subscriptionPlan) {
        if (subscriptionPlan == null || subscriptionPlan.getPrice() == null) {
            return 99000L;
        }

        BigDecimal price = subscriptionPlan.getPrice();

        return price.longValue();
    }
}