package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.repository.CustomerProfileRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.AdminService;

import java.util.List;
import java.util.UUID;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;


    @Override
    public Page<User> getAllCustomer(String key, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (key != null && !key.isEmpty()) {
            Page<User> user1 = userRepository.findByEmailContainingIgnoreCaseOrCustomerProfileFullNameContainingIgnoreCase(key,key,pageable);
            return userRepository.findByEmailContainingIgnoreCaseOrCustomerProfileFullNameContainingIgnoreCase(key, key, pageable);
        }

        return userRepository.findAll(pageable);
    }

    @Override
    public User updateUserStatus(UUID id, AccountStatus status) {
        User user = userRepository.findUserById(id)
                .orElseThrow(() -> new RuntimeException("This user is not exist!"));

        userRepository.updateUserStatus(id, status);

        user.setAccountStatus(status);
        return user;
    }
}
