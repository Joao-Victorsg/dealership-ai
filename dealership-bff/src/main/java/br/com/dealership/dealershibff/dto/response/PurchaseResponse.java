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
        final var vehicle = source.vehicle();
        final var carSnapshot = source.carSnapshot();
        final var clientSnapshot = source.clientSnapshot() != null ? source.clientSnapshot() : source.client();
        final UUID vehicleId = source.carId() != null
                ? source.carId()
                : vehicle != null ? vehicle.id() : null;
        final String status = source.status() != null && !source.status().isBlank()
                ? source.status()
                : "COMPLETED";

        return PurchaseResponse.builder()
                .id(source.id())
                .registeredAt(source.registeredAt())
                .status(status)
                .vehicle(VehicleSnapshot.builder()
                        .id(vehicleId)
                        .model(carSnapshot != null ? carSnapshot.model() : vehicle != null ? vehicle.model() : null)
                        .manufacturer(carSnapshot != null ? carSnapshot.manufacturer() : vehicle != null ? vehicle.manufacturer() : null)
                        .manufacturingYear(carSnapshot != null
                                ? carSnapshot.manufacturingYear()
                                : vehicle != null ? vehicle.manufacturingYear() : null)
                        .externalColor(carSnapshot != null ? carSnapshot.externalColor() : vehicle != null ? vehicle.externalColor() : null)
                        .vin(carSnapshot != null ? carSnapshot.vin() : vehicle != null ? vehicle.vin() : null)
                        .category(carSnapshot != null ? carSnapshot.category() : vehicle != null ? vehicle.category() : null)
                        .listedValue(carSnapshot != null ? carSnapshot.listedValue() : vehicle != null ? vehicle.listedValue() : null)
                        .build())
                .client(ClientSnapshot.of(
                        clientSnapshot != null ? clientSnapshot.firstName() : null,
                        clientSnapshot != null ? clientSnapshot.lastName() : null,
                        clientSnapshot != null ? clientSnapshot.cpf() : null))
                .build();
    }
}
