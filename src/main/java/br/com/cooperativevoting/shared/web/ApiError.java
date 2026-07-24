package br.com.cooperativevoting.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Detalhes padronizados do erro retornado pela API.")
public record ApiError(
    @Schema(example = "VALIDATION_ERROR")
    String code,

    @Schema(description = "Lista de detalhes do erro.")
    List<ApiErrorDetail> details
) {

    public static ApiError of(String code) {
        return new ApiError(code, List.of());
    }

    public static ApiError of(String code, List<ApiErrorDetail> details) {
        return new ApiError(code, details == null ? List.of() : List.copyOf(details));
    }
}
