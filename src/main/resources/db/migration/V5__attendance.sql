CREATE TABLE attendance_punches (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL REFERENCES tenants (id),
    employee_id BIGINT       NOT NULL REFERENCES employees (id),
    badge_id    BIGINT REFERENCES employee_badges (id),
    direction   VARCHAR(10)  NOT NULL,
    scanned_at  TIMESTAMP    NOT NULL DEFAULT now(),
    source      VARCHAR(20)  NOT NULL DEFAULT 'QR'
);

CREATE INDEX idx_attendance_tenant ON attendance_punches (tenant_id);
CREATE INDEX idx_attendance_employee_day ON attendance_punches (employee_id, scanned_at);
