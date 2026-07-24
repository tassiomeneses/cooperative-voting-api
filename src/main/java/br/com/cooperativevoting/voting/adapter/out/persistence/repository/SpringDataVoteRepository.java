package br.com.cooperativevoting.voting.adapter.out.persistence.repository;

import br.com.cooperativevoting.voting.adapter.out.persistence.entity.VoteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface SpringDataVoteRepository extends JpaRepository<VoteJpaEntity, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            INSERT INTO votes (id, agenda_id, associate_id, voter_key, choice, cast_at)
            SELECT :id, :agendaId, :associateId, :voterKey, :choice, :castAt
            FROM voting_sessions session
            WHERE session.agenda_id = :agendaId
              AND :castAt >= session.opened_at
              AND :castAt < session.closed_at
            """,
        nativeQuery = true
    )
    int insertIfSessionOpen(
        @Param("id") UUID id,
        @Param("agendaId") UUID agendaId,
        @Param("associateId") String associateId,
        @Param("voterKey") String voterKey,
        @Param("choice") String choice,
        @Param("castAt") Instant castAt
    );

    @Query(
        value = "SELECT EXISTS (SELECT 1 FROM agendas WHERE id = :agendaId)",
        nativeQuery = true
    )
    boolean existsAgendaById(@Param("agendaId") UUID agendaId);

}
