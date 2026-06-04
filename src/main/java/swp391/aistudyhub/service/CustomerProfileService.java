package swp391.aistudyhub.service;

import swp391.aistudyhub.entity.CustomerProfile;

import java.util.Optional;
import java.util.UUID;

public interface CustomerProfileService {

    Optional<CustomerProfile> findByUserId(UUID userId);
}
