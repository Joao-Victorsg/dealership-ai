package br.com.dealership.salesapi.domain.entity;

import br.com.dealership.salesapi.dto.request.RegisterSaleRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "sales",
        uniqueConstraints = @UniqueConstraint(name = "uk_sales_car_id", columnNames = "car_id")
)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Builder
public class Sale {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "car_id", updatable = false, nullable = false)
    private UUID carId;

    @Column(name = "client_id", updatable = false, nullable = false)
    private UUID clientId;

    @Column(name = "sale_value", precision = 19, scale = 4, updatable = false, nullable = false)
    private BigDecimal saleValue;

    @Column(name = "registered_at", updatable = false, nullable = false)
    private Instant registeredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "client_snapshot", columnDefinition = "jsonb", updatable = false, nullable = false)
    private ClientSnapshot clientSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "car_snapshot", columnDefinition = "jsonb", updatable = false, nullable = false)
    private CarSnapshot carSnapshot;

    public UUID getId() { return id; }
    public UUID getCarId() { return carId; }
    public UUID getClientId() { return clientId; }
    public BigDecimal getSaleValue() { return saleValue; }
    public Instant getRegisteredAt() { return registeredAt; }
    public ClientSnapshot getClientSnapshot() { return clientSnapshot; }
    public CarSnapshot getCarSnapshot() { return carSnapshot; }

    public static Sale from(RegisterSaleRequest request, UUID clientId, BigDecimal saleValue) {
        return Sale.builder()
                .id(UUID.randomUUID())
                .carId(request.carId())
                .clientId(clientId)
                .saleValue(saleValue)
                .registeredAt(Instant.now())
                .clientSnapshot(ClientSnapshot.from(request.clientSnapshot()))
                .carSnapshot(CarSnapshot.from(request.carSnapshot()))
                .build();
    }
}
