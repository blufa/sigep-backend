-- Le matricule d'Etat de l'employé (table employees) sert déjà d'identifiant
-- humainement lisible sur le badge ; seul le qr_token (secret, non deviné) est
-- propre à cette table.
CREATE TABLE employee_badges (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL REFERENCES tenants (id),
    employee_id BIGINT       NOT NULL UNIQUE REFERENCES employees (id),
    qr_token    VARCHAR(100) NOT NULL UNIQUE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    issued_at   TIMESTAMP    NOT NULL DEFAULT now(),
    printed_at  TIMESTAMP
);

CREATE INDEX idx_employee_badges_tenant ON employee_badges (tenant_id);
