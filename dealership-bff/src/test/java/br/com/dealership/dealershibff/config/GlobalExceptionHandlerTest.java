package br.com.dealership.dealershibff.config;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;
import br.com.dealership.dealershibff.domain.exception.BffException;
import br.com.dealership.dealershibff.domain.exception.CarNotAvailableException;
import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.domain.exception.NotFoundException;
import br.com.dealership.dealershibff.dto.response.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup()
                .setControllerAdvice(handler)
                .build();
        MDC.clear();
    }

    @Test
    void shouldReturnConflictWhenCarNotAvailable() {
        final var ex = new CarNotAvailableException("Car is not available");

        final var response = handler.handleBffException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.CAR_NOT_AVAILABLE, response.getBody().error().code());
        assertNotNull(response.getBody().meta().requestId());
    }

    @Test
    void shouldReturnServiceUnavailableWhenDownstreamFails() {
        final var ex = new DownstreamServiceException("Service unavailable");

        final var response = handler.handleBffException(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE, response.getBody().error().code());
    }

    @Test
    void shouldReturnNotFoundForNotFoundException() {
        final var ex = new NotFoundException("Resource not found");

        final var response = handler.handleBffException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorCode.NOT_FOUND, response.getBody().error().code());
    }

    @Test
    void shouldUseRequestIdFromMdcWhenAvailable() {
        final String expectedRequestId = "test-request-id-123";
        MDC.put("requestId", expectedRequestId);

        final var ex = new NotFoundException("Not found");
        final var response = handler.handleBffException(ex);

        assertEquals(expectedRequestId, response.getBody().meta().requestId());
    }

    @Test
    void shouldGenerateRequestIdWhenMdcIsEmpty() {
        MDC.clear();

        final var ex = new NotFoundException("Not found");
        final var response = handler.handleBffException(ex);

        assertNotNull(response.getBody().meta().requestId());
        assertFalse(response.getBody().meta().requestId().isBlank());
    }

    @Test
    void shouldReturnBadRequestForValidationException() throws Exception {
        final var bindingResult = org.mockito.Mockito.mock(BindingResult.class);
        final var fieldError = new org.springframework.validation.FieldError("obj", "name", "must not be blank");
        org.mockito.Mockito.when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));
        final var ex = new MethodArgumentNotValidException(null, bindingResult);

        final var response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_ERROR, response.getBody().error().code());
        assertEquals(1, response.getBody().error().details().size());
        assertEquals("name", response.getBody().error().details().getFirst().field());
        assertEquals("must not be blank", response.getBody().error().details().getFirst().reason());
    }

    @Test
    void shouldReturnBadRequestForMultipleValidationErrors() throws Exception {
        final var bindingResult = org.mockito.Mockito.mock(BindingResult.class);
        final var errors = java.util.List.of(
                new org.springframework.validation.FieldError("obj", "name", "must not be blank"),
                new org.springframework.validation.FieldError("obj", "email", "must be valid email")
        );
        org.mockito.Mockito.when(bindingResult.getFieldErrors()).thenReturn(errors);
        final var ex = new MethodArgumentNotValidException(null, bindingResult);

        final var response = handler.handleValidation(ex);

        assertEquals(2, response.getBody().error().details().size());
    }

    @Test
    void shouldReturnNotFoundForNoResourceFoundException() throws Exception {
        final var ex = new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/unknown", "/unknown");

        final var response = handler.handleNoResource(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorCode.NOT_FOUND, response.getBody().error().code());
    }

    @Test
    void shouldReturnBadRequestForUnreadableMessage() {
        final var ex = new HttpMessageNotReadableException("bad input",
                (org.springframework.http.HttpInputMessage) null);

        final var response = handler.handleUnreadableMessage(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_ERROR, response.getBody().error().code());
    }

    @Test
    void shouldReturnInternalServerErrorForUnhandledThrowable() {
        final var ex = new RuntimeException("unexpected");

        final var response = handler.handleUnexpected(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCode.INTERNAL_ERROR, response.getBody().error().code());
    }

    @Test
    void shouldNotExposeStackTraceInErrorResponse() {
        final var ex = new RuntimeException("unexpected");

        final var response = handler.handleUnexpected(ex);

        final var body = response.getBody();
        assertNotNull(body);
        assertFalse(body.error().message().contains("at "));
        assertTrue(body.error().details() == null || body.error().details().isEmpty());
    }

    @Test
    void shouldReturnBadRequestForConstraintViolation() {
        final var violation = org.mockito.Mockito.mock(jakarta.validation.ConstraintViolation.class);
        final var path = org.mockito.Mockito.mock(jakarta.validation.Path.class);
        org.mockito.Mockito.when(path.toString()).thenReturn("method.fieldName");
        org.mockito.Mockito.when(violation.getPropertyPath()).thenReturn(path);
        org.mockito.Mockito.when(violation.getMessage()).thenReturn("must not be null");
        final var ex = new jakarta.validation.ConstraintViolationException(java.util.Set.<jakarta.validation.ConstraintViolation<?>>of(violation));

        final var response = handler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_ERROR, response.getBody().error().code());
        assertEquals(1, response.getBody().error().details().size());
        assertEquals("fieldName", response.getBody().error().details().getFirst().field());
    }
}
