package br.com.dealership.dealershibff.feign.sales.dto;

import java.util.UUID;

public record SalesApiRegisterRequest(
        UUID carId,
        UUID clientId,
        SalesApiClientSnapshot clientSnapshot,
        SalesApiCarSnapshot carSnapshot
) {

    public static SalesApiRegisterRequest of(
            final UUID carId,
            final UUID clientId,
            final SalesApiClientSnapshot clientSnapshot,
            final SalesApiCarSnapshot carSnapshot
    ) {
        return new SalesApiRegisterRequest(carId, clientId, clientSnapshot, carSnapshot);
    }
}
