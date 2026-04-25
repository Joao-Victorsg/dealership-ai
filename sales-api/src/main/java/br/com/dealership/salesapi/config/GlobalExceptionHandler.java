package br.com.dealership.salesapi.config;

import br.com.dealership.salesapi.domain.exception.CarAlreadySoldException;
import br.com.dealership.salesapi.domain.exception.CarNotAvailableException;
import br.com.dealership.salesapi.domain.exception.SaleNotFoundException;
import br.com.dealership.salesapi.domain.exception.SaleOwnershipException;
import br.com.dealership.salesapi.domain.exception.SalesApiException;
import br.com.dealership.salesapi.domain.exception.SnsPublishException;
import br.com.dealership.salesapi.dto.response.ErrorResponse;
import br.com.dealership.salesapi.dto.response.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SalesApiException.class)
    public ResponseEntity<ErrorResponse> handleSalesApiException(SalesApiException ex) {
        HttpStatus status = switch (ex) {
            case CarNotAvailableException _ -> HttpStatus.UNPROCESSABLE_CONTENT;
            case SaleOwnershipException _ -> HttpStatus.FORBIDDEN;
            case CarAlreadySoldException _ -> HttpStatus.UNPROCESSABLE_CONTENT;
            case SaleNotFoundException _ -> HttpStatus.NOT_FOUND;
            case SnsPublishException _ -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> throw new AssertionError("Unexpected exception type: " + ex.getClass());
        };
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder().message(ex.getMessage()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> FieldError.of(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder().errors(fieldErrors).build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder().message(ex.getMessage()).build());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder().message("Access denied").build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder().message("Internal server error").build());
    }
}
