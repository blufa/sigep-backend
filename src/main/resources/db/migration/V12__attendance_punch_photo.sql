ALTER TABLE attendance_punches
    ADD COLUMN photo BYTEA,
    ADD COLUMN photo_content_type VARCHAR(100);
