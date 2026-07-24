package br.com.cooperativevoting.voting.adapter.out.client.userinfo;

import br.com.cooperativevoting.voting.application.port.out.AssociateEligibilityPort;
import br.com.cooperativevoting.voting.domain.model.enums.AssociateVotingStatus;
import br.com.cooperativevoting.voting.domain.model.vo.Cpf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class UserInfoAssociateEligibilityAdapter implements AssociateEligibilityPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInfoAssociateEligibilityAdapter.class);

    private final UserInfoFeignClient userInfoFeignClient;
    private final UserInfoClientMapper mapper;

    public UserInfoAssociateEligibilityAdapter(
        UserInfoFeignClient userInfoFeignClient,
        UserInfoClientMapper mapper
    ) {
        this.userInfoFeignClient = userInfoFeignClient;
        this.mapper = mapper;
    }

    @Override
    @Cacheable(cacheNames = UserInfoClientConfiguration.ASSOCIATE_ELIGIBILITY_CACHE, key = "@cpfHashingService.hash(#cpf)")
    public AssociateVotingStatus check(Cpf cpf) {
        LOGGER.info("Checking associate eligibility on user-info service for cpf={}", cpf.masked());

        UserInfoClientResponse response = userInfoFeignClient.findByCpf(cpf.value());
        AssociateVotingStatus status = mapper.toDomain(response);

        LOGGER.info("Associate eligibility checked for cpf={}, status={}", cpf.masked(), status);
        return status;
    }
}
