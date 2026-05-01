package br.com.dealership.dealershibff.web;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;
import br.com.dealership.dealershibff.dto.response.ApiErrorResponse;
import br.com.dealership.dealershibff.dto.response.ErrorBody;
import br.com.dealership.dealershibff.dto.response.ErrorDetail;
import br.com.dealership.dealershibff.dto.response.ResponseMeta;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(2)
public class InputSanitizationFilter extends OncePerRequestFilter {

    private static final String SAFE_QUERY_PATTERN = "[^a-zA-Z0-9\\s.,'\"-]";
    private static final String CPF_DIGITS_ONLY = "[^0-9]";
    private static final String CEP_DIGITS_ONLY = "[^0-9]";
    private static final String PHONE_DIGITS_ONLY = "[^0-9]";
    private static final String HTML_BLOCK_PATTERN = "(?si)<(\\w+)[^>]*>.*?</\\1>";

    private final ObjectMapper objectMapper;

    public InputSanitizationFilter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final List<ErrorDetail> violations = new ArrayList<>();
        final Map<String, String[]> sanitizedParams = new HashMap<>(request.getParameterMap());

        // Sanitize search query param 'q'
        final String rawQ = request.getParameter("q");
        if (rawQ != null) {
            // Strip complete HTML element blocks (e.g., <script>...</script>)
            final String strippedOfHtmlBlocks = rawQ.replaceAll(HTML_BLOCK_PATTERN, "").trim();
            if (strippedOfHtmlBlocks.isEmpty() && rawQ.contains("<")) {
                violations.add(ErrorDetail.of("q", "Search query contains invalid characters"));
            } else {
                // Apply safe-char sanitization to the stripped version
                final String sanitizedQ = strippedOfHtmlBlocks
                        .replaceAll(SAFE_QUERY_PATTERN, "")
                        .replaceAll("\\s+", " ")
                        .trim();
                sanitizedParams.put("q", new String[]{sanitizedQ});
                if (!rawQ.equals(sanitizedQ)) {
                    final String requestId = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
                    logger.info("Search query sanitized [requestId=" + requestId + "] sanitized='" + sanitizedQ + "'");
                }
            }
        }

        // Validate and normalize CPF (only from query params — body validation done by Bean Validation)
        final String cpfParam = request.getParameter("cpf");
        if (cpfParam != null) {
            final String digits = cpfParam.replaceAll(CPF_DIGITS_ONLY, "");
            if (!isValidCpf(digits)) {
                violations.add(ErrorDetail.of("cpf", "CPF must be 11 digits with valid check digits"));
            } else {
                sanitizedParams.put("cpf", new String[]{digits});
            }
        }

        // Validate and normalize CEP
        final String cepParam = request.getParameter("cep");
        if (cepParam != null) {
            final String digits = cepParam.replaceAll(CEP_DIGITS_ONLY, "");
            if (digits.length() != 8) {
                violations.add(ErrorDetail.of("cep", "CEP must be exactly 8 digits"));
            } else {
                sanitizedParams.put("cep", new String[]{digits});
            }
        }

        // Validate and normalize phone
        final String phoneParam = request.getParameter("phone");
        if (phoneParam != null) {
            final String digits = phoneParam.replaceAll(PHONE_DIGITS_ONLY, "");
            if (digits.length() < 10 || digits.length() > 11) {
                violations.add(ErrorDetail.of("phone", "Phone must be 10 or 11 digits (Brazilian format)"));
            } else {
                sanitizedParams.put("phone", new String[]{digits});
            }
        }

        if (!violations.isEmpty()) {
            writeValidationError(response, violations);
            return;
        }

        filterChain.doFilter(new SanitizedRequestWrapper(request, sanitizedParams), response);
    }

    private void writeValidationError(final HttpServletResponse response, final List<ErrorDetail> violations)
            throws IOException {
        final String requestId = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
        final var body = ErrorBody.of(ErrorCode.VALIDATION_ERROR, "Request validation failed",
                Collections.unmodifiableList(violations));
        final var errorResponse = ApiErrorResponse.of(body, ResponseMeta.of(requestId));
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    public static boolean isValidCpf(final String digits) {
        if (digits == null || digits.length() != 11) {
            return false;
        }
        if (digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkCpfDigit(digits, 9) && checkCpfDigit(digits, 10);
    }

    private static boolean checkCpfDigit(final String digits, final int position) {
        int sum = 0;
        for (int i = 0; i < position; i++) {
            sum += (digits.charAt(i) - '0') * (position + 1 - i);
        }
        final int remainder = (sum * 10) % 11;
        final int expected = remainder == 10 ? 0 : remainder;
        return expected == (digits.charAt(position) - '0');
    }

    private static final class SanitizedRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String[]> params;

        SanitizedRequestWrapper(final HttpServletRequest request, final Map<String, String[]> params) {
            super(request);
            this.params = Collections.unmodifiableMap(params);
        }

        @Override
        public String getParameter(final String name) {
            final String[] values = params.get(name);
            return values != null && values.length > 0 ? values[0] : null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return params;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(params.keySet());
        }

        @Override
        public String[] getParameterValues(final String name) {
            return params.get(name);
        }
    }
}
