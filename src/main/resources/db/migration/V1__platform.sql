CREATE TABLE tenants (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);
