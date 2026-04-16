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

