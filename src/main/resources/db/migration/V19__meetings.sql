CREATE TABLE meetings (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL REFERENCES tenants (id),
    title        VARCHAR(200) NOT NULL,
    description  VARCHAR(1000),
    scheduled_at TIMESTAMP    NOT NULL,
    location     VARCHAR(200),
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE meeting_attendees (
    meeting_id  BIGINT NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES employees (id)
);

CREATE INDEX idx_meetings_tenant ON meetings (tenant_id);
CREATE INDEX idx_meeting_attendees_meeting ON meeting_attendees (meeting_id);
