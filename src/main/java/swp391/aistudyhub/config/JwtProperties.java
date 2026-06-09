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

    private String secret = "ChangeMeInApplicationLocalPropertiesMustBeAtLeast256BitsLongForHS256!!";
    private long accessExpirationMs = 900_000;
    private long refreshExpirationMs = 604_800_000;
    private long resetExpirationMs = 3_600_000;
}
