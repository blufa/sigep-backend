-- Un SUPER_ADMIN n'est rattaché à aucun service (tenant) : il les administre tous.
ALTER TABLE users ALTER COLUMN tenant_id DROP NOT NULL;

-- Compte super-administrateur de démarrage. Mot de passe par défaut : "password" (bcrypt).
-- A CHANGER IMMEDIATEMENT après la première connexion.
INSERT INTO users (tenant_id, username, password_hash, full_name, role, active)
VALUES (NULL, 'superadmin', '$2a$10$ehtmy0P888hLRzbTsDucUe43tEoH/x9R25Tokqzh0MGeRth89QJuK', 'Super Administrateur', 'SUPER_ADMIN', TRUE);
