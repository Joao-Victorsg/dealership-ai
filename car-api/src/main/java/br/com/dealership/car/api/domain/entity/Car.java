package br.com.dealership.car.api.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import br.com.dealership.car.api.domain.enums.CarCategory;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.enums.PropulsionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "car")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String model;

    @Column(name = "manufacturing_year", nullable = false)
    private Integer manufacturingYear;

    @Column(nullable = false, length = 255)
    private String manufacturer;

    @Column(name = "external_color", nullable = false, length = 100)
    private String externalColor;

    @Column(name = "internal_color", nullable = false, length = 100)
    private String internalColor;

    @Column(nullable = false, length = 17, unique = true)
    private String vin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarStatus status;

    @Getter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "optional_items", columnDefinition = "jsonb")
    private List<String> optionalItems;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal kilometers;

    @Column(name = "is_new", nullable = false)
    private Boolean isNew;

    @Enumerated(EnumType.STRING)
    @Column(name = "propulsion_type", nullable = false, length = 20)
    private PropulsionType propulsionType;

    @Column(name = "listed_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal listedValue;

    @Column(name = "image_key", length = 500)
    private String imageKey;

    @Column(name = "registration_date", nullable = false)
    private Instant registrationDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public List<String> getOptionalItems() {
        return optionalItems != null ? Collections.unmodifiableList(optionalItems) : List.of();
    }

    public void changeStatus(CarStatus status) {
        this.status = status;
    }

    public void changeListedValue(BigDecimal listedValue) {
        this.listedValue = listedValue;
    }

    public void changeImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        if (registrationDate == null) {
            registrationDate = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
