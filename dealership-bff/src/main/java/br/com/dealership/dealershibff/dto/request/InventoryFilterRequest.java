package br.com.dealership.dealershibff.dto.request;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

public record InventoryFilterRequest(
        String q,
        String category,
        String type,
        String condition,
        String manufacturer,
        Integer yearMin,
        Integer yearMax,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String color,
        BigDecimal kmMin,
        BigDecimal kmMax,
        String sortBy,
        String sortDirection,
        Integer page,
        Integer size
) {

    public InventoryFilterRequest {
        if (priceMin != null && priceMax != null && priceMin.compareTo(priceMax) > 0) {
            throw new IllegalArgumentException("priceMin must be less than or equal to priceMax");
        }
        if (yearMin != null && yearMax != null && yearMin > yearMax) {
            throw new IllegalArgumentException("yearMin must be less than or equal to yearMax");
        }
    }

    public String toCacheKey() {
        final var sorted = new TreeMap<String, String>();
        addIfPresent(sorted, "q", q);
        addIfPresent(sorted, "category", category);
        addIfPresent(sorted, "type", type);
        addIfPresent(sorted, "condition", condition);
        addIfPresent(sorted, "manufacturer", manufacturer);
        addIfPresent(sorted, "yearMin", yearMin);
        addIfPresent(sorted, "yearMax", yearMax);
        addIfPresent(sorted, "priceMin", priceMin);
        addIfPresent(sorted, "priceMax", priceMax);
        addIfPresent(sorted, "color", color);
        addIfPresent(sorted, "kmMin", kmMin);
        addIfPresent(sorted, "kmMax", kmMax);
        addIfPresent(sorted, "sortBy", sortBy);
        addIfPresent(sorted, "sortDirection", sortDirection);
        addIfPresent(sorted, "page", page);
        addIfPresent(sorted, "size", size);

        final var sb = new StringBuilder();
        sorted.forEach((k, v) -> {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private static void addIfPresent(final TreeMap<String, String> map, final String key, final Object value) {
        if (value != null) {
            map.put(key, value.toString());
        }
    }
}
