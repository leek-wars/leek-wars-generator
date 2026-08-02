# Instructions pour Claude Code - Leek Wars Generator

## Déploiement

**`git push` sur `master` = déploiement.** Il n'y a pas d'étape manuelle : pousser met le
générateur en production. Une modification du prélude polyglot ou d'une fonction de combat
s'applique donc aux combats réels dès le push.

Corollaire pour toute évolution touchant AUSSI le client (documentation, typages de l'éditeur) :
**pousser le générateur EN PREMIER**. Sinon le client annonce des fonctions que le moteur
n'expose pas encore, et le joueur écrit du code qui plante.

## Structure

- **Générateur de combats** Java 25, `src/main/java/com/leekwars/generator/`
- **`leekscript`** est un **sous-module** git (projet Gradle `:leekscript`) : le compilateur et
  le runtime du langage. Vérifier son commit avant de conclure quoi que ce soit sur le langage.
- **Fonctions de combat** : `FightFunctions.java` (registre) + `classes/*Class.java` (implémentations).
  Convention : nom logique `X` → classe `<X>Class`, méthode statique de même nom, `EntityAI` en 1er
  paramètre. Le registre de la stdlib, lui, est dans `leekscript/.../LeekFunctions.java`.
- **Polyglot** (IA en JS/TS/Python, GraalVM) : `polyglot/`, et surtout les deux préludes
  `src/main/resources/polyglot/objects.js` et `objects.py`.

## Tests

```sh
gradle --offline :test                              # suite du générateur (~6 min)
gradle --offline :test --tests "test.TestXxx"       # un fichier
```

⚠️ **Le `:` est obligatoire.** Sans lui, Gradle applique le filtre au sous-projet `:leekscript`
aussi, qui n'a aucun test correspondant, et échoue sur
`No tests found for given includes` — un faux négatif déroutant.

## Préludes polyglot

`objects.js` et `objects.py` sont **deux miroirs maintenus à la main** (~1400 lignes au total) :
toute modification de l'un doit être répercutée sur l'autre, et sur les typages côté client
(`client/src/component/editor/leekwars-dts.ts` pour TS, `leekwars-pyi.ts` pour Pyright).

Ils exposent l'**API de jeu** (`Entity`, `Cell`, `Field`, `Fight`, `Weapon`…), pas la
bibliothèque standard : en JS/TS et en Python, c'est celle du langage hôte qui sert.

### Règle pour `Math`

`Math` ne contient **que ce que le langage hôte n'a pas** — 18 fonctions en JS, 12 en Python.
Le reste passe par le natif (`Math.sqrt` en JS, `math.sqrt` / `round` / `random.randrange` en
Python). On ne double pas la stdlib : `Math` et `math` à une majuscule près serait une
chausse-trappe, et une IA Python doit se comporter comme du Python.

Conséquence assumée et documentée côté client : `round(2.5)` vaut **2** en Python (arrondi
bancaire) contre **3** en LeekScript et en JS. Un test verrouille cet écart.

Pour les utilitaires de bits, l'aller-retour vers l'hôte n'est pas un choix de confort : les
entiers LeekScript font **64 bits** quand les opérateurs bitwise de JS travaillent sur 32.

## Déterminisme

Un combat doit se rejouer à l'identique depuis sa seed. Le RNG est seedé et gelé, l'horloge
murale figée, et côté Python `pythonDeterminismGuard` reroute aussi `os.urandom`,
`SystemRandom` et `uuid4`. `random.randrange()` est donc déterministe et se recommande sans
réserve dans la documentation joueur.

## Points de vigilance

- **Sandbox = sécurité critique** : le code joueur s'exécute dans le même processus.
- **Compteur d'opérations** non falsifiable par le guest (équité entre joueurs).
- **Performance** : la génération des combats est le goulot d'étranglement du jeu.
- **Compatibilité historique** : un changement de comportement casse la relecture des combats
  déjà enregistrés.
