package br.com.cooperativevoting.voting.adapter.out.client.userinfo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "integrations.user-info")
public record UserInfoClientProperties(
    String baseUrl,
    Retry retry,
    Cache cache
) {

    public UserInfoClientProperties {
        retry = retry == null ? Retry.defaults() : retry;
        cache = cache == null ? Cache.defaults() : cache;
    }

    public record Retry(
        Duration period,
        Duration maxPeriod,
        int maxAttempts
    ) {

        public Retry {
            period = period == null ? Duration.ofMillis(100) : period;
            maxPeriod = maxPeriod == null ? Duration.ofMillis(300) : maxPeriod;
            maxAttempts = maxAttempts <= 0 ? 3 : maxAttempts;
        }

        public static Retry defaults() {
            return new Retry(Duration.ofMillis(100), Duration.ofMillis(300), 3);
        }
    }

    public record Cache(
        Duration ttl,
        long maximumSize
    ) {

        public Cache {
            ttl = ttl == null ? Duration.ofSeconds(30) : ttl;
            maximumSize = maximumSize <= 0 ? 10_000 : maximumSize;
        }

        public static Cache defaults() {
            return new Cache(Duration.ofSeconds(30), 10_000);
        }
    }
}
