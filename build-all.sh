#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"

mkdir -p dist
rm -f dist/Scheme-Size-v*.jar

build_one() {
  local ver="$1"
  local build="$2"
  echo "=== Building $build ($ver) ==="
  ./gradlew clean deploy --no-daemon \
    -PmindustryVersion="$ver" \
    -PgameBuild="$build"
  cp "build/libs/Scheme-Size-v${build}.jar" "dist/Scheme-Size-v${build}.jar"
  echo "=== OK: dist/Scheme-Size-v${build}.jar ==="
}

build_one v156 156
build_one v158.1 158
build_one v159 159

mkdir -p build/dist
cp dist/*.jar build/dist/

echo
ls -la dist/
