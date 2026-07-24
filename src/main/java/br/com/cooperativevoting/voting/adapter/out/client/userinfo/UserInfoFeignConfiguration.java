package br.com.cooperativevoting.voting.adapter.out.client.userinfo;

import feign.Logger;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class UserInfoFeignConfiguration {

    @Bean
    public Logger.Level userInfoFeignLoggerLevel() {
        return Logger.Level.NONE;
    }

    @Bean
    public Retryer userInfoRetryer(UserInfoClientProperties properties) {
        UserInfoClientProperties.Retry retry = properties.retry();
        return new Retryer.Default(
            retry.period().toMillis(),
            retry.maxPeriod().toMillis(),
            retry.maxAttempts()
        );
    }

    @Bean
    public ErrorDecoder userInfoErrorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            if (response.status() == 429 || response.status() >= 500) {
                return new RetryableException(
                    response.status(),
                    "Retryable response from user-info: HTTP " + response.status(),
                    response.request().httpMethod(),
                    null,
                    (Long) null,
                    response.request()
                );
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }
}
