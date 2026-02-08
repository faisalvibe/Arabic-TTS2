#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if [ ! -x "./gradlew" ]; then
  echo "Missing ./gradlew in $ROOT_DIR"
  echo "Run: gradle wrapper"
  exit 1
fi

./gradlew assembleDebug

echo "APK: $ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
