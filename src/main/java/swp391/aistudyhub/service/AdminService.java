package swp391.aistudyhub.service;

import org.springframework.data.domain.Page;
import swp391.aistudyhub.entity.User;

import java.util.List;

public interface AdminService {
    Page<User> getAllCustomer(String key, int page, int size);
}
