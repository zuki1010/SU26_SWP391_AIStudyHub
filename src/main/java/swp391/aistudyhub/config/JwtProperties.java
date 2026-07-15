package swp391.aistudyhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Secret phải >= 32 ký tự.
     * Nên đặt trong application.properties hoặc biến môi trường khi deploy.
     */
    private String secret = "ChangeMeInApplicationLocalPropertiesMustBeAtLeast256BitsLongForHS256!!";

    /**
     * 7 ngày cho demo.
     */
    private long accessExpirationMs = 604_800_000L;

    /**
     * 7 ngày cho demo.
     */
    private long refreshExpirationMs = 604_800_000L;

    /**
     * 1 giờ.
     */
    private long resetExpirationMs = 3_600_000L;
}