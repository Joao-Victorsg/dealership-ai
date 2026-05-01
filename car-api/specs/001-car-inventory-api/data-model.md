# Data Model: Car Inventory API

**Feature**: 001-car-inventory-api  
**Date**: 2026-04-12  
**Status**: Complete

## Entities

### Car

The central entity representing a vehicle in the dealership inventory.

| Field | Type | Constraints | Mutable | Description |
|-------|------|-------------|---------|-------------|
| `id` | `UUID` | PK, auto-generated | No | Unique identifier |
| `model` | `String` | Not null, max 255 | No | Vehicle model name |
| `manufacturing_year` | `Integer` | Not null, >= 1886, <= current year + 1 | No | Year of manufacture |
| `manufacturer` | `String` | Not null, max 255 | No | Vehicle brand |
| `external_color` | `String` | Not null, max 100 | No | Exterior color |
| `internal_color` | `String` | Not null, max 100 | No | Interior color |
| `vin` | `String(17)` | Not null, unique, 17 alphanumeric, uppercase | No | Vehicle Identification Number |
| `status` | `CarStatus` | Not null, enum | Yes | Availability status |
| `optional_items` | `List<String>` | Nullable, JSONB | No | Accessories/extras |
| `category` | `CarCategory` | Not null, enum | No | Body type category |
| `kilometers` | `BigDecimal` | Not null, >= 0 | No | Odometer reading |
| `is_new` | `Boolean` | Not null | No | New or used indicator |
| `propulsion_type` | `PropulsionType` | Not null, enum | No | Engine/motor type |
| `listed_value` | `BigDecimal` | Not null, > 0, precision(12,2) | Yes | Listed sale price |
| `image_key` | `String` | Nullable, max 500 | Yes | S3 object key for image |
| `registration_date` | `LocalDateTime` | Not null, auto-assigned | No | Registration timestamp |
| `created_at` | `LocalDateTime` | Not null, auto-assigned | No | Creation timestamp |
| `updated_at` | `LocalDateTime` | Not null, auto-updated | No | Last-update timestamp |

## Enums

### CarStatus

| Value | Description |
|-------|-------------|
| `AVAILABLE` | Available for purchase |
| `SOLD` | Sold (terminal state — no further transitions) |
| `UNAVAILABLE` | Removed from active listing |

**State transitions**:
- `AVAILABLE` -> `UNAVAILABLE` | `SOLD`
- `UNAVAILABLE` -> `AVAILABLE` | `SOLD`
- `SOLD` -> *(none — terminal state)*

**Registration constraint**: Initial status MUST be `AVAILABLE` or `UNAVAILABLE`.

### CarCategory

`SUV` | `SEDAN` | `SPORT` | `HATCH` | `PICKUP`

### PropulsionType

`ELECTRIC` | `COMBUSTION`

## Validation Rules

### Registration (Create)

| ID | Field(s) | Rule | Error |
|----|----------|------|-------|
| VR-001 | `vin` | Exactly 17 alphanumeric chars after uppercase normalization | VIN must be exactly 17 alphanumeric characters |
| VR-002 | `vin` | Unique across all cars | A car with this VIN already exists |
| VR-003 | `is_new`, `km` | `is_new=true` requires `kilometers=0` | A new car must have zero kilometers |
| VR-004 | `is_new`, `km` | `is_new=false` requires `kilometers>0` | A used car must have kilometers greater than zero |
| VR-005 | `listed_value` | Must be > 0 | Listed value must be a positive number |
| VR-006 | `manufacturing_year` | >= 1886 and <= current year + 1 | Manufacturing year out of range |
| VR-007 | `status` | Must be AVAILABLE or UNAVAILABLE | Initial status cannot be Sold |
| VR-008 | `image_key` | If provided, valid pattern; empty string treated as null | Invalid image reference |

### Update (Patch)

| ID | Field(s) | Rule | Error |
|----|----------|------|-------|
| VR-020 | current `status` | Cannot modify car with status SOLD | Cannot modify a sold car |
| VR-021 | `listed_value` | If provided, must be > 0 | Listed value must be positive |
| VR-022 | `image_key` | If provided, valid; empty string = remove image | Invalid image reference |
| VR-023 | body | At least one mutable field present | No fields to update |

### Filter Parameters

| ID | Field(s) | Rule | Error |
|----|----------|------|-------|
| VR-030 | `min_value`, `max_value` | min <= max when both provided | Minimum value cannot exceed maximum value |
| VR-031 | `min_year`, `max_year` | min <= max when both provided | Minimum year cannot exceed maximum year |

## Database Schema (PostgreSQL)

```sql
CREATE TABLE car (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    model             VARCHAR(255) NOT NULL,
    manufacturing_year INTEGER     NOT NULL,
    manufacturer      VARCHAR(255) NOT NULL,
    external_color    VARCHAR(100) NOT NULL,
    internal_color    VARCHAR(100) NOT NULL,
    vin               VARCHAR(17)  NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    optional_items    JSONB,
    category          VARCHAR(20)  NOT NULL,
    kilometers        NUMERIC(12,2) NOT NULL DEFAULT 0,
    is_new            BOOLEAN      NOT NULL,
    propulsion_type   VARCHAR(20)  NOT NULL,
    listed_value      NUMERIC(12,2) NOT NULL,
    image_key         VARCHAR(500),
    registration_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_car_vin UNIQUE (vin),
    CONSTRAINT chk_car_status CHECK (status IN ('AVAILABLE','SOLD','UNAVAILABLE')),
    CONSTRAINT chk_car_category CHECK (category IN ('SUV','SEDAN','SPORT','HATCH','PICKUP')),
    CONSTRAINT chk_car_propulsion CHECK (propulsion_type IN ('ELECTRIC','COMBUSTION')),
    CONSTRAINT chk_car_value_positive CHECK (listed_value > 0),
    CONSTRAINT chk_car_year_range CHECK (manufacturing_year >= 1886)
);

CREATE INDEX idx_car_status ON car (status);
CREATE INDEX idx_car_category ON car (category);
CREATE INDEX idx_car_manufacturer ON car (manufacturer);
CREATE INDEX idx_car_propulsion_type ON car (propulsion_type);
CREATE INDEX idx_car_year ON car (manufacturing_year);
CREATE INDEX idx_car_value ON car (listed_value);
CREATE INDEX idx_car_status_category ON car (status, category);
```

## Cache Model

| Region | Key Pattern | TTL | Eviction |
|--------|-------------|-----|----------|
| `car-by-id` | `car-by-id::{carId}` | 24h | Evict on car update |
| `car-listings` | `car-listings::{hash(filter+sort+page)}` | 24h | Evict ALL entries on any car write |

### Cache Invalidation Matrix

| Operation | `car-by-id` | `car-listings` |
|-----------|-------------|----------------|
| Register car | — | Evict all |
| Update car | Evict by car ID | Evict all |
| Get by ID | Populate/hit | — |
| List/filter | — | Populate/hit |

