package br.com.cooperativevoting.shared.error;

import br.com.cooperativevoting.shared.web.ApiError;
import br.com.cooperativevoting.shared.web.ApiErrorDetail;
import br.com.cooperativevoting.shared.web.ApiResponse;
import br.com.cooperativevoting.voting.application.exception.AssociateEligibilityUnavailableException;
import br.com.cooperativevoting.voting.domain.exception.AgendaNotFoundException;
import br.com.cooperativevoting.voting.domain.exception.AssociateUnableToVoteException;
import br.com.cooperativevoting.voting.domain.exception.DomainException;
import br.com.cooperativevoting.voting.domain.exception.DuplicateVoteException;
import br.com.cooperativevoting.voting.domain.exception.InvalidDomainObjectException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionAlreadyOpenedException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionClosedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<ApiErrorDetail> details = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fieldError -> new ApiErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
            .toList();

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Existem campos inválidos na requisição.",
            "VALIDATION_ERROR",
            details,
            request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        List<ApiErrorDetail> details = exception.getConstraintViolations()
            .stream()
            .map(violation -> new ApiErrorDetail(violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Existem parâmetros inválidos na requisição.",
            "VALIDATION_ERROR",
            details,
            request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "O corpo da requisição está inválido ou mal formatado.",
            "MALFORMED_JSON",
            List.of(new ApiErrorDetail("body", "Envie um JSON válido.")),
            request
        );
    }

    @ExceptionHandler(AgendaNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAgendaNotFound(
        AgendaNotFoundException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, "Pauta não encontrada.", exception.getCode(), request);
    }

    @ExceptionHandler(AssociateUnableToVoteException.class)
    public ResponseEntity<ApiResponse<Void>> handleAssociateUnableToVote(
        AssociateUnableToVoteException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, "Este associado não está apto a votar.", exception.getCode(), request);
    }

    @ExceptionHandler({
        DuplicateVoteException.class,
        VotingSessionAlreadyOpenedException.class,
        VotingSessionClosedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleConflict(
        DomainException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, friendlyMessageFor(exception), exception.getCode(), request);
    }

    @ExceptionHandler(InvalidDomainObjectException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDomainObject(
        InvalidDomainObjectException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Dados inválidos para a operação.",
            exception.getCode(),
            List.of(new ApiErrorDetail(null, friendlyInvalidDomainDetail(exception.getMessage()))),
            request
        );
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(
        DomainException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, friendlyMessageFor(exception), exception.getCode(), request);
    }

    @ExceptionHandler(AssociateEligibilityUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleAssociateEligibilityUnavailable(
        AssociateEligibilityUnavailableException exception,
        HttpServletRequest request
    ) {
        LOGGER.warn("Associate eligibility service unavailable: {}", exception.getMessage());
        return buildResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Não foi possível consultar se o associado está apto a votar. Tente novamente em instantes.",
            "ASSOCIATE_ELIGIBILITY_UNAVAILABLE",
            request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
        Exception exception,
        HttpServletRequest request
    ) {
        LOGGER.error("Unexpected API error", exception);
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Não foi possível processar a solicitação. Tente novamente mais tarde.",
            "INTERNAL_ERROR",
            request
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(
        HttpStatus status,
        String message,
        String code,
        HttpServletRequest request
    ) {
        return buildResponse(status, message, code, List.of(), request);
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(
        HttpStatus status,
        String message,
        String code,
        List<ApiErrorDetail> details,
        HttpServletRequest request
    ) {
        ApiError error = ApiError.of(code, details);
        ApiResponse<Void> response = ApiResponse.failure(message, error, request.getRequestURI());
        return ResponseEntity.status(status).body(response);
    }

    private String friendlyMessageFor(DomainException exception) {
        return switch (exception.getCode()) {
            case "DUPLICATE_VOTE" -> "Este associado já votou nesta pauta.";
            case "VOTING_SESSION_ALREADY_OPENED" -> "Esta pauta já possui uma sessão de votação.";
            case "VOTING_SESSION_CLOSED" -> "A sessão de votação não está aberta para esta pauta.";
            case "ASSOCIATE_UNABLE_TO_VOTE" -> "Este associado não está apto a votar.";
            case "AGENDA_NOT_FOUND" -> "Pauta não encontrada.";
            default -> "Não foi possível concluir a operação solicitada.";
        };
    }

    private String friendlyInvalidDomainDetail(String message) {
        if (message == null) {
            return "Verifique os dados informados e tente novamente.";
        }

        if (message.contains("cpf")) {
            return "CPF informado é inválido.";
        }

        if (message.contains("agendaId")) {
            return "Identificador da pauta inválido.";
        }

        if (message.contains("votingSessionDuration")) {
            return "Duração da sessão inválida.";
        }

        if (message.contains("voteChoice")) {
            return "Voto inválido.";
        }

        return "Verifique os dados informados e tente novamente.";
    }
}
