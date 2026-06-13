package swp391.aistudyhub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {

    String path = request.getServletPath();
    String authHeader = request.getHeader("Authorization");

    System.out.println("JWT FILTER PATH = " + path);
    System.out.println("AUTH HEADER = " + authHeader);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        System.out.println("NO BEARER TOKEN");
        filterChain.doFilter(request, response);
        return;
    }

    String token = authHeader.substring(7);

    try {
        boolean valid = jwtService.isTokenValid(token);
        boolean access = jwtService.isAccessToken(token);

        System.out.println("TOKEN VALID = " + valid);
        System.out.println("IS ACCESS TOKEN = " + access);

        if (SecurityContextHolder.getContext().getAuthentication() == null
                && valid
                && access) {

            String email = jwtService.extractEmail(token);
            System.out.println("TOKEN EMAIL = " + email);

            CustomUserDetails userDetails =
                    (CustomUserDetails) userDetailsService.loadUserByUsername(email);

            System.out.println("USER DETAILS = " + userDetails.getUsername());
            System.out.println("AUTHORITIES = " + userDetails.getAuthorities());

            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println("AUTHENTICATION SET SUCCESS");
        }
    } catch (Exception e) {
        System.out.println("JWT FILTER ERROR = " + e.getMessage());
        e.printStackTrace();
    }

    filterChain.doFilter(request, response);
}
}
