package swp391.aistudyhub.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/verify-email",
            "/api/auth/resend-verification",
            "/api/email/test",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/reports/reasons"
    };

    private static final String[] USER_ROLES = {
            "CUSTOMER",
            "ROLE_CUSTOMER",
            "MODERATOR",
            "ROLE_MODERATOR",
            "ADMIN",
            "ROLE_ADMIN"
    };

    private static final String[] STAFF = {
            "MODERATOR",
            "ROLE_MODERATOR",
            "ADMIN",
            "ROLE_ADMIN"
    };

    private static final String[] ADMIN_ONLY = {
            "ADMIN",
            "ROLE_ADMIN"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/public/").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/admin/document").hasAnyAuthority(STAFF)
                        .requestMatchers(HttpMethod.GET, "/api/admin/document/").hasAnyAuthority(STAFF)

                        .requestMatchers(HttpMethod.PUT, "/api/v1/documents/*/review").hasAnyAuthority(STAFF)

                        .requestMatchers("/api/v1/documents").hasAnyAuthority(USER_ROLES)
                        .requestMatchers("/api/v1/documents/**").hasAnyAuthority(USER_ROLES)

                        .requestMatchers("/api/v1/storage").hasAnyAuthority(USER_ROLES)
                        .requestMatchers("/api/v1/storage/**").hasAnyAuthority(USER_ROLES)

                        .requestMatchers("/api/chat/**").hasAnyAuthority(USER_ROLES)

                        .requestMatchers(HttpMethod.POST, "/api/reports", "/api/reports/**").hasAnyAuthority(USER_ROLES)

                        .requestMatchers("/api/reports/pending", "/api/reports/*/process").hasAnyAuthority(STAFF)

                        .requestMatchers("/api/admin/**").hasAnyAuthority(ADMIN_ONLY)

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /*
     * PasswordEncoder chuyển tiếp:
     * - Mật khẩu mới: luôn encode bằng BCrypt.
     * - Mật khẩu cũ plain text: vẫn cho login tạm thời.
     * - Sau khi login thành công, AuthServiceImpl sẽ tự đổi plain text sang BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

            @Override
            public String encode(CharSequence rawPassword) {
                return bcrypt.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (rawPassword == null || encodedPassword == null) {
                    return false;
                }

                if (isBcryptHash(encodedPassword)) {
                    return bcrypt.matches(rawPassword, encodedPassword);
                }

                // Hỗ trợ tạm tài khoản cũ đang lưu plain text.
                return encodedPassword.equals(rawPassword.toString());
            }

            private boolean isBcryptHash(String value) {
                return value.startsWith("$2a$")
                        || value.startsWith("$2b$")
                        || value.startsWith("$2y$");
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://aistudyfe.onrender.com"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}