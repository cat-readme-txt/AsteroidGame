#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="$ROOT_DIR/dist"
ARCHIVE="$DIST_DIR/VoidNavigator-Java17-portable-0.zip"

"$ROOT_DIR/scripts/build-desktop.sh"
mkdir -p "$DIST_DIR"
STAGING_DIR="$(mktemp -d "$DIST_DIR/.portable.XXXXXX")"
trap 'rm -rf "$STAGING_DIR"' EXIT

PACKAGE_DIR="$STAGING_DIR/VoidNavigator"
mkdir -p "$PACKAGE_DIR"
cp "$ROOT_DIR/build/desktop/AsteroidGame.jar" "$PACKAGE_DIR/"
tr -d '\r' < "$ROOT_DIR/launchers/Play-VoidNavigator.bat" \
  | awk '{printf "%s\r\n", $0}' > "$PACKAGE_DIR/Play-VoidNavigator.bat"
cp "$ROOT_DIR/README.md" "$ROOT_DIR/GAME_MECHANICS.md" "$ROOT_DIR/STORY.md" "$PACKAGE_DIR/"

(cd "$STAGING_DIR" && zip -q -r "$ARCHIVE" VoidNavigator)
echo "Packaged portable Java 17 download: $ARCHIVE"
