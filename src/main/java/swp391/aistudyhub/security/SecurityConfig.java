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
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
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
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] CUSTOMER_OR_ADMIN = {
            "CUSTOMER",
            "ROLE_CUSTOMER",
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

                        // Public APIs
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/public/").permitAll()

                        // Staff review public-document requests. Must stay before /api/v1/documents/**.
                        .requestMatchers(HttpMethod.PUT, "/api/v1/documents/*/review").hasAnyAuthority(STAFF)

                        // Documents: CUSTOMER and ADMIN can use upload/manage/share APIs.
                        .requestMatchers("/api/v1/documents").hasAnyAuthority(CUSTOMER_OR_ADMIN)
                        .requestMatchers("/api/v1/documents/**").hasAnyAuthority(CUSTOMER_OR_ADMIN)

                        // Storage is used by document upload and dashboard.
                        .requestMatchers("/api/v1/storage").hasAnyAuthority(CUSTOMER_OR_ADMIN)
                        .requestMatchers("/api/v1/storage/**").hasAnyAuthority(CUSTOMER_OR_ADMIN)

                        // Chat is available for regular users and admins who test the system.
                        .requestMatchers("/api/chat/**").hasAnyAuthority(CUSTOMER_OR_ADMIN)

                        // Admin dashboard APIs.
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

    /**
     * Demo mode: old project data is stored as plain text.
     * Change to BCryptPasswordEncoder after the team migrates existing passwords.
     */
    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
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