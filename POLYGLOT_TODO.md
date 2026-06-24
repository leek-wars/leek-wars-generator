# Polyglot AI — Chantiers restants

> Liste de travail pour livrer le support IA **JavaScript / TypeScript / Python** (issue `5pilow/leek-wars#3179`) en prod 2.49.
> Compagnon du `POLYGLOT_PORTING_GUIDE.md` (pièges runtime). Ce fichier-ci = la todo-list ; le guide = le savoir technique.

## État au 2026-06-24

**Branche generator** : `polyglot-recon` (worktree `/tmp/lwpoly-recon/generator`, HEAD `9f5104e`) = `master` + 32 commits, **0 behind**. Submodule leekscript = master (`9fb1e29`).

**Déployé en beta et opérationnel** :
- 3 langages : JS, TypeScript (transpilé `tsc` vendoré), Python (GraalPy). Bridge API plate + stdlib LeekScript.
- Déterminisme : instrument Truffle `StatementCounter` (bit-reproductible).
- Anti-DoS : statement limit + watchdog wall-clock (5 s/tour, 3 dépassements → IA neutralisée).
- Sandbox sécu auditée (évasion hôte / process / FS / déterminisme tous bloqués).
- Multi-fichiers (modules ES pour JS/TS, imports natifs Python via FileSystem polyglot en mémoire).
- API objet complète (style LS5) sur les 3 langages + éditeur : `me` / `Entity` / `Cell` / `Weapon` / `Chip` / `Fight` / `Field` / `Registers` / `Debug`, effets en objets `Effect` / `EffectTemplate`.
- Dépréciation forme plate amorcée (`@deprecated` dans `leekwars.d.ts`).
- Éditeur : coloration + language service Monaco TS + `leekwars.d.ts` auto-généré + pont markers → panneau d'erreurs LW.
- Worker/daemon beta GraalVM opérationnels (pièges fat-jar zip64 / signatures / Multi-Release / merge-services réglés).

**Branches d'intégration** (cherry-pick prod) :
- server `feature/polyglot-ai-integration` = `origin/master` + 7 commits (`251de575`)
- client `feature/polyglot-ai-integration` = `origin/master` + 11 commits (`fbf463f22`)
- ⚠️ Ces deux branches **ne contiennent PAS** encore les commits effets-objets (generator `9f5104e` / client `66c9af120`) → à re-dériver de develop avant le cut prod.

---

## A. Valider un combat polyglot end-to-end sur beta

> ✅ **BASELINE VALIDÉE end-to-end sur beta (24/06)** — fight `52437868` (Klaude vs PolyDummy) : IA `.js` en **API objet** compilée par le daemon (`ai/write` → `problems:[]`) puis exécutée par le worker GraalVM (51 actions, équipement d'arme + déplacement + tir, aucune erreur d'IA). Le pipeline polyglot de base tourne en prod-beta.

- [x] Accès beta opérationnel (gate cookie `memory/.beta_access.cookie` + login Klaude ; mdp beta réaligné via DB, cf [[reference_beta_access]])
- [x] Créer une IA `.js` + écrire le code via l'API → daemon valide sans erreur
- [x] Combat lancé (test-scenario) et terminé ; l'API objet (`Fight.getNearestEnemy`/`me.weapons`/`me.setWeapon`/`me.moveToward`/`me.useWeapon`) exécutée en combat réel
- [ ] Idem pour `.ts` et `.py` sur beta (baseline JS prouvée ; TS/PY = même pipeline, à confirmer)
- [ ] Diagnostics éditeur live (coloration + syntaxe daemon + types Monaco) sur du code cassé, via l'UID navigateur connecté
- [ ] Valider le déterminisme : un combat miroir rejoué donne le même résultat
- [ ] **`me.summon` PAS encore validable sur beta** : commité aujourd'hui sur `polyglot-recon`, pas déployé → nécessite un deploy beta (push `private/develop` + dispatch `generator-updated` ref develop) avant validation

## B. `me.summon` (seul vrai trou fonctionnel de l'API objet)

> ✅ **FAIT (24/06)** — conçu via workflow (cartographie + 3 revues adversariales), implémenté et vérifié (suite polyglot complète verte). Modèle d'exécution confirmé : le tour du bulbe est une itération séparée (pas de réentrance), `getEntity()` renvoie déjà le bulbe ; seul `me` était figé.

- [x] **Verrou 1 — marshaller callback** : `TypeMarshaller.coerce` enveloppe une `Value.canExecute()` en `FunctionLeekValue` (`wrapGuestFunction`, `parametersCount=0`) → ré-invoquée par `BulbAI`.
- [x] **Verrou 2 — `me` dynamique** : `me.id` = getter `getEntity()` + setter no-op (objects.js `Object.defineProperty`, objects.py `@property`). Les autres `Entity` gardent un id figé. Coût d'ops nul (vérifié).
- [x] **Gardes par-tour pour le bulbe** (trous critiques relevés par la revue : le bulbe ne passe pas par `runIA`) → `PolyglotEntityAI.runGuestCallback` rejoue `turnStartNanos` + `statementCounter.reset()` + `context.resetLimits()` + watchdog wall-clock + `try/catch → mapException`. Ferme : statement limit cumulatif (tuait un tour ultérieur de l'invocateur), watchdog absent (DoS), exceptions guest mal classées en erreur serveur + contexte empoisonné.
- [x] `me.summon(chip, cell, callback[, name])` ajouté à `Me` (objects.js + objects.py).
- [x] Tests (3, verts) : `summonedBulbIsMeDuringItsTurn` (JS), `summonedBulbIsMeDuringItsTurnPython`, `summonCallbackErrorIsGracefulForOwner` (invocateur survit 64 tours à un callback qui lève).
- Différé (non bloquant) : test DoS lourd dédié (boucle/travail natif du bulbe coupé) — chemin de garde identique à `runIA`, déjà couvert par TestPolyglotDoS ; isolation du flux RNG owner/bulbe (couplage non documenté, OK si ordre de tour déterministe).

## C. Dépréciation de la forme plate

- [ ] Valider live : fonctions plates barrées dans l'éditeur + remontée au panneau d'erreurs
- [ ] Tuner la sévérité du marker `deprecated` dans le pont markers→panneau
- [ ] À terme : passer en erreur dure (comme LS5 §2.2) — décision de timing à acter

## D. Documentation joueurs

- [ ] Doc joueur : écrire une IA en JS / TS / Python (contrat `turn()`, entrée, multi-fichiers)
- [ ] Doc de l'API objet (`me`, `Entity`, `Cell`, `Weapon`, `Chip`, `Fight`, `Field`, `Registers`, `Debug`, `Effect`/`EffectTemplate`)
- [ ] Mentionner les limites (heap non bornée, pas de hooks beforeFight/afterFight, dépréciation forme plate)

## E. Release prod 2.49

> Bundle complet : polyglot + Stripe + challenges (#1320) + reste. Historique linéaire (cherry-pick par hash, pas par range).

- [ ] **Re-sync les branches d'intégration** : ajouter les commits effets-objets manquants (generator `9f5104e` / client `66c9af120`) en re-dérivant de develop
- [ ] Vérifier que les 4 fixes fat-jar (zip64 / signatures / Multi-Release / merge-services) sont bien sur la branche server d'intégration (pas seulement zip64+signatures)
- [ ] Cherry-pick generator → master (polyglot-recon prêt : master + 32, 0 behind)
- [ ] Cherry-pick server `feature/polyglot-ai-integration` → master/prod
- [ ] Cherry-pick client `feature/polyglot-ai-integration` → master/prod
- [ ] Vérif identité fichier-à-fichier avec develop (qui a buildé vert) sauf fixes non-polyglot exclus
- [ ] Smoke-test : `gradle beta` (jar zip64), créer un Engine depuis le fat jar (`Context.eval("js","40+2")==42`)
- [ ] Suivre le deploy CI (push master = auto-deploy) ; vérifier les services worker/daemon prod

## F. Tuning diagnostics éditeur (mineur)

- [ ] Affiner les retours de diagnostics syntaxe/type en direct
- [ ] Vérifier le warmup transpileur TS au boot worker + sandbox de validation daemon

---

## Tests de référence (worktree recon)

`gradle :test --tests 'test.<Classe>'` (PAS `gradle test` → déclenche `:leekscript:test` cassé en local JDK 25) :

- `TestPolyglotObjectApi` — API objet JS+Python (props cohérentes, combat, Weapon/Chip/Effect)
- `TestQuantumFight` / `TestQuantumDuel` — IA Quantum portée JS / duel JS-vs-PY
- `TestTypeScriptFight` / `TestTypeScriptMultiFile` — IA TS en combat
- `TestThreeWayFight` — JS vs Python vs LeekScript dans le même combat
- `TestPolyglotSecurity` / `TestPolyglotDoS` / `TestPolyglotDeterminism` — gardes
- `TestStatementCounterSpike` — instrument Truffle

## Topologie déploiement (rappel)

- generator polyglot → `private/develop` ; le public leekscript master doit avoir `getStandardFunctions` (OK `9fb1e29`).
- worker/daemon beta : `docker-{worker,daemon}-beta.yml` (push server develop `worker/**`|`daemon/**` ou `repository_dispatch: generator-updated`). **PIÈGE** : le dispatch exécute le workflow de master → checkout `ref: develop` explicite (fix `6c31d07b` sur master server).
- api/static/client beta : push develop des repos respectifs.
- Aucune migration DB (langage détecté par extension ; `leek.ai_path` + FS = source de vérité).
