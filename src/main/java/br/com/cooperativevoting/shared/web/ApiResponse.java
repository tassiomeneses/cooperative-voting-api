package br.com.cooperativevoting.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Envelope padronizado para todas as respostas da API.")
public record ApiResponse<T>(
    @Schema(example = "true")
    boolean success,

    @Schema(example = "Operação realizada com sucesso.")
    String message,

    @Schema(description = "Dados retornados pela operação.")
    T data,

    @Schema(description = "Informações de erro. Presente somente quando success=false.")
    ApiError error,

    @Schema(example = "2026-07-24T12:00:00Z")
    Instant timestamp,

    @Schema(example = "/pautas")
    String path
) {

    public static <T> ApiResponse<T> success(String message, T data, String path) {
        return new ApiResponse<>(true, message, data, null, Instant.now(), path);
    }

    public static <T> ApiResponse<T> failure(String message, ApiError error, String path) {
        return new ApiResponse<>(false, message, null, error, Instant.now(), path);
    }
}
