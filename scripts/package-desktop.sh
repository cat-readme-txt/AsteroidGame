#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/desktop"
DIST_DIR="$ROOT_DIR/dist"
APP_NAME="${APP_NAME:-VoidNavigator}"
APP_VERSION="${APP_VERSION:-1.0.0}"
PACKAGE_TYPE="${PACKAGE_TYPE:-app-image}"

"$ROOT_DIR/scripts/build-desktop.sh"

mkdir -p "$DIST_DIR"

jpackage \
  --input "$BUILD_DIR" \
  --main-jar "AsteroidGame.jar" \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --type "$PACKAGE_TYPE" \
  --dest "$DIST_DIR"

echo "Packaged desktop app in: $DIST_DIR"
