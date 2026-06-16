package swp391.aistudyhub.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> getAllUserAccount() {
        return userRepository.findAll();
    }
}
