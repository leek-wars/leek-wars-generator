# Leek Wars Generator

[![CI](https://github.com/leek-wars/leek-wars-generator/actions/workflows/build.yml/badge.svg)](https://github.com/leek-wars/leek-wars-generator/actions/workflows/build.yml)

Leek Wars fight generator using [leekscript](https://github.com/leek-wars/leekscript) language.

## Requirements
- Java 25 (OpenJDK or Amazon Corretto)
- Gradle 9.x

## Build
```
gradle jar
```

## Test
```
gradle test
```

## AI analysis task
```
java -jar generator.jar --analyze test/ai/basic.leek
```
![Fight generation task](https://github.com/leek-wars/leek-wars-generator-v1/blob/master/doc/compilation_task.svg)

## Fight generation task
```
java -jar generator.jar test/scenario/scenario1.json
```

![Fight generation task](https://github.com/leek-wars/leek-wars-generator-v1/blob/master/doc/fight_task.svg)

## Tester une IA en local

Deux façons, selon le langage de l'IA.

### Le plus simple : Docker (aucun build)

L'image publique embarque le moteur, l'isolate et les données — rien à compiler :

```sh
# IA LeekScript, JavaScript OU Python : monte le dossier courant sur /ai
docker run --rm -v "$PWD":/ai ghcr.io/leek-wars/leek-wars-generator --analyze /ai/mon_ia.js
docker run --rm -v "$PWD":/ai ghcr.io/leek-wars/leek-wars-generator /ai/scenario.json
```

### En Java, sans Docker

- **IA LeekScript** : rien de plus que le jar. `gradle jar` puis :

  ```sh
  java -jar generator.jar --analyze test/ai/basic.leek
  java -jar generator.jar test/scenario/scenario1.json
  ```

- **IA JavaScript / Python (polyglot)** : il faut en plus l'**image isolate** (lib native
  GraalVM, linux-amd64). Elle n'est pas commitée (127 Mo) — récupère-la depuis la release
  publique dans `libs/` avant `gradle jar` :

  ```sh
  mkdir -p libs
  curl -fsSL -o libs/js-isolate-resources-linux-amd64.jar \
    https://github.com/leek-wars/leek-wars-graal-isolate/releases/download/v25.1.3-combined-2/js-isolate-resources-linux-amd64.jar
  gradle jar
  java -jar generator.jar mon_scenario.json      # les IA .js / .py sont détectées par extension
  ```

  L'image isolate se rebuild aussi soi-même (build lourd) : voir
  [leek-wars-graal-isolate](https://github.com/leek-wars/leek-wars-graal-isolate).

### Format de scénario

Un combat local est décrit par un JSON (fermiers, équipes, entités avec leur IA/arme/puces,
carte, seed…). Voir `test/scenario/scenario1.json` pour un exemple 2v2 complet ; le champ
`ai` de chaque entité pointe un fichier `.leek`, `.js` ou `.py`.

## Credits
Developed by Dawyde & Pilow © 2012-2026
