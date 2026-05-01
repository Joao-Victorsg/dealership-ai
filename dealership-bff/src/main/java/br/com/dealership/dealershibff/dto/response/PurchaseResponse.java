package br.com.dealership.dealershibff.dto.response;

import br.com.dealership.dealershibff.feign.sales.dto.SalesApiSaleResponse;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PurchaseResponse(
        UUID id,
        Instant registeredAt,
        String status,
        VehicleSnapshot vehicle,
        ClientSnapshot client
) {

    public static PurchaseResponse from(final SalesApiSaleResponse source) {
        final var v = source.vehicle();
        final var c = source.client();
        return PurchaseResponse.builder()
                .id(source.id())
                .registeredAt(source.registeredAt())
                .status(source.status())
                .vehicle(VehicleSnapshot.builder()
                        .id(v.id())
                        .model(v.model())
                        .manufacturer(v.manufacturer())
                        .manufacturingYear(v.manufacturingYear())
                        .externalColor(v.externalColor())
                        .vin(v.vin())
                        .category(v.category())
                        .listedValue(v.listedValue())
                        .build())
                .client(ClientSnapshot.of(c.firstName(), c.lastName(), c.cpf()))
                .build();
    }
}
