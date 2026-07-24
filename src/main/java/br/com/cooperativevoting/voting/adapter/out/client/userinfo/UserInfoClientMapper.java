package br.com.cooperativevoting.voting.adapter.out.client.userinfo;

import br.com.cooperativevoting.voting.application.exception.AssociateEligibilityUnavailableException;
import br.com.cooperativevoting.voting.domain.model.enums.AssociateVotingStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class UserInfoClientMapper {

    public AssociateVotingStatus toDomain(UserInfoClientResponse response) {
        if (response == null || response.status() == null || response.status().isBlank()) {
            throw new AssociateEligibilityUnavailableException(
                "Invalid empty response from user-info service",
                null
            );
        }

        try {
            return AssociateVotingStatus.valueOf(response.status().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AssociateEligibilityUnavailableException(
                "Invalid status from user-info service: " + response.status(),
                exception
            );
        }
    }
}
