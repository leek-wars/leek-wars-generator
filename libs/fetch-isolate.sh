#!/bin/sh
# Télécharge dans ce dossier l'image isolate GraalVM custom (release publique du dépôt
# leek-wars/leek-wars-graal-isolate) épinglée par isolate-image.txt, « <tag> <sha256> », et
# vérifie son empreinte. Changer d'image = changer cette ligne. Sert à la CI, au Dockerfile et
# en local : sh libs/fetch-isolate.sh
set -eu
dir=$(dirname "$0")
pin="$dir/isolate-image.txt"
jar="$dir/js-isolate-resources-linux-amd64.jar"
bad() { echo "$pin : ligne « <tag> <sha256> » attendue" >&2; exit 1; }
tag='' sha=''
read -r tag sha < "$pin" || true # || : ligne sans retour final
# Le tag entre dans l'URL : ni « / » ni autre caractère qui sortirait des releases de ce dépôt.
case $tag in '' | . | .. | *[!A-Za-z0-9._-]*) bad ;; esac
echo "$sha" | grep -Eq '^[0-9a-f]{64}$' || bad
sha256() { # sha256sum (Linux) ou shasum (macOS)
	if command -v sha256sum > /dev/null; then sha256sum "$1"; else shasum -a 256 "$1"; fi | cut -d' ' -f1
}
if [ -f "$jar" ] && [ "$(sha256 "$jar")" = "$sha" ]; then
	echo "$jar : $tag, déjà là"
	exit 0
fi
trap 'rm -f "$jar.part"' EXIT
url="https://github.com/leek-wars/leek-wars-graal-isolate/releases/download/$tag/js-isolate-resources-linux-amd64.jar"
# Trois essais, quelle que soit l'erreur ; un essai plus lent que ISOLATE_MIN_SPEED o/s pendant
# 30 s est abandonné (bas par défaut pour une connexion lente, la CI peut exiger plus).
n=1
until curl -fsSL --connect-timeout 20 --speed-limit "${ISOLATE_MIN_SPEED:-65536}" --speed-time 30 -o "$jar.part" "$url"; do
	[ "$n" -lt 3 ] || exit 1
	n=$((n + 1))
	sleep 5
done
[ "$(sha256 "$jar.part")" = "$sha" ] || { echo "$tag : sha256 inattendu" >&2; exit 1; }
mv "$jar.part" "$jar"
echo "$jar : $tag"
