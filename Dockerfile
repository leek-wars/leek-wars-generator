# Image du générateur de combats Leek Wars, prête à l'emploi (LeekScript + IA JS/Python).
#
# Le fat jar `generator.jar` embarque déjà la lib native isolate (libpolyglotisolate.so,
# linux-amd64/glibc) : d'où une base glibc (temurin), pas alpine/musl.
#
# Build (standalone, depuis un clone AVEC le sous-module leekscript) :
#   git submodule update --init --recursive
#   docker build -t leek-wars-generator .
# Usage :
#   docker run --rm -v "$PWD":/ai leek-wars-generator --analyze /ai/mon_ia.js
#   docker run --rm -v "$PWD":/ai leek-wars-generator /ai/scenario.json

# ---- build ---------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl unzip \
 && rm -rf /var/lib/apt/lists/*
ARG GRADLE_VERSION=9.2.1
# Gradle 9.x (le wrapper commité est en 8.5, insuffisant pour ce projet).
RUN curl -fsSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip \
 && unzip -q /tmp/gradle.zip -d /opt \
 && ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle \
 && rm /tmp/gradle.zip
WORKDIR /src
# Image isolate (cf libs/fetch-isolate.sh) après Gradle, qui change moins souvent, et avant les
# sources, dont un changement ne la retélécharge pas (.dockerignore exclut libs/*.jar : la copie
# des sources ne l'écrase pas).
COPY libs/fetch-isolate.sh libs/isolate-image.txt libs/
RUN sh libs/fetch-isolate.sh
COPY . .
RUN gradle --no-daemon jar

# ---- runtime -------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app
# generator.jar = fat jar autonome (moteur + isolate). data/ = défauts d'armes/puces/
# invocations lus en relatif depuis le CWD par Generator.init().
COPY --from=build /src/generator.jar ./generator.jar
COPY --from=build /src/data ./data
# --enable-native-access : l'isolate charge une lib native (sinon warning restreint JDK 25).
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "generator.jar"]
