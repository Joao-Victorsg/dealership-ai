CREATE TABLE IF NOT EXISTS sales
(
    id              UUID             NOT NULL DEFAULT gen_random_uuid(),
    car_id          UUID             NOT NULL,
    client_id       UUID             NOT NULL,
    sale_value      NUMERIC(19, 4)   NOT NULL,
    registered_at   TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    client_snapshot JSONB            NOT NULL,
    car_snapshot    JSONB            NOT NULL,

    CONSTRAINT pk_sales PRIMARY KEY (id),
    CONSTRAINT uk_sales_car_id UNIQUE (car_id),
    CONSTRAINT chk_sales_value CHECK (sale_value > 0),
    CONSTRAINT chk_car_snapshot_status CHECK (
        (car_snapshot ->> 'status') IN ('AVAILABLE', 'SOLD', 'UNAVAILABLE')
    )
);

CREATE INDEX IF NOT EXISTS idx_sales_client_id
    ON sales (client_id);

CREATE INDEX IF NOT EXISTS idx_sales_registered_at
    ON sales (registered_at);

CREATE INDEX IF NOT EXISTS idx_sales_client_registered
    ON sales (client_id, registered_at);
