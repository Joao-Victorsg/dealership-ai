package br.com.dealership.salesapi.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            MDC.put("http.method", request.getMethod());
            MDC.put("http.path", request.getRequestURI());
            String query = request.getQueryString();
            if (query != null) {
                MDC.put("http.query", query);
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.put("http.status", String.valueOf(response.getStatus()));
            MDC.put("http.latency_ms", String.valueOf(System.currentTimeMillis() - start));
            MDC.clear();
        }
    }
}
