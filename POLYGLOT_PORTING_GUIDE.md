# Guide de portage LeekScript → JavaScript / Python (runtime polyglot)

Retour d'expérience du portage 1:1 de l'IA **Quantum** (186 fichiers LeekScript v4,
~29k lignes) vers JS (`ia-js`) et Python (`ia-py`), exécutés par le runtime GraalVM
Polyglot du generator (branche `feature/polyglot-ai`, **privée**).

Les conventions **statiques** (syntaxe, structure de classe, types, opérateurs) sont dans
le `TRANSLATION.md` de chaque repo. **Ce guide-ci couvre le RUNTIME** : les divergences
de sémantique qui ne se révèlent qu'à l'exécution, et l'intégration moteur. Ce sont elles
qui font qu'un port « qui compile » ne joue pas — ou plante.

---

## 0. LE piège n°1 : le compteur d'opérations (sinon l'IA ne joue jamais)

**Symptôme** : l'IA charge sans erreur mais n'agit pas. Les logs montrent N tours de
`hard start` puis plus rien, **zéro erreur de code**, `getOperations()` figé à une petite
valeur (~80) alors que `getMaxOperations()` vaut des millions. Après 3 tours, l'IA est
neutralisée.

**Cause** : en LeekScript natif, **chaque statement incrémente le compteur d'ops**. Une IA
de recherche (alpha-bêta, exploration de combos) borne son exploration avec une garde
`getOperations() > seuil`. Sous le polyglot, le calcul **pur guest** (boucles JS/Python)
n'incrémente PAS `mOperations` : seul le travail hôte des fonctions de combat appelle
`ai.ops(...)`. La garde de l'IA ne se déclenche donc jamais → la recherche explore tout
l'arbre → le backstop wall-clock (5 s) coupe le tour → après `MAX_WALL_CLOCK_TIMEOUTS=3`,
l'IA est `disabled`.

**Correctif** (generator, `PolyglotEntityAI.getOperations()`) : surfacer un terme d'ops
**synthétique proportionnel au temps écoulé dans le tour**, calibré pour atteindre
`getMaxOperations()` à `OPS_TIME_BUDGET_FRACTION` (0.5) du budget wall-clock. La garde de
l'IA se déclenche alors naturellement, bien avant le backstop dur, et l'IA rend sa meilleure
action. L'enforcement réel du budget (`ops()` → `TOO_MUCH_OPERATIONS`) reste sur
`mOperations` brut ; le terme synthétique ne sert qu'à la LECTURE par le guest.

**Conséquence déterminisme** : comme le backstop wall-clock, ce terme dépend de la charge
machine → une IA de recherche polyglot n'est pas bit-reproductible entre machines (le
vainqueur d'un duel miroir peut changer d'un run à l'autre). Acceptable pour faire
tourner/terminer des combats ; à raffiner (compteur de **statements guest** déterministe)
pour des combats classés.

> Toute IA portée qui s'auto-limite via `getOperations()` dépend de ce correctif. C'est la
> première chose à vérifier si « ça compile mais ça ne joue pas ».

---

## 1. Modèle d'exécution

- 1 fichier `.leek` → 1 module `.js` (module ES) ou `.py` (chemin relatif identique ;
  **Python** : tirets interdits dans les noms de module → `foo-bar.leek` devient
  `foo_bar.py`, et il faut des `__init__.py`).
- L'API de jeu (`getCell`, `useWeapon`, `useChip`, `lineOfSight`, `debug`…) et les
  constantes (`CHIP_*`, `WEAPON_*`, `EFFECT_*`, `COLOR_*`, `BOSS_*`…) sont **injectées dans
  le scope global** → appel direct, sans import.
- Entrée : le fichier racine définit `turn()` (JS : `globalThis.turn = …` ; Python :
  `def turn()`). **Le source est évalué UNE fois** (les `static`/globals persistent tout le
  combat), puis `turn()` est rejouée chaque tour. `beforeFight()` est appelée avant le combat
  si définie.
- La stdlib LeekScript se scinde en deux :
  - **Bridgée** par le runtime (`PolyglotAPIBridge` installe `FightFunctions` +
    `LeekFunctions.getStandardFunctions()`) : `round/min/max/abs`, `getColor`, etc.
  - **NON bridgée** (opère sur les conteneurs natifs) : tout ce qui manipule
    tableaux/maps/sets — voir §3.

---

## 2. Divergences de sémantique (la source de 90 % des bugs runtime)

LeekScript est **permissif** ; il ne lève quasi jamais. JS l'est en partie (renvoie
`undefined`/`NaN`) ; **Python lève**. D'où la règle : *ce qui « marchait » silencieusement en
LeekScript doit être reproduit explicitement, surtout en Python.*

| Cas | LeekScript | JavaScript | Python | Correctif |
|---|---|---|---|---|
| **`null` dans une somme** (`score += f()` où `f` tombe sans return) | `null` → 0 | `+= undefined` → `NaN` (silencieux !) | `TypeError: int + None` | `score += (f() or 0)` ; ou faire renvoyer 0 |
| **Accès tableau hors bornes** | `null` | `undefined` → `NaN` en arithmétique | `IndexError` | helper borné renvoyant 0/None (ex. `shield_factor`) |
| **`map[clé absente]`** | `null` | `undefined` | `KeyError` | `.get(k)` ; ou un `dict` tolérant (`LSObject`) |
| **Supprimer une clé absente** | no-op | `delete` ok | `KeyError` | `mapRemove`/`.pop(k, None)`, **jamais** `del map[k]` |
| **Division entière `\`** | opérateur dédié | `Math.trunc(a/b)` | `int(a/b)` (PAS `//` si négatif possible) | — |
| **`/` par 0** | `null`/0, ne lève pas | `Infinity`/`NaN` | `ZeroDivisionError` | garder l'opérande |
| **`for v in map`** | itère les **valeurs** | `for…of map` itère `[k,v]` | itère les **clés** | Python : `for v in lw_values(map)` |
| **`for (k:v in coll)`** | clé+valeur | `coll.entries()` (Map) / `[i,v] of arr.entries()` (Array) | `.items()` / `enumerate` | distinguer Map vs Array |
| **Littéraux string adjacents dans `[…]`** | éléments **séparés** | **concaténés** en 1 élément | **concaténés** en 1 élément | rétablir les virgules (piège PI_DIGITS : 2000 lignes → 1) |
| **Callback avec args en trop** | ignorés | ignorés | `TypeError` | tronquer à `co_argcount` (`_apply`) |
| **clé = objet avec `equals`/`hashCode`** | égalité **par valeur** | identité de référence | hashable requis | JS : sérialiser la clé ; Python : `__eq__`+`__hash__` ou `tuple(sorted(...))` |
| **`x | 0`** (troncature) sur un float | int | int | float inchangé | Python : `int(x)` |
| **Bitmask > 32 bits** | entier 64-bit natif | **`BigInt` obligatoire** (`1n << BigInt(n)`) ; mélanger BigInt+Number **lève** | `int` (précision arbitraire, OK) | JS : propager le BigInt à TOUS les points de calcul (hash, comparaisons, masques) |
| **Retour implicite** | `null` | `undefined` | `None` | identique, sauf si comparé ensuite |
| **`"" + n`** | coerce | coerce | exige `str(n)` / f-string | Python uniquement |

---

## 3. Stdlib collection (non bridgée)

Les fonctions qui opèrent sur tableaux/maps/sets **natifs** ne sont pas bridgées (le runtime
n'a pas de `ArrayClass`/`MapClass` guest). Les réimplémenter dans un prélude importé **en
premier** : `count, push, pop, insert, arrayMap/Filter/FoldLeft/Iter/Some/Every, arraySort,
removeElement, sum, average, reverse, shuffle, inArray, join, arraySlice, arrayGet,
arrayFrequencies, arrayRandom, clone (shallow, préserve les BigInt), set* (Set),
map* (mapGet/Put/PutAll/Remove/Filter/First/Fold/Keys/Values/Size/…)`.

- **JS** : `globalThis.<fn> = …` dans `lw-prelude.js`.
- **Python** : `setattr(builtins, "<fn>", …)` dans `lw_prelude.py` (= namespace global). Y
  ajouter `LSObject` (dict tolérant : `.attr` ET `[clé absente]` → None) et `lw_values`
  (itère les valeurs d'un dict). Vérifier la **couverture complète** : un seul `mapFilter`
  manquant casse un tour entier (`NameError`).

> Callbacks : signatures calquées sur LeekScript `(valeur, index/clé, conteneur)`. Le prélude
> doit tronquer les args en trop (`co_argcount` en Python) car LeekScript/JS les ignorent.

---

## 4. Système de modules

### JavaScript (modules ES)
- Les **cycles d'imports sont tolérés** (live bindings) — SAUF la TDZ : une classe utilisée
  avant sa définition dans un cycle `extends` lève `ReferenceError`. Pattern : sortir la
  population de `Class.all` / `Class.factories` dans un **module registre**
  (`feature-registry.js`, `boss-registry.js`) importé depuis l'entrée APRÈS la hiérarchie.
- Les classes/fonctions référencées **sans import** (namespace global LeekScript) :
  s'auto-enregistrer `globalThis.X = X` en fin de fichier.
- `global X` LeekScript réassigné en bare → `globalThis.X` (un `export let` réassigné dans un
  module strict lève).

### Python (le plus dur)
- Les **cycles d'imports lèvent**. Stratégie « builtins = namespace global » :
  1. ne garder QUE les imports d'**héritage** (`class Sub(Base)`, acycliques) ; **stripper**
     tous les autres imports locaux (les marquer `# [lw-strip]`) → casse le cluster
     circulaire (86 modules pour Quantum) ;
  2. chaque module enregistre ses classes/fonctions/globals dans `builtins` → les références
     bare résolvent (équivalent `globalThis`) ;
  3. l'entrée importe la **clôture transitive des includes** (pas tous les fichiers : sinon
     les benchmarks/générateurs s'exécutent à l'import) via une boucle **point-fixe** qui
     réessaie les `NameError` d'ordre.
- **Globals mutables PARTAGÉS entre modules** (le piège architectural) : LeekScript `global`
  est une instance unique mutée par un module et lue par un autre. En Python, `global` est
  **par module** → désynchronisation. Solution : un **objet namespace partagé** (`RG`)
  enregistré sur `builtins`, et **réécrire tous les accès** `V1` → `RG.V1` (lecture ET
  écriture passent par le même objet, plus besoin de `global`). Exclure les homonymes qui
  sont en réalité des **paramètres** (ex. `closure_binary_map(V0..V17)`).
- **Walrus `:=` ne peut PAS cibler un attribut** (`RG.X := …` est une erreur de syntaxe). Si
  le port utilise `:=` sur un nom devenu `RG.X`, le dé-walruser en instructions, ou garder ce
  nom comme local simple s'il n'est pas partagé cross-module.
- `def turn()` en collision avec un nom de méthode existant → renommer (ex. `Lambda_ai`).

---

## 5. Cycle de vie / init

- `TURN = getTurn()` au top-level est **figé** (évalué 1× à l'import, vs chaque tour en
  LeekScript). Le rafraîchir dans une init différée appelée au début de `turn()`.
- Cycle `util ↔ quantum` (le top-level de `util` déclenche les `init()`) : déplacer ce
  top-level dans une fonction `utilInit()`/`util_init()` appelée au 1er tour.
- Déterminisme : router `Math.random()` (JS) vers le RNG du combat, sinon replays non
  reproductibles. `arrayRandom`/`randInt` cosmétiques tolérés en non-déterministe.

---

## 6. Côté generator (intégration)

- `detectLanguage` : accepter `.js`/`.mjs`/`.py` (et **insensible à la casse** — serveur et
  client normalisent en minuscules).
- `PolyglotAPIBridge` : installer `FightFunctions` + `LeekFunctions.getStandardFunctions()`
  (package `leekscript.runner.classes.`, 1er param = l'AI) + `LeekConstants.values()`
  (skipper `Infinity`/`NaN` déjà globals JS). **Overloads par type, même arité** (`abs(long)`
  vs `abs(double)`) : préférer `double`, pénaliser `BigIntegerValue`, sinon `round/min/max`
  tronquent les reals.
- Données bundlées parfois stale : tolérer `max_uses` absent (**défaut `-1` = illimité**,
  PAS `0` qui rend toute arme/puce inutilisable via `maxUses != -1 && uses >= maxUses`) et
  `los` en int.
- Maps de jeu marshallées en **ProxyObject** (pas Map JS) → `Object.keys`. Tableaux de jeu =
  ProxyArray (itérable, a `.map`).

---

## 7. Symptôme → cause → fix (mémo)

| Symptôme observé | Cause probable | Fix |
|---|---|---|
| Charge sans erreur, **n'agit pas**, ops figées, disabled après 3 tours | compteur d'ops guest non incrémenté | §0 `getOperations` synthétique |
| `NameError: V1` (Python) | global mutable cross-module | §4 namespace `RG` |
| `NameError: mapFilter` (Python) | helper collection manquant | §3 compléter le prélude |
| `TypeError: int + None` | `null` LeekScript dans une somme | `… or 0` |
| `IndexError` sur une table de scoring | accès hors bornes (LS→null) | accès borné → 0 |
| `KeyError` sur `del map[k]` | suppression de clé absente | `mapRemove`/`.pop(k, None)` |
| Liste de 1 élément géant au lieu de N | littéraux string adjacents concaténés | rétablir les virgules |
| `Cannot mix BigInt and other types` (JS) | masque 64-bit + Number | tout passer en BigInt |
| `ReferenceError: X is not defined` (JS) | cycle ES / classe non enregistrée | registre + `globalThis.X = X` |

---

## 8. Checklist pour un transformeur automatique

1. Renommer fichiers/dossiers (Python : tirets → underscores, `__init__.py`).
2. Classes : `export`/`globalThis.X = X` (JS) ; `setattr(builtins, …)` (Python).
3. Imports : garder l'héritage, stripper le reste (Python) ; registres pour les cycles
   `extends` (JS).
4. `\` → `Math.trunc`/`int(a/b)` ; `x | 0` float → `int(x)` ; `/0` gardé.
5. `for v in map` → valeurs (Python `lw_values`) ; `for k:v` → `.entries()`/`.items()`.
6. Sommes avec retour possible `null` → `or 0`.
7. Accès tableau/map potentiellement OOB → borné/`.get`.
8. `del map[k]` → `mapRemove`.
9. Littéraux string adjacents dans un array → virgules.
10. Masques > 32 bits (JS) → BigInt sur toute la chaîne.
11. Globals mutables partagés → namespace `RG` (Python).
12. Prélude collections complet + `LSObject` + `lw_values`.
13. Init différée (TURN, cycles util↔quantum).
14. **Vérifier en combat réel** (compile ≠ joue) : déplacements, attaques, dégâts létaux,
    fin de combat.
