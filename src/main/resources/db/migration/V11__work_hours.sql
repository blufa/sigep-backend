ALTER TABLE tenants
    ADD COLUMN work_start_time TIME,
    ADD COLUMN work_end_time TIME;

UPDATE tenants
SET work_start_time = '08:30',
    work_end_time   = '16:30'
WHERE code = 'SNIESM';
