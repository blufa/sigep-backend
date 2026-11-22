# Déploiement SIGAP — guide pas à pas complet

Ce guide suppose que le VPS Hostinger et le proxy Caddy de `lacaeducation` existent déjà. SIGAP
**rejoint ce même VPS et ce même proxy** — rien à recréer, on ajoute juste une nouvelle application.
Chaque étape donne la commande exacte à taper et, quand un fichier doit être créé, son contenu
exact à copier-coller.

```
Internet ─▶ Caddy :443 (HTTPS auto, déjà en place) ─┬─ ecole.jeccamtech.com / api...            ─▶ lacaeducation
                                                       ├─ infrastructure.jeccamtech.com           ─▶ front PROD sigep
                                                       ├─ api.infrastructure.jeccamtech.com        ─▶ backend PROD ─▶ PostgreSQL
                                                       ├─ dev.infrastructure.jeccamtech.com        ─▶ front DEV sigep
                                                       └─ api-dev.infrastructure.jeccamtech.com    ─▶ backend DEV  ─▶ PostgreSQL
```

---

## Étape 1 — DNS

Dans hPanel → `jeccamtech.com` → DNS / Nameservers → Enregistrements DNS, ajouter :

| Type | Nom (Host)              | Pointe vers (Contenu) |
|------|--------------------------|------------------------|
| A    | `infrastructure`         | IP_DU_VPS (la même IP que `ecole`, visible dans hPanel → VPS → Vue d'ensemble) |
| A    | `*.infrastructure`       | IP_DU_VPS |

Le wildcard couvre `api.infrastructure`, `dev.infrastructure` et `api-dev.infrastructure` en une
fois. Si Hostinger refuse les wildcards, ajouter les 4 lignes une par une (`infrastructure`,
`api.infrastructure`, `dev.infrastructure`, `api-dev.infrastructure`), toutes en A vers IP_DU_VPS.

Vérifier que ça a propagé (peut prendre de quelques minutes à quelques heures) :

```bash
dig +short infrastructure.jeccamtech.com api.infrastructure.jeccamtech.com
```

Chaque commande doit afficher l'IP du VPS. Si rien ne s'affiche, attends encore un peu avant de
continuer.

---

## Étape 2 — Dossier sur le VPS

```bash
ssh root@IP_DU_VPS
mkdir -p /opt/sigep
exit
```

(Remplace `root` par ton utilisateur habituel si ce n'est pas `root`.)

---

## Étape 3 — Clé SSH de déploiement

Si tu veux réutiliser exactement la même clé que lacaeducation, passe directement à l'Étape 4 et
réutilise les mêmes valeurs `VPS_HOST`/`VPS_USER`/`VPS_SSH_KEY`. Sinon, génère-en une nouvelle
**sur ta machine** (pas sur le VPS) :

```bash
ssh-keygen -t ed25519 -C "deploy-sigep" -f ~/.ssh/sigep_deploy_key -N ""
```

Ça crée deux fichiers :
- `~/.ssh/sigep_deploy_key` (clé **privée** — ira dans le secret GitHub `VPS_SSH_KEY`)
- `~/.ssh/sigep_deploy_key.pub` (clé **publique** — à autoriser sur le VPS)

Copie la clé publique sur le VPS :

```bash
ssh-copy-id -i ~/.ssh/sigep_deploy_key.pub root@IP_DU_VPS
```

Teste que ça fonctionne avant de continuer :

```bash
ssh -i ~/.ssh/sigep_deploy_key root@IP_DU_VPS "echo connexion OK"
```

Tu dois voir `connexion OK` s'afficher. Si ça demande un mot de passe, la clé n'est pas encore
acceptée — recommence `ssh-copy-id`.

---

## Étape 4 — Secrets GitHub (dans les DEUX dépôts : `sigep` ET `sigep-ui`)

Pour **chacun** des deux dépôts GitHub : Settings → Secrets and variables → Actions → New
repository secret. Créer ces 3 secrets, avec exactement ces noms :

| Nom du secret | Valeur à coller |
|---|---|
| `VPS_HOST` | L'IP du VPS (ex. `123.45.67.89`) |
| `VPS_USER` | `root` (ou ton utilisateur SSH) |
| `VPS_SSH_KEY` | Le contenu **complet** de `~/.ssh/sigep_deploy_key` (la clé privée), copié avec `cat ~/.ssh/sigep_deploy_key` — colle tout, y compris les lignes `-----BEGIN ...-----` et `-----END ...-----` |

```bash
cat ~/.ssh/sigep_deploy_key
```

Copie toute la sortie de cette commande dans le secret `VPS_SSH_KEY`.

---

## Étape 5 — Fichiers `.env.prod` et `.env.dev` sur le VPS

Ces fichiers ne sont **jamais commités** (créés à la main, uniquement sur le VPS). Connecte-toi :

```bash
ssh root@IP_DU_VPS
cd /opt/sigep
```

Génère deux secrets aléatoires (un pour chaque environnement, à utiliser ci-dessous) :

```bash
openssl rand -base64 48   # pour JWT_SECRET de .env.prod
openssl rand -base64 48   # pour JWT_SECRET de .env.dev — relance la commande, ne réutilise pas le même
openssl rand -base64 24   # pour POSTGRES_PASSWORD de .env.prod
openssl rand -base64 24   # pour POSTGRES_PASSWORD de .env.dev
```

Garde les 4 résultats sous la main, puis crée le fichier de production :

```bash
nano .env.prod
```

Colle ce contenu, en remplaçant les 3 lignes marquées `# ⚠️ À REMPLIR` par tes propres valeurs
(le `GHCR_OWNER` est le nom d'utilisateur ou d'organisation GitHub qui possède le dépôt `sigep` —
visible dans l'URL `github.com/<GHCR_OWNER>/sigep`) :

```bash
STACK=sigep-prod
GHCR_OWNER=ton-pseudo-github        # ⚠️ À REMPLIR
BACKEND_TAG=prod
FRONTEND_TAG=prod

POSTGRES_DB=sigep_prod
POSTGRES_USER=sigep_prod
POSTGRES_PASSWORD=colle-ici-le-1er-openssl-rand-base64-24   # ⚠️ À REMPLIR

JWT_SECRET=colle-ici-le-1er-openssl-rand-base64-48          # ⚠️ À REMPLIR
JWT_EXPIRATION_MINUTES=480

FRONT_URL=https://infrastructure.jeccamtech.com
```

Enregistre (`Ctrl+O`, `Entrée`, puis `Ctrl+X` pour quitter nano). Fais la même chose pour le
développement :

```bash
nano .env.dev
```

```bash
STACK=sigep-dev
GHCR_OWNER=ton-pseudo-github        # ⚠️ À REMPLIR (même valeur que .env.prod)
BACKEND_TAG=dev
FRONTEND_TAG=dev

POSTGRES_DB=sigep_dev
POSTGRES_USER=sigep_dev
POSTGRES_PASSWORD=colle-ici-le-2e-openssl-rand-base64-24    # ⚠️ À REMPLIR

JWT_SECRET=colle-ici-le-2e-openssl-rand-base64-48           # ⚠️ À REMPLIR
JWT_EXPIRATION_MINUTES=480

FRONT_URL=https://dev.infrastructure.jeccamtech.com
```

Vérifie que les deux fichiers existent bien et ne sont pas vides :

```bash
cat .env.prod
cat .env.dev
exit
```

---

## Étape 6 — Premier déploiement (depuis ta machine, dans le dépôt `sigep`)

`deploy/docker-compose.yml` est synchronisé automatiquement sur le VPS par
`.github/workflows/deploy.yml` à chaque push — rien à copier toi-même.

Pousse d'abord sur `develop` (ça déploie l'environnement de test, pour vérifier que tout
fonctionne avant la prod) :

```bash
git push origin develop
```

Va sur GitHub → onglet **Actions** du dépôt `sigep` et regarde le workflow tourner. Il doit finir
en vert (✅). S'il échoue, clique dessus pour voir le message d'erreur exact.

Une fois vert, fais la même chose côté frontend (dépôt `sigep-ui`) :

```bash
git push origin develop
```

Répète ensuite les deux `git push` ci-dessus mais sur `master` pour déployer la production :

```bash
# dans sigep
git push origin master
# dans sigep-ui
git push origin master
```

**Important** : le backend doit toujours être poussé en premier (il dépose
`deploy/docker-compose.yml` sur le VPS) — le frontend en a besoin pour démarrer son propre
conteneur.

---

## Étape 7 — Ajouter les domaines au proxy Caddy existant

Le proxy (`/opt/lacaeducation/proxy/Caddyfile`, partagé avec lacaeducation) ne connaît pas encore
SIGAP. Depuis ta machine, dans le dépôt `sigep` :

```bash
scp deploy/Caddyfile.snippet root@IP_DU_VPS:/tmp/sigep-caddy.snippet
ssh root@IP_DU_VPS
cat /tmp/sigep-caddy.snippet >> /opt/lacaeducation/proxy/Caddyfile
docker exec proxy-caddy caddy reload --config /etc/caddy/Caddyfile
rm /tmp/sigep-caddy.snippet
exit
```

Caddy obtient automatiquement les certificats HTTPS (Let's Encrypt) pour les nouveaux domaines dès
le premier accès — rien d'autre à configurer.

---

## Étape 8 — Vérification

```bash
curl -s https://api.infrastructure.jeccamtech.com/api/auth/login -o /dev/null -w "%{http_code}\n"
# 400 ou 401 attendu (pas 502/404) = le backend prod répond bien derrière Caddy

curl -s https://infrastructure.jeccamtech.com -o /dev/null -w "%{http_code}\n"
# 200 attendu = le frontend prod répond bien derrière Caddy

curl -s https://api-dev.infrastructure.jeccamtech.com/api/auth/login -o /dev/null -w "%{http_code}\n"
curl -s https://dev.infrastructure.jeccamtech.com -o /dev/null -w "%{http_code}\n"
# mêmes codes attendus pour l'environnement dev
```

Si un `curl` renvoie `502` : le conteneur n'a pas démarré — va vérifier sur le VPS :

```bash
ssh root@IP_DU_VPS
docker ps -a --filter "name=sigep"          # les 6 conteneurs (prod+dev : db/backend/frontend) doivent être "Up"
docker logs sigep-prod-backend --tail 50    # erreurs éventuelles au démarrage
```

Ouvre enfin `https://infrastructure.jeccamtech.com` dans un navigateur : connecte-toi avec
`admin` / `password` (compte créé par défaut au premier démarrage, voir `V6__seed.sql`) — et
change ce mot de passe immédiatement une fois connecté.

<!-- test-deploy: valider le pipeline develop (claude 2026-11-21T00:48Z) -->
