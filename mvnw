#!/bin/sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROPS="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

read_property() {
  sed -n "s/^$1=//p" "$PROPS" | head -n 1
}

DIST_URL=$(read_property distributionUrl)
DIST_SHA512=$(read_property distributionSha512Sum)
MAVEN_VERSION=$(read_property mavenVersion)
CACHE_ROOT=${MAVEN_USER_HOME:-"$HOME/.m2"}/wrapper/dists/ktaf
MAVEN_HOME="$CACHE_ROOT/apache-maven-$MAVEN_VERSION"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$CACHE_ROOT"
  ARCHIVE="$CACHE_ROOT/apache-maven-$MAVEN_VERSION-bin.zip"
  TMP="$ARCHIVE.tmp.$$"
  trap 'rm -f "$TMP"' EXIT INT TERM

  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --silent --show-error "$DIST_URL" --output "$TMP"
  elif command -v wget >/dev/null 2>&1; then
    wget --quiet "$DIST_URL" --output-document="$TMP"
  else
    echo "Error: curl or wget is required to download Maven." >&2
    exit 1
  fi

  if command -v sha512sum >/dev/null 2>&1; then
    ACTUAL_SHA512=$(sha512sum "$TMP" | awk '{print $1}')
  elif command -v shasum >/dev/null 2>&1; then
    ACTUAL_SHA512=$(shasum -a 512 "$TMP" | awk '{print $1}')
  else
    echo "Error: sha512sum or shasum is required to verify Maven." >&2
    exit 1
  fi
  if [ "$ACTUAL_SHA512" != "$DIST_SHA512" ]; then
    echo "Error: Maven distribution checksum mismatch." >&2
    exit 1
  fi

  rm -rf "$MAVEN_HOME"
  unzip -q "$TMP" -d "$CACHE_ROOT"
  mv "$TMP" "$ARCHIVE"
  trap - EXIT INT TERM
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
