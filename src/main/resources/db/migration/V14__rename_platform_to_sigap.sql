-- La plateforme s'appelle désormais SIGAP (anciennement SIGEP) — mise à jour du nom affiché
-- du compte administrateur de démarrage seedé en V6.
UPDATE users SET full_name = 'Administrateur SIGAP' WHERE username = 'admin' AND full_name = 'Administrateur SIGEP';
