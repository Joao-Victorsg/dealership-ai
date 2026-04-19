CREATE TABLE clients (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id      VARCHAR(255) NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    cpf              VARCHAR(100) NOT NULL,
    cpf_hash         VARCHAR(64)  NOT NULL,
    phone_number     VARCHAR(20)  NOT NULL,
    postcode         VARCHAR(10)  NOT NULL,
    street_number    VARCHAR(20)  NOT NULL,
    street_name      VARCHAR(200),
    city             VARCHAR(100),
    state            VARCHAR(2),
    address_searched BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMP
);

CREATE UNIQUE INDEX uq_clients_keycloak_id ON clients (keycloak_id);
CREATE UNIQUE INDEX uq_clients_cpf_hash    ON clients (cpf_hash);
