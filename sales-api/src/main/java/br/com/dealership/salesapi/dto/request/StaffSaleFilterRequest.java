package br.com.dealership.salesapi.dto.request;

import java.time.Instant;
import java.util.UUID;

public record StaffSaleFilterRequest(UUID clientId, UUID carId, Instant from, Instant to) {
}
