ALTER TABLE tenants
    ADD COLUMN ministry_name VARCHAR(500),
    ADD COLUMN service_name VARCHAR(500),
    ADD COLUMN logo BYTEA,
    ADD COLUMN logo_content_type VARCHAR(100);

UPDATE tenants
SET ministry_name = 'Ministère de l''Éducation Nationale, de l''Enseignement Technique, de la Formation Professionnelle et de l''Alphabétisation',
    service_name  = 'Service National des Infrastructures, Équipements Scolaires et Maintenance'
WHERE code = 'SNIESM';
