# Migration SIGAP vers un nouveau VPS Hostinger (même domaine, mêmes données)

Ce guide transfère `sigep` (backend) + `sigep-ui` (frontend) **et leurs données** de l'ancien VPS
(celui qui partage le proxy Caddy avec `lacaeducation`) vers un **nouveau VPS Hostinger dédié**,
vierge. Le nom de domaine reste le même (`infrastructure.jeccamtech.com` et ses sous-domaines) —
seule l'IP derrière change, à la toute fin (Étape 9).

```
AVANT :  infrastructure.jeccamtech.com ──▶ ANCIEN VPS (IP_ANCIEN) ──▶ proxy Caddy partagé avec lacaeducation
APRÈS :  infrastructure.jeccamtech.com ──▶ NOUVEAU VPS (IP_NOUVEAU) ──▶ proxy Caddy dédié à sigep seul
```

Principe : on installe tout sur le nouveau serveur et on **vérifie que ça fonctionne** (via l'IP,
avant que le domaine ne pointe dessus) avant de basculer le DNS — donc zéro coupure si une étape
se passe mal, l'ancien serveur continue de servir le trafic réel jusqu'à la bascule finale.

Remplace partout `IP_ANCIEN` et `IP_NOUVEAU` par les deux IP réelles.

---

## Étape 1 — Docker sur le nouveau VPS

```bash
ssh root@IP_NOUVEAU
curl -fsSL https://get.docker.com | sh
mkdir -p /opt/sigep /opt/proxy
exit
```

Vérifie que ça a fonctionné :

```bash
ssh root@IP_NOUVEAU "docker --version && docker compose version"
```

---

## Étape 2 — Proxy Caddy dédié (le nouveau VPS n'a pas celui de lacaeducation)

Sur ta machine, dans le dépôt `sigep` :

```bash
cat > /tmp/proxy-compose.yml <<'EOF'
services:
  caddy:
    image: caddy:2-alpine
    container_name: proxy-caddy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile
      - caddy_data:/data
      - caddy_config:/config
    networks: [web]

volumes:
  caddy_data:
  caddy_config:

networks:
  web:
    name: web
EOF
scp /tmp/proxy-compose.yml root@IP_NOUVEAU:/opt/proxy/docker-compose.yml
scp deploy/Caddyfile.snippet root@IP_NOUVEAU:/opt/proxy/Caddyfile
```

Le fichier copié est un *snippet* commenté pour un proxy partagé — sur ce nouveau VPS il est seul,
donc nettoie l'en-tête en commentaire (facultatif, cosmétique) :

```bash
ssh root@IP_NOUVEAU
cd /opt/proxy
sed -i '1,3d' Caddyfile   # retire les 3 lignes de commentaire du haut, le reste est du Caddyfile valide
docker compose up -d
docker ps --filter name=proxy-caddy   # doit afficher "Up"
exit
```

---

## Étape 3 — Copier `.env.prod` / `.env.dev` depuis l'ancien VPS (mêmes identifiants, aucun changement)

Toujours depuis ta machine :

```bash
scp root@IP_ANCIEN:/opt/sigep/.env.prod /tmp/.env.prod
scp root@IP_ANCIEN:/opt/sigep/.env.dev /tmp/.env.dev
scp /tmp/.env.prod root@IP_NOUVEAU:/opt/sigep/.env.prod
scp /tmp/.env.dev root@IP_NOUVEAU:/opt/sigep/.env.dev
rm /tmp/.env.prod /tmp/.env.dev
```

En gardant les mêmes `POSTGRES_PASSWORD`/`JWT_SECRET` qu'avant : la restauration de la base
(Étape 5) n'a pas besoin d'ajuster d'identifiants, et les utilisateurs déjà connectés n'auront pas
à se reconnecter après la bascule.

---

## Étape 4 — Nouvelle clé SSH de déploiement pour le nouveau VPS

```bash
ssh-keygen -t ed25519 -C "deploy-sigep-nouveau-vps" -f ~/.ssh/sigep_deploy_key_v2 -N ""
ssh-copy-id -i ~/.ssh/sigep_deploy_key_v2.pub root@IP_NOUVEAU
ssh -i ~/.ssh/sigep_deploy_key_v2 root@IP_NOUVEAU "echo connexion OK"
```

Puis, dans **chacun** des deux dépôts GitHub (`sigep` et `sigep-ui`) → Settings → Secrets and
variables → Actions, **mets à jour** les secrets existants (ne pas en créer de nouveaux, éditer
les mêmes noms) :

| Secret | Nouvelle valeur |
|---|---|
| `VPS_HOST` | `IP_NOUVEAU` |
| `VPS_USER` | `root` |
| `VPS_SSH_KEY` | sortie complète de `cat ~/.ssh/sigep_deploy_key_v2` (avec les lignes `BEGIN`/`END`) |

**Ne pousse rien sur `develop`/`master` avant l'Étape 6** — tant que les données ne sont pas
restaurées, un déploiement démarrerait le backend sur une base vide.

---

## Étape 5 — Dump des bases sur l'ANCIEN VPS

```bash
ssh root@IP_ANCIEN
docker exec sigep-prod-db pg_dump -U sigep_prod -d sigep_prod -F c -f /tmp/sigep_prod.dump
docker exec sigep-dev-db pg_dump -U sigep_dev -d sigep_dev -F c -f /tmp/sigep_dev.dump
docker cp sigep-prod-db:/tmp/sigep_prod.dump /tmp/sigep_prod.dump
docker cp sigep-dev-db:/tmp/sigep_dev.dump /tmp/sigep_dev.dump
exit
```

(Adapte `sigep_prod`/`sigep_dev` si tes `POSTGRES_USER`/`POSTGRES_DB` dans `.env.prod`/`.env.dev`
portent un autre nom — regarde dedans avec `cat /opt/sigep/.env.prod` si besoin.)

Transfère les deux dumps sur ta machine puis vers le nouveau VPS :

```bash
scp root@IP_ANCIEN:/tmp/sigep_prod.dump /tmp/sigep_prod.dump
scp root@IP_ANCIEN:/tmp/sigep_dev.dump /tmp/sigep_dev.dump
scp /tmp/sigep_prod.dump root@IP_NOUVEAU:/tmp/sigep_prod.dump
scp /tmp/sigep_dev.dump root@IP_NOUVEAU:/tmp/sigep_dev.dump
```

---

## Étape 6 — Démarrer UNIQUEMENT les bases sur le nouveau VPS, puis restaurer

Il faut que `deploy/docker-compose.yml` soit déjà présent sur le nouveau VPS pour démarrer la
base seule — copie-le manuellement cette première fois (ensuite le déploiement GitHub Actions le
resynchronise tout seul à chaque push) :

```bash
scp deploy/docker-compose.yml root@IP_NOUVEAU:/opt/sigep/docker-compose.yml
ssh root@IP_NOUVEAU
cd /opt/sigep
docker network create web 2>/dev/null || true   # déjà créé à l'Étape 2, cette ligne est sans danger si elle existe
docker compose --env-file .env.prod -p sigep-prod up -d db
docker compose --env-file .env.dev  -p sigep-dev  up -d db
docker ps --filter "name=sigep" --filter "name=db"   # attendre "healthy" sur les deux avant de continuer
```

Restaure les dumps dans les bases fraîchement créées (encore vides) :

```bash
cat /tmp/sigep_prod.dump | docker exec -i sigep-prod-db pg_restore -U sigep_prod -d sigep_prod --no-owner --no-acl
cat /tmp/sigep_dev.dump  | docker exec -i sigep-dev-db  pg_restore -U sigep_dev  -d sigep_dev  --no-owner --no-acl
rm /tmp/sigep_prod.dump /tmp/sigep_dev.dump
exit
```

`--no-owner --no-acl` évite les erreurs si les rôles PostgreSQL internes diffèrent légèrement
entre les deux serveurs — sans incidence sur tes données applicatives.

---

## Étape 7 — Déployer le code (backend puis frontend, develop puis master)

Depuis ta machine, dans `sigep` :

```bash
git push origin develop
```

Va sur GitHub → Actions du dépôt `sigep`, attends le ✅. Puis dans `sigep-ui` :

```bash
git push origin develop
```

Attends le ✅, puis répète les deux `git push` sur `master` (backend d'abord) pour la prod :

```bash
# dans sigep
git push origin master
# dans sigep-ui
git push origin master
```

Le backend démarre sur la base restaurée : Flyway voit l'historique de migrations déjà présent
(copié avec le reste des données) et ne rejoue rien — tes employés, badges et pointages existants
sont là dès le premier démarrage.

---

## Étape 8 — Déclarer les domaines au nouveau proxy Caddy

```bash
docker exec proxy-caddy caddy reload --config /etc/caddy/Caddyfile
```

(Le `Caddyfile` a déjà les bons domaines depuis l'Étape 2 — ce reload n'est utile que si tu l'as
modifié entre-temps.)

---

## Étape 9 — Vérifier AVANT de toucher au DNS (le domaine pointe encore sur l'ancien VPS)

Force temporairement ton navigateur/terminal à parler au nouveau serveur sans changer le DNS
public, via l'en-tête `Host` :

```bash
curl -s -H "Host: api.infrastructure.jeccamtech.com" https://IP_NOUVEAU/api/auth/login \
  -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"password"}' -k
```

(`-k` ignore la vérification du certificat TLS — normal, Caddy ne l'a pas encore émis pour ce nom
sur cette IP tant que le DNS ne pointe pas dessus ; c'est juste pour tester la route.)

Si tu reçois un `accessToken` avec le même nom d'administrateur que sur la prod actuelle → les
données ont bien été restaurées et l'application fonctionne sur le nouveau serveur.

---

## Étape 10 — Bascule DNS (seule étape qui affecte le trafic réel)

Dans hPanel → `jeccamtech.com` → DNS, remplace l'IP des enregistrements A existants :

| Type | Nom | Ancienne valeur | Nouvelle valeur |
|------|-----|------------------|-------------------|
| A | `infrastructure` | IP_ANCIEN | **IP_NOUVEAU** |
| A | `*.infrastructure` (ou les 3 lignes séparées `api.infrastructure`/`dev.infrastructure`/`api-dev.infrastructure`) | IP_ANCIEN | **IP_NOUVEAU** |

```bash
dig +short infrastructure.jeccamtech.com
```

Attends que ça affiche `IP_NOUVEAU` (propagation : quelques minutes à quelques heures). Caddy sur
le nouveau VPS émettra automatiquement le certificat HTTPS dès la première requête réelle.

---

## Étape 11 — Vérification finale (une fois le DNS propagé)

```bash
curl -s https://api.infrastructure.jeccamtech.com/api/auth/login -o /dev/null -w "%{http_code}\n"
curl -s https://infrastructure.jeccamtech.com -o /dev/null -w "%{http_code}\n"
curl -s https://api-dev.infrastructure.jeccamtech.com/api/auth/login -o /dev/null -w "%{http_code}\n"
curl -s https://dev.infrastructure.jeccamtech.com -o /dev/null -w "%{http_code}\n"
```

Ouvre `https://infrastructure.jeccamtech.com` dans un navigateur et connecte-toi normalement —
tes employés/badges/pointages existants doivent apparaître tels quels.

---

## Étape 12 — Ancien VPS

Garde-le allumé quelques jours sans rien supprimer (filet de sécurité le temps de confirmer que
tout fonctionne bien sur le nouveau), avant de l'arrêter ou de le résilier dans hPanel.
