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
import swp391.aistudyhub.service.MemberService;

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

    @Override
    @Transactional
    public void registerMember() {
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }

        User user = userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("This subscription is not available"));

        UserMemberSubscription userMemberSubscription = userMemberSubscriptionRepository.findByUser(user)
                .orElse(null);
        Instant now = Instant.now();

        if(userMemberSubscription == null) {
            userMemberSubscription = new UserMemberSubscription();
            userMemberSubscription.setUser(user);
            userMemberSubscription.setSubscriptionPlan(subscriptionPlan);
            userMemberSubscription.setStartDate(now);
            userMemberSubscription.setEndDate(now.plus(30, ChronoUnit.DAYS));
            userMemberSubscription.setStatus(MemberStatus.ACTIVE);
        } else {
            userMemberSubscription.setSubscriptionPlan(subscriptionPlan);
            if(userMemberSubscription.getStatus() == MemberStatus.ACTIVE && userMemberSubscription.getEndDate().isAfter(now)) {
                userMemberSubscription.setEndDate(userMemberSubscription.getEndDate().plus(30, ChronoUnit.DAYS));
            } else {
                userMemberSubscription.setStartDate(now);
                userMemberSubscription.setEndDate(now.plus(30, ChronoUnit.DAYS));
                userMemberSubscription.setStatus(MemberStatus.ACTIVE);
            }
        }

        userMemberSubscriptionRepository.save(userMemberSubscription);
    }
}
