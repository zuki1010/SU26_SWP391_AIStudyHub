package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.response.PaymentResponseDTO;
import swp391.aistudyhub.entity.PaymentTransaction;
import swp391.aistudyhub.entity.SubscriptionPlan;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.entity.UserMemberSubscription;
import swp391.aistudyhub.enums.MemberStatus;
import swp391.aistudyhub.repository.PaymentTransactionRepository;
import swp391.aistudyhub.repository.SubscriptionPlanRepository;
import swp391.aistudyhub.repository.UserMemberSubscriptionRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.EmailService;
import swp391.aistudyhub.service.MemberService;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMemberSubscriptionRepository userMemberSubscriptionRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private EmailService emailService;

    @Value("${payment.bank-code:TCB}")
    private String bankCode;

    @Value("${payment.bank-name:Techcombank}")
    private String bankName;

    @Value("${payment.account-number:}")
    private String accountNumber;

    @Value("${payment.account-name:AI STUDY HUB}")
    private String accountName;

    @Value("${payment.amount:0}")
    private Long paymentAmount;

    @Value("${payment.display-price:99000}")
    private Long displayPrice;

    @Override
    @Transactional
    public PaymentResponseDTO createPremiumPayment() {
        User user = getCurrentUser();

        long orderCode = generateOrderCode();
        long amount = paymentAmount == null ? 0L : paymentAmount;
        long price = displayPrice == null ? resolveAmount(getPremiumPlan()) : displayPrice;

        String transferContent = "AISH" + orderCode;
        String description = "Premium " + transferContent;
        String qrUrl = buildVietQrUrl(amount, transferContent);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUser(user);
        transaction.setOrderCode(orderCode);
        transaction.setAmount(amount);
        transaction.setDisplayPrice(price);
        transaction.setDescription(description);
        transaction.setTransferContent(transferContent);
        transaction.setBankCode(bankCode);
        transaction.setBankName(bankName);
        transaction.setAccountNumber(accountNumber);
        transaction.setAccountName(accountName);
        transaction.setStatus("PENDING_CONFIRMATION");
        transaction.setPaymentLinkId("BANK-" + orderCode);
        transaction.setCheckoutUrl(qrUrl);
        transaction.setQrCode(qrUrl);

        paymentTransactionRepository.save(transaction);

        return mapPaymentResponse(
                transaction,
                false,
                "Phiên giao dịch đã được tạo."
        );
    }

    @Override
    @Transactional
    public PaymentResponseDTO confirmPremiumPayment(Long orderCode) {
        if (orderCode == null) {
            throw new RuntimeException("Thiếu mã giao dịch.");
        }

        User currentUser = getCurrentUser();

        PaymentTransaction transaction = paymentTransactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên giao dịch."));

        if (!Objects.equals(transaction.getUser().getId(), currentUser.getId())) {
            throw new RuntimeException("Phiên giao dịch không thuộc tài khoản hiện tại.");
        }

        if ("MANUAL_CONFIRMED".equalsIgnoreCase(transaction.getStatus())) {
            return mapPaymentResponse(
                    transaction,
                    true,
                    "Tài khoản Premium đã được kích hoạt."
            );
        }

        transaction.setStatus("MANUAL_CONFIRMED");
        transaction.setPaidAt(Instant.now());

        SubscriptionPlan subscriptionPlan = getPremiumPlan();

        activatePremium(
                transaction.getUser(),
                subscriptionPlan,
                transaction.getAmount()
        );

        paymentTransactionRepository.save(transaction);

        return mapPaymentResponse(
                transaction,
                true,
                "Tài khoản Premium đã được kích hoạt."
        );
    }

    @Override
    @Transactional
    public void handlePaymentWebhook(Map<String, Object> payload) {
    }

    @Override
    @Transactional
    public void registerMember() {
        throw new RuntimeException("Vui lòng xác nhận giao dịch trước khi kích hoạt Premium.");
    }

    @Override
    @Transactional(readOnly = true)
    public UserMemberSubscription getMemberDetail() {
        User user = getCurrentUser();

        return userMemberSubscriptionRepository.findByUser(user).orElse(null);
    }

    private void activatePremium(
            User user,
            SubscriptionPlan subscriptionPlan,
            Long mailAmount
    ) {
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

            boolean stillActive = userMemberSubscription.getStatus() == MemberStatus.ACTIVE
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

        try {
            emailService.sendPaymentSuccessEmail(
                    user.getEmail(),
                    resolvePlanName(subscriptionPlan),
                    mailAmount == null ? 0L : mailAmount
            );
        } catch (Exception emailError) {
            System.out.println("Premium activation email error: " + emailError.getMessage());
        }
    }

    private SubscriptionPlan getPremiumPlan() {
        return subscriptionPlanRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Gói Premium hiện chưa khả dụng."));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new RuntimeException("Vui lòng đăng nhập để tiếp tục.");
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản hiện tại."));
    }

    private long generateOrderCode() {
        long now = System.currentTimeMillis();
        long orderCode = now % 1_000_000_000L;

        while (paymentTransactionRepository.findByOrderCode(orderCode).isPresent()) {
            orderCode++;
        }

        return orderCode;
    }

    private String buildVietQrUrl(Long amount, String transferContent) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new RuntimeException("Thiếu số tài khoản Techcombank.");
        }

        String cleanBankCode = isBlank(bankCode) ? "TCB" : bankCode.trim();
        String cleanAccountNo = accountNumber.trim();
        String cleanAccountName = isBlank(accountName) ? "AI STUDY HUB" : accountName.trim();

        String encodedContent = encode(transferContent);
        String encodedName = encode(cleanAccountName);

        long qrAmount = amount == null ? 0L : amount;

        return "https://img.vietqr.io/image/"
                + cleanBankCode
                + "-"
                + cleanAccountNo
                + "-compact2.png"
                + "?amount="
                + qrAmount
                + "&addInfo="
                + encodedContent
                + "&accountName="
                + encodedName;
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value == null ? "" : value,
                StandardCharsets.UTF_8
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
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

    private PaymentResponseDTO mapPaymentResponse(
            PaymentTransaction transaction,
            boolean isPremium,
            String message
    ) {
        PaymentResponseDTO dto = new PaymentResponseDTO();

        dto.setOrderCode(transaction.getOrderCode());
        dto.setAmount(transaction.getAmount());
        dto.setDisplayPrice(
                transaction.getDisplayPrice() == null
                        ? resolveAmount(getPremiumPlan())
                        : transaction.getDisplayPrice()
        );
        dto.setStatus(transaction.getStatus());
        dto.setCheckoutUrl(transaction.getCheckoutUrl());
        dto.setQrCode(transaction.getQrCode());
        dto.setPaymentLinkId(transaction.getPaymentLinkId());
        dto.setDescription(transaction.getDescription());
        dto.setTransferContent(transaction.getTransferContent());
        dto.setBankCode(transaction.getBankCode());
        dto.setBankName(transaction.getBankName());
        dto.setAccountNumber(transaction.getAccountNumber());
        dto.setAccountName(transaction.getAccountName());
        dto.setIsPremium(isPremium);
        dto.setMessage(message);

        return dto;
    }
}