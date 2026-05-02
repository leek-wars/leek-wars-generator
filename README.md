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

## Credits
Developed by Dawyde & Pilow © 2012-2026
