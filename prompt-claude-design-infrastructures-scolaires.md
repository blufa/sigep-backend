# Prompt pour Claude Design — Plateforme d'inventaire du patrimoine scolaire (SNIESM)

```
Tu es un architecte logiciel senior spécialisé dans les systèmes d'information pour le secteur public (éducation/infrastructures). Conçois l'architecture d'une plateforme de gestion du patrimoine scolaire pour le Service National des Infrastructures et Équipements Scolaires et Maintenances (SNIESM), sous le Ministère de l'Éducation Nationale, de l'Enseignement Technique, de la Formation Professionnelle et de l'Alphabétisation (MENET-FP) — République de Guinée.

## Contexte et objectif métier

Le SNIESM a besoin d'un inventaire national exhaustif et à jour de tout le patrimoine scolaire, pour éclairer les décisions d'investissement : faut-il construire une nouvelle école, rénover un bâtiment existant, ou étendre un établissement déjà en place ? Ces décisions doivent être objectivées par des données (capacité réelle vs besoin, état des infrastructures, équipements manquants), pas par appréciation subjective.

## Ce qui doit être recensé, par établissement

- Identité de l'école : nom, code officiel, type (préscolaire/primaire/collège/lycée/technique), statut (public/privé), date de création
- Localisation administrative complète : région → préfecture → sous-préfecture/commune → zone/secteur, avec coordonnées GPS
- Bâtiments : nombre, nombre de salles de classe, matériaux de construction, année de construction, état général (bon/moyen/dégradé/à démolir)
- Capacité : capacité d'accueil théorique (nombre de places), effectif réel d'élèves inscrits, taux d'occupation, nombre d'enseignants
- Équipements et mobilier : tables-bancs, tableaux, bibliothèque, laboratoires, matériel informatique — quantité, état, besoin
- Latrines/sanitaires : nombre, séparées filles/garçons, état fonctionnel, accès à l'eau
- Accès aux services de base : eau potable, électricité, clôture/sécurité, accessibilité pour élèves en situation de handicap
- Photos de l'état actuel des infrastructures (preuve visuelle, horodatée, géolocalisée)

## Logique métier centrale : aide à la décision

Le système doit calculer, pour chaque école ou chaque zone, un écart entre la situation constatée et des normes de référence officielles (ratio élèves/salle, élèves/latrine, élèves/enseignant, etc. — à paramétrer, configurables par l'administrateur). À partir de cet écart, il doit :
- Signaler automatiquement les établissements en déficit (sur-effectif, latrines insuffisantes, bâtiments dégradés)
- Proposer un score de priorité d'intervention (construction neuve / rénovation / extension) par école et agrégé par zone/préfecture/région
- Permettre une vue cartographique (carte de la Guinée avec code couleur par niveau de besoin, filtrable par région/préfecture)
- Permettre une vue tableau de bord exécutif : synthèse nationale, par région, comparaisons dans le temps

## Collecte des données sur le terrain — point critique

Les agents qui visitent les écoles (inspecteurs, DPE/DCE, techniciens du SNIESM) travaillent souvent en zone rurale sans connexion internet fiable. L'architecture doit prévoir :
- Une application mobile de collecte fonctionnant hors-ligne (saisie + photos + GPS stockés localement), avec synchronisation différée dès qu'une connexion est disponible
- Un cycle de mise à jour périodique (campagnes de recensement annuelles ou à la demande), avec historique des versions pour suivre l'évolution dans le temps (un bâtiment qui se dégrade, une extension qui a été réalisée)
- Un circuit de validation : les données remontées par un agent de terrain sont-elles automatiquement actives, ou doivent-elles être validées par un niveau hiérarchique avant de compter dans les statistiques officielles ?

## Architecture SaaS multi-tenant — obligatoire

La plateforme doit être conçue dès le départ comme un SaaS multi-tenant, sur le même principe que ce qui a déjà été fait pour la plateforme sœur "lacaeducation" :

- Un **super-utilisateur** (super-admin), au-dessus de tous les services, qui peut créer, activer/désactiver et administrer les **tenants** (= les services rattachés au ministère). Un ministère comporte en effet plusieurs services/directions distincts (ex. SNIESM, mais aussi d'autres directions qui pourraient rejoindre la plateforme plus tard), chacun avec ses propres données, utilisateurs et périmètre d'écoles, totalement isolés les uns des autres.
- Chaque **service (tenant)** a son propre espace : ses utilisateurs, ses écoles/régions suivies, ses rapports — aucune donnée ne doit fuiter d'un service à l'autre.
- Le super-admin peut créer un service, y rattacher un premier administrateur, et ce dernier gère ensuite ses propres utilisateurs internes (régionaux, préfectoraux, agents de terrain) à l'intérieur de son tenant.
- Isolation technique à prévoir explicitement : chaque donnée métier (écoles, bâtiments, équipements...) rattachée à un tenant, avec un mécanisme de filtrage automatique par tenant au niveau de la couche d'accès aux données (pas seulement côté interface), pour qu'aucune requête ne puisse accidentellement mélanger les données de deux services.
- Prévoir un tableau de bord dédié au super-admin : liste des services actifs, statistiques d'usage globales, gestion des accès — distinct du tableau de bord métier de chaque service (celui destiné au DG/DGA de ce service).

## Rôles et niveaux d'accès

- **Super-administrateur (niveau plateforme, tous services)** : création/gestion des services (tenants), supervision globale
- Niveau national du service (DG/DGA, ex. SNIESM) : vue consolidée de son service, arbitrage budgétaire, export de rapports
- Niveau régional (DREF) : vue sur sa région, au sein de son service
- Niveau préfectoral (DPE/DCE) : vue sur sa préfecture, saisie/validation
- Agent de terrain : saisie uniquement, sur les écoles qui lui sont assignées

## Ce que je demande concrètement

1. Un schéma d'architecture globale (couches applicatives, flux de données entre la collecte terrain, la base centrale, et les tableaux de bord)
2. Un modèle de données (entités principales et leurs relations) couvrant tout ce qui précède
3. Une proposition de découpage en modules fonctionnels
4. Les choix techniques recommandés pour le scénario hors-ligne/synchronisation (c'est le point le plus délicat du projet)
5. Une maquette ou description du tableau de bord décisionnel destiné à la direction (DG/DGA), pensée pour une présentation à la hiérarchie — sobre, lisible en une vue, avec la carte par niveau de besoin en élément central

## Style attendu du livrable

Document clair et structuré, présentable tel quel à une direction générale (DG/DGA) — pas de jargon technique non expliqué, chaque choix d'architecture justifié en une phrase par son bénéfice métier.
```

---

**Points ajoutés par rapport à la demande initiale** (fréquemment oubliés dans ce type de cahier des charges, et probables premières questions du DGA) :

- **Collecte hors-ligne** — sans ça le projet échoue dès la première visite en zone rurale sans réseau
- **Normes de référence configurables** — sans seuil officiel, impossible de dire objectivement qu'une école est "en besoin"
- **Score de priorité** — sinon le DGA doit lire des milliers de fiches une par une
- **Circuit de validation hiérarchique** des données remontées par les agents
- **Historique dans le temps** — une seule photo de l'état actuel ne permet pas de voir si la situation s'améliore ou se dégrade

Retirer ce qui ne s'applique pas au contexte avant envoi.
