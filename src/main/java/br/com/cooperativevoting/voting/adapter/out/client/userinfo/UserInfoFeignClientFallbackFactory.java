package br.com.cooperativevoting.voting.adapter.out.client.userinfo;

import br.com.cooperativevoting.voting.application.exception.AssociateEligibilityUnavailableException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class UserInfoFeignClientFallbackFactory implements FallbackFactory<UserInfoFeignClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInfoFeignClientFallbackFactory.class);

    @Override
    public UserInfoFeignClient create(Throwable cause) {
        return cpf -> {
            String maskedCpf = mask(cpf);

            if (hasCause(cause, FeignException.NotFound.class)) {
                LOGGER.warn("User-info returned 404 for cpf={}. Treating associate as unable to vote.", maskedCpf);
                return UserInfoClientResponse.unableToVote();
            }

            LOGGER.error("User-info fallback triggered for cpf={}. Blocking vote until eligibility can be checked.", maskedCpf, cause);
            throw new AssociateEligibilityUnavailableException(
                "User-info service is unavailable",
                cause
            );
        };
    }

    private String mask(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return "***";
        }
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> expectedType) {
        Throwable current = throwable;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
