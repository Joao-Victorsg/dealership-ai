package br.com.dealership.car.api.dto.response;

import java.util.List;

public record CarFilterOptionsResponse(
        List<String> manufacturers,
        List<String> exteriorColors
) {
}
