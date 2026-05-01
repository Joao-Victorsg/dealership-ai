package br.com.dealership.dealershibff.config;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;
import br.com.dealership.dealershibff.domain.exception.BffException;
import br.com.dealership.dealershibff.dto.response.ApiErrorResponse;
import br.com.dealership.dealershibff.dto.response.ErrorBody;
import br.com.dealership.dealershibff.dto.response.ErrorDetail;
import br.com.dealership.dealershibff.dto.response.ResponseMeta;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BffException.class)
    public ResponseEntity<ApiErrorResponse> handleBffException(final BffException ex) {
        final String requestId = getRequestId();
        log.warn("BFF exception [requestId={}] code={} message={}", requestId, ex.getErrorCode(), ex.getMessage());
        final var body = ErrorBody.of(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiErrorResponse.of(body, ResponseMeta.of(requestId)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(final MethodArgumentNotValidException ex) {
        final String requestId = getRequestId();
        final List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ErrorDetail.of(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("Validation error [requestId={}] fields={}", requestId, details.stream()
                .map(ErrorDetail::field).toList());
        final var body = ErrorBody.of(ErrorCode.VALIDATION_ERROR, "Request validation failed", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(body, ResponseMeta.of(requestId)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(final ConstraintViolationException ex) {
        final String requestId = getRequestId();
        final List<ErrorDetail> details = ex.getConstraintViolations().stream()
                .map(cv -> ErrorDetail.of(extractFieldName(cv), cv.getMessage()))
                .toList();
        log.warn("Constraint violation [requestId={}]", requestId);
        final var body = ErrorBody.of(ErrorCode.VALIDATION_ERROR, "Request validation failed", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(body, ResponseMeta.of(requestId)));
    }

    @ExceptionHandler(BeanInstantiationException.class)
    public ResponseEntity<ApiErrorResponse> handleBeanInstantiation(final BeanInstantiationException ex) {
        final Throwable cause = ex.getCause();
        if (cause instanceof IllegalArgumentException iae) {
            return handleIllegalArgument(iae);
        }
        return handleUnexpected(ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(final AccessDeniedException ex) {
        final String requestId = getRequestId();
        log.warn("Access denied [requestId={}]", requestId);
        final var body = ErrorBody.of(ErrorCode.FORBIDDEN, "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(body, ResponseMeta.of(requestId)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
        final String requestId = getRequestId();
        log.warn("Illegal argument [requestId={}] message={}", requestId, ex.getMessage());
        final var body = ErrorBody.of(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(body, ResponseMeta.of(requestId)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(final NoResourceFoundException ex) {
        final String requestId = getRequestId();
        final var body = ErrorBody.of(ErrorCode.NOT_FOUND, "The requested resource was not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(body, ResponseMeta.of(requestId)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(final HttpMessageNotReadableException ex) {
        final String requestId = getRequestId();
        log.warn("Unreadable HTTP message [requestId={}]", requestId);
        final var body = ErrorBody.of(ErrorCode.VALIDATION_ERROR, "Request body is malformed or missing");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(body, ResponseMeta.of(requestId)));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(final Throwable ex) {
        final String requestId = getRequestId();
        log.error("Unexpected error [requestId={}]", requestId, ex);
        final var body = ErrorBody.of(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(body, ResponseMeta.of(requestId)));
    }

    private String getRequestId() {
        final String mdcValue = MDC.get("requestId");
        return mdcValue != null ? mdcValue : UUID.randomUUID().toString();
    }

    private String extractFieldName(final ConstraintViolation<?> cv) {
        final String path = cv.getPropertyPath().toString();
        final int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }
}
