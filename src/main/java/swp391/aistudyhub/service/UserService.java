package swp391.aistudyhub.service;

import swp391.aistudyhub.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);
}
