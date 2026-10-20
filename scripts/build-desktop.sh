#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/desktop"
CLASSES_DIR="$BUILD_DIR/classes"
JAR_PATH="$BUILD_DIR/AsteroidGame.jar"

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"

javac --release 17 -d "$CLASSES_DIR" "$ROOT_DIR"/src/desktop/*.java

if [ -d "$ROOT_DIR/src/desktop/assets" ]; then
  cp -R "$ROOT_DIR/src/desktop/assets" "$CLASSES_DIR/assets"
fi

jar --create --file "$JAR_PATH" --main-class DesktopLauncher -C "$CLASSES_DIR" .

echo "Built desktop jar: $JAR_PATH"
