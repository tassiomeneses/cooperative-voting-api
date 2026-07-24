package br.com.cooperativevoting.voting.adapter.out.persistence.repository;

import br.com.cooperativevoting.voting.adapter.out.persistence.entity.AgendaJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataAgendaRepository extends JpaRepository<AgendaJpaEntity, UUID> {

    @EntityGraph(attributePaths = "votingSession")
    @Query("select agenda from AgendaJpaEntity agenda where agenda.id = :id")
    Optional<AgendaJpaEntity> findByIdWithVotingSession(@Param("id") UUID id);

    @Query(
        value = """
            SELECT
                COALESCE(SUM(totals.yes_votes), 0) AS yesVotes,
                COALESCE(SUM(totals.no_votes), 0) AS noVotes
            FROM agendas agenda
            LEFT JOIN agenda_vote_total_buckets totals ON totals.agenda_id = agenda.id
            WHERE agenda.id = :id
            GROUP BY agenda.id
            """,
        nativeQuery = true
    )
    Optional<AgendaVoteTallyProjection> findVoteTallyByAgendaId(@Param("id") UUID id);
}
