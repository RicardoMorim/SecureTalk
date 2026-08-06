package ricardo.messagingapp.messagingcore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@ConfigurationProperties(
        prefix = "secure-talk"
)
@Getter
@Setter
@Component
public class Properties {

    private RateLimiterConfiguration rateLimiter = new RateLimiterConfiguration();

    @Getter
    @Setter
    public static class RateLimiterConfiguration {
        private int maxRequests = 150;
        private long timeWindowMs = 60000L;
    }

}
