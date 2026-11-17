ALTER TABLE tenants
    DROP COLUMN city;

ALTER TABLE employees
    ADD COLUMN city VARCHAR(255);
