CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL REFERENCES tenants (id),
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_users_tenant ON users (tenant_id);
