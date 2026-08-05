#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"

mkdir -p dist
rm -f dist/Scheme-Size*.jar

./gradlew clean deploy --no-daemon -PmindustryVersion=v156
cp build/libs/Scheme-Size.jar dist/Scheme-Size.jar

echo
ls -la dist/
unzip -p dist/Scheme-Size.jar mod.hjson | grep -E 'version:|minGameVersion:'
