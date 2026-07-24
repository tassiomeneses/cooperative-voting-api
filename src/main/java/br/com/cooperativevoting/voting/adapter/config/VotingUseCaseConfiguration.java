package br.com.cooperativevoting.voting.adapter.config;

import br.com.cooperativevoting.voting.application.port.in.CreateAgendaUseCase;
import br.com.cooperativevoting.voting.application.port.in.FindVotingResultUseCase;
import br.com.cooperativevoting.voting.application.port.in.OpenVotingSessionUseCase;
import br.com.cooperativevoting.voting.application.port.in.RegisterVoteUseCase;
import br.com.cooperativevoting.voting.application.port.out.AgendaRepositoryPort;
import br.com.cooperativevoting.voting.application.port.out.AssociateEligibilityPort;
import br.com.cooperativevoting.voting.application.port.out.VoteRepositoryPort;
import br.com.cooperativevoting.voting.application.usecase.createagenda.mapper.CreateAgendaMapper;
import br.com.cooperativevoting.voting.application.usecase.createagenda.service.CreateAgendaService;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.mapper.FindVotingResultMapper;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.service.FindVotingResultService;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.mapper.OpenVotingSessionMapper;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.service.OpenVotingSessionService;
import br.com.cooperativevoting.voting.application.usecase.registervote.mapper.RegisterVoteMapper;
import br.com.cooperativevoting.voting.application.usecase.registervote.service.RegisterVoteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class VotingUseCaseConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CreateAgendaMapper createAgendaMapper() {
        return new CreateAgendaMapper();
    }

    @Bean
    public OpenVotingSessionMapper openVotingSessionMapper() {
        return new OpenVotingSessionMapper();
    }

    @Bean
    public RegisterVoteMapper registerVoteMapper() {
        return new RegisterVoteMapper();
    }

    @Bean
    public FindVotingResultMapper findVotingResultMapper() {
        return new FindVotingResultMapper();
    }

    @Bean
    public CreateAgendaUseCase createAgendaUseCase(
        AgendaRepositoryPort agendaRepositoryPort,
        CreateAgendaMapper createAgendaMapper,
        Clock clock
    ) {
        return new CreateAgendaService(agendaRepositoryPort, createAgendaMapper, clock);
    }

    @Bean
    public OpenVotingSessionUseCase openVotingSessionUseCase(
        AgendaRepositoryPort agendaRepositoryPort,
        OpenVotingSessionMapper openVotingSessionMapper,
        Clock clock
    ) {
        return new OpenVotingSessionService(agendaRepositoryPort, openVotingSessionMapper, clock);
    }

    @Bean
    public RegisterVoteUseCase registerVoteUseCase(
        VoteRepositoryPort voteRepositoryPort,
        AssociateEligibilityPort associateEligibilityPort,
        RegisterVoteMapper registerVoteMapper,
        Clock clock
    ) {
        return new RegisterVoteService(
            voteRepositoryPort,
            associateEligibilityPort,
            registerVoteMapper,
            clock
        );
    }

    @Bean
    public FindVotingResultUseCase findVotingResultUseCase(
        AgendaRepositoryPort agendaRepositoryPort,
        FindVotingResultMapper findVotingResultMapper
    ) {
        return new FindVotingResultService(agendaRepositoryPort, findVotingResultMapper);
    }
}
