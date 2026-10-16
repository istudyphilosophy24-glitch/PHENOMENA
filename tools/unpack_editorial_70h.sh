#!/usr/bin/env bash
# Unpack the editorial library ONLY. This script never invokes Gradle or builds an APK.
set -euo pipefail
cd "$(dirname "$0")/.."
ARCHIVE="$(mktemp -t phenomena-70h.XXXXXXXX.tar.xz)"
trap 'rm -f "$ARCHIVE"' EXIT
base64 --decode source/PHENOMENA-70h-Android-Source.tar.xz.base64 > "$ARCHIVE"
echo "89321f6e12d8ddf56fea17a4fa133945f3ce1e5f980695711539674a46c1082a  $ARCHIVE" | sha256sum --check --
tar -xJf "$ARCHIVE" --strip-components=1
python3 tools/validate_catalog.py
python3 tools/validate_seminars.py
python3 tools/validate_70h_catalog.py
echo "Source extracted. No APK build has been started."
