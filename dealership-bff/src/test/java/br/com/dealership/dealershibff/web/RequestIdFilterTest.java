package br.com.dealership.dealershibff.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class RequestIdFilterTest {

    private RequestIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestIdFilter();
        MDC.clear();
    }

    @Test
    void shouldGenerateRequestIdWhenHeaderAbsent() throws Exception {
        final var request = new MockHttpServletRequest();
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
        assertFalse(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER).isBlank());
    }

    @Test
    void shouldUseProvidedRequestIdWhenHeaderPresent() throws Exception {
        final var requestId = "provided-request-id";
        final var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, requestId);
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(requestId, response.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
    }

    @Test
    void shouldClearMdcAfterRequestCompletes() throws Exception {
        final var request = new MockHttpServletRequest();
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    }

    @Test
    void shouldSetRequestIdAsAttribute() throws Exception {
        final var requestId = "attr-test-id";
        final var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, requestId);
        final var response = new MockHttpServletResponse();

        final var chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                assertEquals(requestId, req.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
            }
        };

        filter.doFilter(request, response, chain);
    }

    @Test
    void shouldIgnoreBlankRequestIdHeader() throws Exception {
        final var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "   ");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        final var responseId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertNotNull(responseId);
        assertFalse(responseId.isBlank());
    }
}
