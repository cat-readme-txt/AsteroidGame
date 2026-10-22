#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/desktop"
DIST_DIR="$ROOT_DIR/dist"
APP_NAME="${APP_NAME:-VoidNavigator}"
PACKAGE_VERSION="0"
JPACKAGE_VERSION="1"
ZIP_NAME="${APP_NAME}-macOS-$(uname -m)-${PACKAGE_VERSION}.zip"

"$ROOT_DIR/scripts/build-desktop.sh"

mkdir -p "$DIST_DIR"
STAGING_DIR="$(mktemp -d "$DIST_DIR/.package.XXXXXX")"
trap 'rm -rf "$STAGING_DIR"' EXIT

jpackage \
  --input "$BUILD_DIR" \
  --main-jar "AsteroidGame.jar" \
  --name "$APP_NAME" \
  --app-version "$JPACKAGE_VERSION" \
  --type app-image \
  --dest "$STAGING_DIR"

APP_PATH="$STAGING_DIR/$APP_NAME.app"
plutil -replace CFBundleShortVersionString -string "$PACKAGE_VERSION" "$APP_PATH/Contents/Info.plist"
plutil -replace CFBundleVersion -string "$PACKAGE_VERSION" "$APP_PATH/Contents/Info.plist"
sed -i '' "s/jpackage.app-version=$JPACKAGE_VERSION/jpackage.app-version=$PACKAGE_VERSION/" \
  "$APP_PATH/Contents/app/$APP_NAME.cfg"
codesign --force --deep --sign - "$APP_PATH"
codesign --verify --deep --strict "$APP_PATH"
DOWNLOAD_DIR="$STAGING_DIR/$APP_NAME-macOS-$(uname -m)-$PACKAGE_VERSION"
mkdir -p "$DOWNLOAD_DIR"
ditto "$APP_PATH" "$DOWNLOAD_DIR/$APP_NAME.app"
cp "$ROOT_DIR/README.md" "$ROOT_DIR/GAME_MECHANICS.md" "$ROOT_DIR/STORY.md" "$DOWNLOAD_DIR/"
cp "$ROOT_DIR/scripts/Uninstall-VoidNavigator.sh" "$DOWNLOAD_DIR/Uninstall-VoidNavigator.command"
chmod +x "$DOWNLOAD_DIR/Uninstall-VoidNavigator.command"
ditto -c -k --sequesterRsrc --keepParent "$DOWNLOAD_DIR" "$STAGING_DIR/$ZIP_NAME"

if [[ -e "$DIST_DIR/$APP_NAME.app" ]]; then
  mv "$DIST_DIR/$APP_NAME.app" "$STAGING_DIR/previous.app"
fi
mv "$APP_PATH" "$DIST_DIR/$APP_NAME.app"
mv "$STAGING_DIR/$ZIP_NAME" "$DIST_DIR/$ZIP_NAME"

find "$DIST_DIR" -maxdepth 1 -type f -name "$APP_NAME-macOS-*.zip" \
  ! -name "$ZIP_NAME" -delete
find "$DIST_DIR" -maxdepth 1 -type d -name "$APP_NAME-[0-9]*.app" \
  -exec rm -rf {} +

echo "Packaged desktop version 0: $DIST_DIR/$APP_NAME.app and $DIST_DIR/$ZIP_NAME"
