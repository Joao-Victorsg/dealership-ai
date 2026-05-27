package br.com.dealership.dealershibff.feign.car.dto;

import java.util.List;

public record CarApiFilterOptionsResponse(
        List<String> manufacturers,
        List<String> exteriorColors
) {
}
