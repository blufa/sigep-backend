CREATE TABLE positions (
    id        BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT       NOT NULL REFERENCES tenants (id),
    title     VARCHAR(150) NOT NULL,
    code      VARCHAR(50)  NOT NULL,
    active    BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (tenant_id, code)
);

-- matricule = matricule d'Etat de l'agent (attribué par l'Etat, pas généré par SIGEP)
CREATE TABLE employees (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL REFERENCES tenants (id),
    matricule    VARCHAR(50)  NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    first_name   VARCHAR(100) NOT NULL,
    hierarchie   VARCHAR(100),
    genre        VARCHAR(10)  NOT NULL,
    position_id  BIGINT REFERENCES positions (id),
    phone        VARCHAR(30),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, matricule)
);

CREATE INDEX idx_positions_tenant ON positions (tenant_id);
CREATE INDEX idx_employees_tenant ON employees (tenant_id);
