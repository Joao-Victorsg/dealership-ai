package br.com.dealership.dealershibff.web;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class InputSanitizationFilterTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private InputSanitizationFilter filter;

    @BeforeEach
    void setUp() {
        org.slf4j.MDC.clear();
    }

    @Test
    void shouldPassRequestWithNoSpecialParams() throws Exception {
        final var request = new MockHttpServletRequest();
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldSanitizeQueryParamByRemovingScriptTags() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setParameter("q", "<script>alert(1)</script>car");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        final var wrappedRequest = (jakarta.servlet.ServletRequest) chain.getRequest();
        assertFalse(wrappedRequest.getParameter("q").contains("<"));
        assertFalse(wrappedRequest.getParameter("q").contains(">"));
    }

    @Test
    void shouldReturn400ForInvalidCpf() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setParameter("cpf", "00000000000");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertTrue(response.getContentAsString().contains("cpf"));
    }

    @Test
    void shouldReturn400ForCepWithWrongLength() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setParameter("cep", "1234567");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("cep"));
    }

    @Test
    void shouldNormalizeCepByRemovingDash() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setParameter("cep", "01310-100");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("01310100", chain.getRequest().getParameter("cep"));
    }

    @Test
    void shouldReturn400ForPhoneWithInvalidLength() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setParameter("phone", "12345");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("phone"));
    }

    @Test
    void shouldReturn400ForAllThreeInvalidParamsAtOnce() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setParameter("cpf", "00000000000");
        request.setParameter("cep", "123");
        request.setParameter("phone", "1234");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        final var body = response.getContentAsString();
        assertTrue(body.contains("cpf"));
        assertTrue(body.contains("cep"));
        assertTrue(body.contains("phone"));
    }

    @Test
    void shouldPassValidCpf() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setParameter("cpf", "529.982.247-25");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("52998224725", chain.getRequest().getParameter("cpf"));
    }

    @Test
    void shouldPassValid11DigitPhone() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setParameter("phone", "11999887766");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("11999887766", chain.getRequest().getParameter("phone"));
    }

    @Test
    void shouldValidateCpfWithKnownValidValue() {
        assertTrue(InputSanitizationFilter.isValidCpf("52998224725"));
    }

    @Test
    void shouldRejectCpfWithAllSameDigits() {
        assertFalse(InputSanitizationFilter.isValidCpf("11111111111"));
    }

    @Test
    void shouldRejectCpfWithWrongLength() {
        assertFalse(InputSanitizationFilter.isValidCpf("1234567890"));
    }

    @Test
    void shouldRejectNullCpf() {
        assertFalse(InputSanitizationFilter.isValidCpf(null));
    }

    @Test
    void shouldRejectCpfWithInvalidFirstCheckDigit() {
        // "12345678921": first check digit should be 0 (remainder==10), actual is 2 → FAIL
        assertFalse(InputSanitizationFilter.isValidCpf("12345678921"));
    }

    @Test
    void shouldRejectCpfWithInvalidSecondCheckDigitOnly() {
        // "12345678901": first check passes (remainder 10 → expected 0, actual 0),
        //               second check fails (expected 9, actual 1)
        assertFalse(InputSanitizationFilter.isValidCpf("12345678901"));
    }

    @Test
    void shouldPassSafeQueryParamWithNoSanitizationNeeded() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setParameter("q", "corolla");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("corolla", chain.getRequest().getParameter("q"));
    }

    @Test
    void shouldReturn400ForQueryContainingOnlyScriptTag() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setParameter("q", "<script>alert('xss')</script>");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("q"));
    }

    @Test
    void shouldReturn400ForPhoneWithMoreThan11Digits() throws Exception {
        final var request = new MockHttpServletRequest();
        request.setParameter("phone", "119998877665");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("phone"));
    }

    @Test
    void shouldReturnNullForUnknownParameterInSanitizedWrapper() throws Exception {
        final var request = new MockHttpServletRequest();
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest().getParameter("nonexistent_param"));
    }

    @Test
    void shouldSanitizeQueryParamWhenItIsOnlyWhitespace() throws Exception {
        // Whitespace-only q: strippedOfHtmlBlocks becomes "" (trimmed), but rawQ has no "<"
        // so it falls to the else branch → sanitizedQ = ""
        final var request = new MockHttpServletRequest();
        request.setParameter("q", "   ");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("", chain.getRequest().getParameter("q"));
    }
}
