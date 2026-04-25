package br.com.dealership.salesapi.messaging;

import br.com.dealership.salesapi.domain.entity.CarSnapshot;
import br.com.dealership.salesapi.domain.entity.ClientSnapshot;
import br.com.dealership.salesapi.domain.entity.Sale;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record SaleEventPayload(
        UUID saleId,
        UUID carId,
        UUID clientId,
        BigDecimal saleValue,
        Instant registeredAt,
        ClientSnapshot clientSnapshot,
        CarSnapshot carSnapshot
) {
    public static SaleEventPayload from(Sale sale) {
        return SaleEventPayload.builder()
                .saleId(sale.getId())
                .carId(sale.getCarId())
                .clientId(sale.getClientId())
                .saleValue(sale.getSaleValue())
                .registeredAt(sale.getRegisteredAt())
                .clientSnapshot(sale.getClientSnapshot())
                .carSnapshot(sale.getCarSnapshot())
                .build();
    }
}
