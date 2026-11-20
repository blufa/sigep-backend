-- Menus visibles pour un utilisateur, assignés par son administrateur (liste de clés séparées par
-- des virgules, ex. "dashboard,attendance"). NULL = aucune restriction (tous les menus de son rôle).
ALTER TABLE users ADD COLUMN allowed_menus TEXT;
