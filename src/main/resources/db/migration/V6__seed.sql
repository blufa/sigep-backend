-- Tenant initial : le Service National des Infrastructures et Équipements Scolaires et Maintenances.
INSERT INTO tenants (name, code, active)
VALUES ('Service National des Infrastructures et Équipements Scolaires et Maintenances', 'SNIESM', TRUE);

-- Compte administrateur de démarrage. Mot de passe par défaut : "password" (bcrypt).
-- A CHANGER IMMEDIATEMENT après la première connexion.
INSERT INTO users (tenant_id, username, password_hash, full_name, role, active)
SELECT id, 'admin', '$2a$10$ehtmy0P888hLRzbTsDucUe43tEoH/x9R25Tokqzh0MGeRth89QJuK', 'Administrateur SIGEP', 'ADMIN', TRUE
FROM tenants
WHERE code = 'SNIESM';
