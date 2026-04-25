package br.com.dealership.salesapi.dto.response;

import br.com.dealership.salesapi.domain.entity.Sale;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record SaleResponse(
        UUID id,
        UUID carId,
        UUID clientId,
        BigDecimal saleValue,
        Instant registeredAt,
        ClientSnapshotResponse clientSnapshot,
        CarSnapshotResponse carSnapshot
) {
    public static SaleResponse from(Sale sale) {
        return SaleResponse.builder()
                .id(sale.getId())
                .carId(sale.getCarId())
                .clientId(sale.getClientId())
                .saleValue(sale.getSaleValue())
                .registeredAt(sale.getRegisteredAt())
                .clientSnapshot(ClientSnapshotResponse.from(sale.getClientSnapshot()))
                .carSnapshot(CarSnapshotResponse.from(sale.getCarSnapshot()))
                .build();
    }
}
