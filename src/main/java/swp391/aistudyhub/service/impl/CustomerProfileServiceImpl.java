package swp391.aistudyhub.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.entity.CustomerProfile;
import swp391.aistudyhub.repository.CustomerProfileRepository;
import swp391.aistudyhub.service.CustomerProfileService;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;

    @Override
    public Optional<CustomerProfile> findByUserId(UUID userId) {
        return customerProfileRepository.findByUser_Id(userId);
    }
}
