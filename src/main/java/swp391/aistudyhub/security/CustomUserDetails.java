package swp391.aistudyhub.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.AccountStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final AccountStatus accountStatus;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();

        String userRole = user.getRole();

        if (userRole != null && userRole.startsWith("ROLE_")) {
            userRole = userRole.substring(5);
        }

        this.role = userRole;
        this.accountStatus = user.getAccountStatus();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (role == null || role.isBlank()) {
            return authorities;
        }

        authorities.add(new SimpleGrantedAuthority(role));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus == null || accountStatus != AccountStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus == null || accountStatus == AccountStatus.ACTIVE;
    }
}