package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.entity.SubscriptionPlan;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.entity.UserMemberSubscription;
import swp391.aistudyhub.repository.SubscriptionPlanRepository;
import swp391.aistudyhub.repository.UserMemberSubscriptionRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.MemberService;

@Service
public class MemberServiceImpl implements MemberService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMemberSubscriptionRepository userMemberSubscriptionRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Override
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
                .orElseThrow(() -> new RuntimeException("This user don't have any member subscription"));

    }
}
