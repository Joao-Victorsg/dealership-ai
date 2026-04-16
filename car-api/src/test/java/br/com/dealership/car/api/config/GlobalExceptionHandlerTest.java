package br.com.dealership.car.api.config;

import br.com.dealership.car.api.domain.exception.CarNotFoundException;
import br.com.dealership.car.api.domain.exception.DuplicateVinException;
import br.com.dealership.car.api.domain.exception.SoldCarModificationException;
import br.com.dealership.car.api.domain.entity.Car;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private MethodArgumentNotValidException methodArgEx;

    @Mock
    private BindingResult bindingResult;

    @Test
    void shouldHandleValidationExceptionWithFieldErrors() {
        var fieldError = new org.springframework.validation.FieldError("car", "model", "must not be blank");
        when(methodArgEx.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        var response = handler.handleValidation(methodArgEx);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation failed", response.getBody().data().message());
        assertEquals(1, response.getBody().data().fieldErrors().size());
        assertEquals("model", response.getBody().data().fieldErrors().getFirst().field());
    }

    @Test
    void shouldHandleValidationExceptionWithNoFieldErrors() {
        when(methodArgEx.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        var response = handler.handleValidation(methodArgEx);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(0, response.getBody().data().fieldErrors().size());
    }

    @Test
    void shouldHandleHttpMessageNotReadableWithIllegalArgumentCause() {
        var cause = new IllegalArgumentException("No enum constant for: INVALID_VALUE");
        var httpInputMessage = mock(org.springframework.http.HttpInputMessage.class);
        var ex = new HttpMessageNotReadableException("Cannot deserialize", cause, httpInputMessage);

        var response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No enum constant for: INVALID_VALUE", response.getBody().data().message());
    }

    @Test
    void shouldHandleHttpMessageNotReadableWithoutIllegalArgumentCause() {
        var httpInputMessage = mock(org.springframework.http.HttpInputMessage.class);
        var ex = new HttpMessageNotReadableException("Cannot read body", httpInputMessage);

        var response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request body", response.getBody().data().message());
    }

    @Test
    void shouldHandleCarNotFoundException() {
        var id = UUID.randomUUID();
        var ex = new CarNotFoundException(id);

        var response = handler.handleCarNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Car not found with id: " + id, response.getBody().data().message());
    }

    @Test
    void shouldHandleDuplicateVinException() {
        var ex = new DuplicateVinException("1HGBH41JXMN109186");

        var response = handler.handleDuplicateVin(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("A car with this VIN already exists", response.getBody().data().message());
    }

    @Test
    void shouldHandleSoldCarModificationException() {
        var ex = new SoldCarModificationException();

        var response = handler.handleSoldCarModification(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertEquals("Cannot modify a sold car", response.getBody().data().message());
    }

    @Test
    void shouldHandleObjectOptimisticLockingFailureException() {
        var ex = new ObjectOptimisticLockingFailureException(Car.class, UUID.randomUUID());

        var response = handler.handleOptimisticLock(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(
                "The resource was modified by another request. Please retry with the latest version.",
                response.getBody().data().message()
        );
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        var ex = new IllegalArgumentException("Manufacturing year must be between 1886 and 2027");

        var response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Manufacturing year must be between 1886 and 2027", response.getBody().data().message());
    }

    @Test
    void shouldHandleGenericException() {
        var ex = new RuntimeException("Unexpected error");

        var response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().data().message());
    }

    @Test
    void shouldResolveNestedRootCauseForHttpMessageNotReadable() {
        var deepCause = new IllegalArgumentException("Deep root cause");
        var middleCause = new RuntimeException("Wrapper", deepCause);
        var httpInputMessage = mock(org.springframework.http.HttpInputMessage.class);
        var ex = new HttpMessageNotReadableException("Cannot deserialize", middleCause, httpInputMessage);

        var response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Deep root cause", response.getBody().data().message());
    }

    @Test
    void shouldExposeVinFromDuplicateVinException() {
        var vin = "1HGBH41JXMN109186";
        var ex = new DuplicateVinException(vin);
        assertEquals(vin, ex.getVin());
    }
}


