package br.com.dealership.dealershibff.dto.response;

import java.util.List;

public record InventoryFilterOptionsResponse(
        List<String> manufacturers,
        List<String> exteriorColors
) {
}
