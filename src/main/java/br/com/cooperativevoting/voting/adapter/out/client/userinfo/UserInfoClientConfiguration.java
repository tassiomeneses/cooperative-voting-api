package br.com.cooperativevoting.voting.adapter.out.client.userinfo;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching(proxyTargetClass = true)
@EnableFeignClients(clients = UserInfoFeignClient.class)
@EnableConfigurationProperties(UserInfoClientProperties.class)
public class UserInfoClientConfiguration {

    public static final String ASSOCIATE_ELIGIBILITY_CACHE = "associateEligibility";
    public static final String USER_INFO_CIRCUIT_BREAKER_NAME = "userInfo";

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager cacheManager(UserInfoClientProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(ASSOCIATE_ELIGIBILITY_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .recordStats()
            .expireAfterWrite(properties.cache().ttl())
            .maximumSize(properties.cache().maximumSize()));
        return cacheManager;
    }

    @Bean
    public CircuitBreakerNameResolver userInfoCircuitBreakerNameResolver() {
        return (feignClientName, target, method) -> USER_INFO_CIRCUIT_BREAKER_NAME;
    }

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> userInfoCircuitBreakerCustomizer(
        @Value("${resilience4j.timelimiter.instances.userInfo.timeoutDuration:3s}") Duration timeoutDuration
    ) {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(timeoutDuration)
            .cancelRunningFuture(true)
            .build();

        return factory -> factory.configure(
            builder -> builder.timeLimiterConfig(timeLimiterConfig),
            USER_INFO_CIRCUIT_BREAKER_NAME
        );
    }
}
