#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$ROOT"

BUILD="$ROOT/build"
CLASSES="$BUILD/classes"
STAGE="$BUILD/jar"
OUTPUT="$BUILD/RenderScale-Legacy-1.7.10-1.0.1.jar"
CLASSPATH="$ROOT/deps/forge-universal.jar:$ROOT/deps/launchwrapper.jar:$ROOT/deps/asm-all.jar:$ROOT/deps/lwjgl.jar"

rm -rf "$BUILD"
mkdir -p "$CLASSES" "$STAGE/META-INF"

SOURCES=$(find src/main/java -name '*.java' -print | sort)
if [ -z "$SOURCES" ]; then
    echo "No Java sources found." >&2
    exit 1
fi

java -jar "$ROOT/deps/ecj.jar" \
    -1.7 \
    -proc:none \
    -d "$CLASSES" \
    -classpath "$CLASSPATH" \
    $SOURCES

cp -R "$CLASSES/." "$STAGE/"
cp src/main/resources/mcmod.info "$STAGE/mcmod.info"
cp META-INF/MANIFEST.MF "$STAGE/META-INF/MANIFEST.MF"
cp LICENSE "$STAGE/LICENSE"
cp README.md "$STAGE/README.md"

(cd "$STAGE" && zip -X -q -r "$OUTPUT" .)
unzip -t "$OUTPUT" >/dev/null

echo "Built: $OUTPUT"
