package br.com.dealership.dealershibff.feign.sales.dto;

import java.util.UUID;

public record SalesApiRegisterRequest(
        UUID carId,
        SalesApiVehicleSnapshot vehicle,
        SalesApiClientSnapshot client,
        String saleIntent
) {

    public static SalesApiRegisterRequest of(
            final UUID carId,
            final SalesApiVehicleSnapshot vehicle,
            final SalesApiClientSnapshot client,
            final String saleIntent
    ) {
        return new SalesApiRegisterRequest(carId, vehicle, client, saleIntent);
    }
}
