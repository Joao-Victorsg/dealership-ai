package br.com.dealership.car.api.config;

import br.com.dealership.car.api.domain.exception.CarNotFoundException;
import br.com.dealership.car.api.domain.exception.DuplicateVinException;
import br.com.dealership.car.api.domain.exception.SoldCarModificationException;
import br.com.dealership.car.api.dto.response.ErrorResponse;
import br.com.dealership.car.api.dto.response.FieldError;
import br.com.dealership.car.api.dto.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Response<ErrorResponse>> handleValidation(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> FieldError.of(fe.getField(), fe.getDefaultMessage()))
                .toList();

        var response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(Response.of(response));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Response<ErrorResponse>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        var rootCause = findRootCause(ex);
        var message = rootCause instanceof IllegalArgumentException
                ? rootCause.getMessage()
                : "Malformed request body";

        var response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .build();
        return ResponseEntity.badRequest().body(Response.of(response));
    }

    @ExceptionHandler(CarNotFoundException.class)
    ResponseEntity<Response<ErrorResponse>> handleCarNotFound(CarNotFoundException ex) {
        var response = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.of(response));
    }

    @ExceptionHandler(DuplicateVinException.class)
    ResponseEntity<Response<ErrorResponse>> handleDuplicateVin(DuplicateVinException ex) {
        var response = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Response.of(response));
    }

    @ExceptionHandler(SoldCarModificationException.class)
    ResponseEntity<Response<ErrorResponse>> handleSoldCarModification(SoldCarModificationException ex) {
        var response = ErrorResponse.builder()
                .status(HttpStatus.UNPROCESSABLE_CONTENT.value())
                .error(HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(Response.of(response));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<Response<ErrorResponse>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        var response = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message("The resource was modified by another request. Please retry with the latest version.")
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Response.of(response));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Response<ErrorResponse>> handleIllegalArgument(IllegalArgumentException ex) {
        var response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.badRequest().body(Response.of(response));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Response<ErrorResponse>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        var response = ErrorResponse.builder()
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
