CREATE TABLE missions (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT    NOT NULL REFERENCES tenants (id),
    employee_id BIGINT    NOT NULL REFERENCES employees (id),
    start_date  DATE      NOT NULL,
    end_date    DATE      NOT NULL,
    reason      VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_missions_tenant ON missions (tenant_id);
CREATE INDEX idx_missions_employee_dates ON missions (employee_id, start_date, end_date);
