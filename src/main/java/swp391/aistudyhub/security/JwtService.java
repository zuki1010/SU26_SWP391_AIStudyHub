package swp391.aistudyhub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.config.JwtProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String TYPE_RESET = "reset";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(UUID userId, String email, String role) {
        return buildToken(userId, email, role, TYPE_ACCESS, jwtProperties.getAccessExpirationMs());
    }

    public String generateRefreshToken(UUID userId, String email, String role) {
        return buildToken(userId, email, role, TYPE_REFRESH, jwtProperties.getRefreshExpirationMs());
    }

    public String generateResetToken(UUID userId, String email) {
        return buildToken(userId, email, null, TYPE_RESET, jwtProperties.getResetExpirationMs());
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(extractClaim(token, claims -> claims.get(CLAIM_TYPE, String.class)));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(extractClaim(token, claims -> claims.get(CLAIM_TYPE, String.class)));
    }

    public boolean isResetToken(String token) {
        return TYPE_RESET.equals(extractClaim(token, claims -> claims.get(CLAIM_TYPE, String.class)));
    }

    public UUID extractUserId(String token) {
        String userId = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userId);
    }

    public String extractEmail(String token) {
        return extractSubject(token);
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private String buildToken(UUID userId, String email, String role, String type, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .setSubject(email)
                .claim("userId", userId.toString())
                .claim(CLAIM_TYPE, type)
                .setIssuedAt(now)
                .setExpiration(expiry);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.signWith(getSigningKey()).compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
