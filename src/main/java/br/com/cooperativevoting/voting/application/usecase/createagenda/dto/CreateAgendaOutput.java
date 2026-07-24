package br.com.cooperativevoting.voting.application.usecase.createagenda.dto;

import java.time.Instant;

public record CreateAgendaOutput(
    String agendaId,
    String title,
    String description,
    Instant createdAt
) {
}
