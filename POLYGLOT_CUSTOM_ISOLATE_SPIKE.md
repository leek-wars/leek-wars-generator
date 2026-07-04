# Spike — Custom GraalVM polyglot isolate : réconcilier ops déterministes + limite RAM

> Objectif de ce doc : permettre à une **autre session** (ou une autre personne) de tenter le
> build sans le contexte de la conversation d'origine. Tout ce qu'il faut est ici.

## But en une phrase

Avoir **à la fois** (a) une **limite RAM par-poireau** (par contexte) et (b) un **compteur
d'opérations DÉTERMINISTE** pour les IA polyglot JS/Python — ce qui est aujourd'hui mutuellement
exclusif en GraalVM community.

## État actuel (déjà livré sur beta, generator `private/develop`)

Commits : `f5dd155` (limite RAM par-poireau) + `ab10af4` (ops en temps CPU).

- **RAM par-poireau = FAIT et validé en combat beta** (fight `52437878` : IA gloutonne bornée
  `too_much_ops`, worker survit, voisins OK). Mécanisme : dans `PolyglotSandbox`, **un `Engine`
  `SandboxPolicy.ISOLATED` par langage** (map lazy ; images `js/python-isolate-linux-amd64-community:25.1.3`,
  OSS MIT/UPL) + un cap **`sandbox.MaxHeapMemory` PAR contexte** = `getMaxRAM()` de l'entité.
- **Ops = temps CPU du thread de combat** (`PolyglotEntityAI.getOperations()` → fallback
  `ThreadMXBean.getThreadCpuTime`, helper `markTurnStart()`). **Non déterministe** (accepté pour beta).
- La classe **`StatementCounter`** (instrument Truffle, `com.leekwars.generator.polyglot.StatementCounter`)
  est **conservée dans le code mais MORTE sous isolate** (cf ci-dessous). `PolyglotSandbox.getStatementCounter()`
  renvoie `null`.

## Pourquoi les deux sont incompatibles aujourd'hui

- La **limite RAM** (`sandbox.MaxHeapMemory`, heap RETENU par contexte) **exige le mode isolate**
  (`SandboxPolicy.ISOLATED` + artefacts `-isolate`). En in-process (js-community), pas de cap heap
  par contexte.
- Le **compteur déterministe** = notre instrument Truffle `StatementCounter` (du code Java **hôte**).
  En mode isolate, le guest tourne dans une **image native pré-compilée par Oracle** (`js-isolate-community`)
  qui **ne contient pas notre classe**. Vérifié empiriquement : sous isolate,
  `engine.getInstruments().keySet()` = `[sandbox, debugger]` seulement, pas `lw-statement-counter`.
- Le sandbox built-in compte bien les statements (il applique `MaxStatements`) mais **n'expose AUCUNE
  API pour LIRE le compteur courant** (seulement poser une limite + callback `onLimit`).

## Voie écartée : `ExecutionListener`

`org.graalvm.polyglot.management.ExecutionListener` compte les statements **sans instrument custom**,
et **marche sous isolate + est déterministe** (testé : run1 == run2). MAIS **~42× plus lent**
(4668 ms vs 112 ms pour 2M itérations) : chaque statement déclenche un callback hôte qui traverse
la frontière isolate. **Rédhibitoire** pour une IA de recherche. → écarté.

## L'approche à tenter : custom isolate avec notre instrument compilé dedans

Idée : un instrument compilé **DANS l'image isolate** compte **isolate-side** (zéro traversée par
statement). L'hôte ne lit l'**agrégat** qu'occasionnellement (dans `getOperations()`), pas par statement.
C'est ce qui évite le coût qui tue `ExecutionListener`.

**Hook identifié** (recherche dans le build GraalVM) : la lib isolate est construite par **mx**, et
sa config (`truffle/mx.truffle/mx_truffle.py`) expose **`isolate_deps`** et **`isolate_build_options`**.
`isolate_deps` = les jars mis sur le **classpath native-image** de la lib isolate. Comme un instrument
Truffle est auto-découvert (`@TruffleInstrument.Registration` → `META-INF/services` → closed-world
native-image), **ajouter notre `StatementCounter` à `isolate_deps` devrait le compiler dans l'image**.
Les isolates `-community` étant **open source**, le build est reproductible depuis `oracle/graal`.

### Recette (à valider)

Prérequis : machine avec de l'espace disque (plusieurs Go) et du temps CPU (build long).

1. **Outils** :
   ```
   git clone https://github.com/graalvm/mx.git
   export PATH=$PWD/mx:$PATH
   git clone https://github.com/oracle/graal.git      # la source (GraalJS est dans graal-js ou via suite)
   # GraalJS : repo oracle/graaljs (suite mx 'graal-js'), à cloner à côté de graal/ et mx/
   git clone https://github.com/oracle/graaljs.git
   ```
   Installer une **GraalVM JDK avec native-image** (`native-image --version` doit répondre) et pointer
   `JAVA_HOME` dessus. Absent de la machine d'origine du spike (java local = OpenJDK stock).

2. **Baseline d'abord** : reproduire le build de la lib **js-isolate-community** SANS rien changer,
   pour établir que le build passe sur la machine. Chercher la cible mx du build isolate dans la suite
   graal-js (`mx.graal-js/` ou `mx.js/`), typiquement via `mx build` puis une distribution isolate
   (les distributions qui produisent `js-isolate-linux-amd64-community`). Grep `isolate` dans les
   `suite.py` / `mx_*.py` de graaljs.

3. **Injecter l'instrument** : deux options à essayer —
   - a) Ajouter le jar de notre `StatementCounter` (extrait du generator) aux **`isolate_deps`** de la
     distribution isolate de GraalJS (fork de la config mx).
   - b) Passer un classpath supplémentaire via **`isolate_build_options`** (options native-image du
     build isolate) si `isolate_deps` n'est pas le bon levier.
   L'instrument doit compiler contre `truffle-api` de la MÊME version (25.1.3) et être visible en
   `META-INF/services/...TruffleInstrumentProvider` (le processor `truffle-dsl-processor` le génère).

4. **Rebuild** la lib isolate → produit un `.so`/jar custom pour linux-amd64.

5. **Substituer** dans l'uber-jar worker : remplacer `js-isolate-linux-amd64-community:25.1.3` par
   notre artefact custom (même coordonnées ou dépendance locale). Idem GraalPy si on veut le Python.

6. **Câbler côté generator** : dans `PolyglotSandbox`, re-tenter le lookup
   `engine.getInstruments().get(StatementCounter.ID).lookup(StatementCounter.Counter.class)` (le code
   existe déjà, il renvoyait null). `getStatementCounter()` re-renverrait le compteur ; `getOperations()`
   reprend automatiquement la branche déterministe (elle est toujours là).

### ⚠️ LE RISQUE À VALIDER EN PREMIER (avant tout le reste)

**Est-ce que l'hôte peut LIRE le compteur d'un instrument embarqué dans l'isolate, et à quel coût ?**
`engine.getInstruments().get(ID)` va-t-il lister notre instrument custom sous isolate ? Et
`lookup(Counter.class).get()` marche-t-il à travers la frontière isolate (proxy marshallé ?), à un
coût acceptable (lecture occasionnelle, pas par statement) ? **La doc ne le dit pas.** C'est le
premier truc à tester dès qu'un build custom tourne — inutile d'industrialiser le build si la lecture
ne passe pas. Piste si la lecture directe ne marche pas : exposer le compteur **guest-side** (une
fonction guest qui lit l'instrument dans l'isolate) et le remonter via le bridge.

## Fallbacks si le custom isolate ne passe pas

- **GraalVM EE / Oracle GraalVM** : peut exposer des metering resource-limits non dispo en community
  (à revérifier sur la version courante).
- **Isolation par process** : garder l'in-process (instrument déterministe OK) + borner la RAM par un
  process JVM séparé avec `-Xmx`. Donne les deux, mais RAM **par-process** (per-poireau = 1 process/poireau,
  lourd) + grosse ré-archi IPC.
- **Rester en temps CPU** (l'état actuel) : accepter le non-déterminisme tant que l'arène classée prod
  ne l'exige pas.

## Effort & recommandation

Spike de **plusieurs jours** : build mx de GraalVM (lourd, expertise mx), fork de la config isolate,
maintenance à chaque bump de version GraalVM. **À sortir seulement quand l'arène CLASSÉE en prod
exige la bit-reproductibilité.** Pour beta / non-classé, le **temps CPU actuel suffit**.

## ✅ RÉSULTAT DU SPIKE (2026-07-04) : SUCCÈS

Build dans `/media/hdd/lw-isolate-spike` (graal+graaljs @ `graal-25.1.3`, mx 7.82.2,
labsjdk-ce-25.0.3-jvmci-25.1-b19, `BOOTSTRAP_GRAALVM` = dev-build `25.1.3-dev-20260621_0111`
du repo graalvm-ce-dev-builds). Harnais : `harness/Gate2.java`.

Verdict mesuré (image js custom avec l'instrument embarqué) :
- **Déterminisme : OUI** — run1 == run2 == 600004 statements sur deux contextes frais.
- **Overhead comptage : ~+3%** sur compute guest pur (1721 vs 1669 µs/eval) — contre +4200%
  pour l'ExecutionListener. Le pari du comptage isolate-side est gagné.
- **Coût lecture hôte : ~3 µs** par lecture (occasionnelle, dans getOperations()).

Les 4 découvertes qui font marcher le montage :
1. **Le lookup de service NE traverse PAS la frontière** (`lookup(Counter.class)` = null,
   confirmé — c'était LE risque). Canal de lecture = **polyglot bindings** : l'instrument
   publie un exécutable `lwStatementCounter` (execute()=lire, execute(x)=reset) dans chaque
   contexte (ContextsListener + context.enter), l'hôte lit via
   `context.getPolyglotBindings().getMember(..).execute()` — API standard, bridgée.
2. **L'instrument est lazy** et son activateur historique (le lookup) est cassé sous isolate →
   activation par **option Truffle** (`@Option` vide → `.option("lw-statement-counter", "true")`
   sur l'engine, comme cpusampler).
3. **ThreadLocal est blocklisté** en compilation runtime native-image → le compteur devient un
   simple champ `long` (correct : 1 engine = 1 combat = 1 thread ; et plus rapide, l'incrément
   se PE-compile). À reporter dans le StatementCounter du generator.
4. Le provider DSL exige `--initialize-at-build-time=com.leekwars.generator.polyglot`
   (ajouté aux isolate_build_options).

Recette d'injection : projet mx `com.leekwars.truffle.instrument` + distribution module
`LW_INSTRUMENT` dans la suite graal-js, ajoutée à `additional_image_path_artifacts` de
`register_polyglot_isolate_distributions` (patch mx_graal_js.py, gaté `LW_ISOLATE_INSTRUMENT=1`).
Build : `POLYGLOT_ISOLATES=js mx -p graaljs/graal-js --dy /substratevm build
--dependencies graal-js:JS_ISOLATE_RESOURCES_LINUX_AMD64` (~5 min d'image sur 24 cœurs).

Reste avant prod : nom de binding secret par combat OU builtin Polyglot js désactivé (sinon un
joueur peut `Polyglot.import` et reset le compteur = triche) ; rebuild python-isolate équivalent ;
bench Quantum ia-js sous l'image custom ; câblage PolyglotSandbox (option d'activation + lecture
bindings dans getOperations) ; process de rebuild à chaque bump GraalVM.

## Références

**Code (generator, branche `polyglot-ram-limit` = `private/develop`)** :
- `src/main/java/com/leekwars/generator/polyglot/PolyglotSandbox.java` (engines ISOLATED par langage,
  `createContext(..., maxHeapBytes)` avec `sandbox.MaxHeapMemory`).
- `src/main/java/com/leekwars/generator/polyglot/PolyglotEntityAI.java` (`getOperations()`,
  `markTurnStart()`, temps CPU).
- `src/main/java/com/leekwars/generator/polyglot/StatementCounter.java` (l'instrument à embarquer).
- `build.gradle` (deps `js/python-isolate-linux-amd64-community:25.1.3`).
- Test : `src/test/java/test/TestPolyglotRamLimit.java`.

**PoCs de la session d'origine** (dans `/tmp/lw-ram-poc`, éphémères — recréables) :
- `ExecListenerTest.java` : ExecutionListener sous isolate (déterministe mais 42× lent).
- `PerContextHeap.java` / `ProdConfig.java` : cap RAM par contexte sur isolate partagé (marche).
- `Gate3.java` : bridge + FileSystem sous `SandboxPolicy.ISOLATED`.

**Sources GraalVM** :
- Embedding Languages : https://www.graalvm.org/latest/reference-manual/embed-languages/
- Sandboxing / resource limits : https://www.graalvm.org/latest/security-guide/sandboxing/
- mx build Truffle (hook isolate) : https://github.com/oracle/graal/blob/master/truffle/mx.truffle/mx_truffle.py
- Truffle README (mx) : https://github.com/oracle/graal/blob/master/truffle/README.md
- SDK CHANGELOG : https://github.com/oracle/graal/blob/master/sdk/CHANGELOG.md
