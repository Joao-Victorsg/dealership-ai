package br.com.dealership.dealershibff.feign.sales;

import br.com.dealership.dealershibff.domain.exception.CarNotAvailableException;
import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SalesApiErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(final String methodKey, final Response response) {
        final String responseBody = readResponseBody(response);
        final String detail = responseBody.isBlank() ? "" : " body=" + responseBody;
        return switch (response.status()) {
            case 409 -> new CarNotAvailableException("Car is no longer available");
            case 400 -> new IllegalArgumentException("Sales API rejected purchase payload." + detail);
            case 422 -> isAlreadySoldError(responseBody)
                    ? new CarNotAvailableException("Car is no longer available")
                    : new IllegalArgumentException("Sales API rejected purchase payload." + detail);
            default -> new DownstreamServiceException("Sales API error: " + response.status() + detail);
        };
    }

    private boolean isAlreadySoldError(final String responseBody) {
        return responseBody != null && responseBody.toLowerCase().contains("already sold");
    }

    private String readResponseBody(final Response response) {
        if (response.body() == null) {
            return "";
        }
        try {
            return Util.toString(response.body().asReader(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            return "";
        }
    }
}
