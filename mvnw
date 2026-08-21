#!/bin/sh
set -eu

SCRIPT_DIR=${0%/*}
if [ "$SCRIPT_DIR" = "$0" ]; then
  SCRIPT_DIR=.
fi
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR" && pwd)
PROPS="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"
BOOTSTRAP="$BASE_DIR/.mvn/wrapper/MavenBootstrap.java"

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVA_CMD=java
else
  echo "Error: Java 21 or newer is required to run the Maven wrapper." >&2
  exit 1
fi

CACHE_ROOT=${MAVEN_USER_HOME:-"$HOME/.m2"}/wrapper/dists/ktaf
MAVEN_HOME=$("$JAVA_CMD" "$BOOTSTRAP" "$PROPS" "$CACHE_ROOT")
exec sh "$MAVEN_HOME/bin/mvn" "$@"
