package br.com.dealership.clientapi.config;

import br.com.dealership.clientapi.dto.response.ErrorResponse;
import br.com.dealership.clientapi.dto.response.FieldError;
import br.com.dealership.clientapi.dto.response.Response;
import br.com.dealership.clientapi.exception.ClientNotFoundException;
import br.com.dealership.clientapi.exception.DuplicateCpfException;
import br.com.dealership.clientapi.exception.DuplicateKeycloakIdException;
import br.com.dealership.clientapi.exception.ProfileInactiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Response<ErrorResponse>> handleValidation(MethodArgumentNotValidException ex) {
        final var fieldErrors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof org.springframework.validation.FieldError fe) {
                        return FieldError.of(fe.getField(), fe.getDefaultMessage());
                    }
                    return FieldError.of(error.getObjectName(), error.getDefaultMessage());
                })
                .toList();

        final var response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(Response.of(response));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Response<ErrorResponse>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        final var rootCause = findRootCause(ex);
        final var message = rootCause instanceof IllegalArgumentException
                ? rootCause.getMessage()
                : "Malformed request body";

        final var response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .build();
        return ResponseEntity.badRequest().body(Response.of(response));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Response<ErrorResponse>> handleIllegalArgument(IllegalArgumentException ex) {
        final var response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.badRequest().body(Response.of(response));
    }

    @ExceptionHandler({DuplicateCpfException.class, DuplicateKeycloakIdException.class, ProfileInactiveException.class})
    ResponseEntity<Response<ErrorResponse>> handleBusinessRule(RuntimeException ex) {
        final var response = ErrorResponse.builder()
                .status(HttpStatus.UNPROCESSABLE_CONTENT.value())
                .error(HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.unprocessableContent().body(Response.of(response));
    }

    @ExceptionHandler(ClientNotFoundException.class)
    ResponseEntity<Response<ErrorResponse>> handleClientNotFound(ClientNotFoundException ex) {
        final var response = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("Access denied")
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Response.of(response));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Response<ErrorResponse>> handleAccessDenied(AccessDeniedException ex) {
        final var response = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("Access denied")
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Response.of(response));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<Response<ErrorResponse>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        final var response = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message("The resource was modified by another request. Please retry with the latest version.")
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Response.of(response));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Response<ErrorResponse>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        final var response = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.of(response));
    }

    private static Throwable findRootCause(Throwable throwable) {
        var cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
