#!/bin/bash
# Prepares cloud sessions: JDK 25 (the build's pinned toolchain), npm deps, Gradle warm-up.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

cd "${CLAUDE_PROJECT_DIR:-$(pwd)}"

JDK_DIR=/opt/jdk25
if [ ! -x "$JDK_DIR/bin/java" ]; then
  mkdir -p "$JDK_DIR"
  curl -fsSL https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz \
    | tar xz --strip-components=1 -C "$JDK_DIR"
fi

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo "export JAVA_HOME=$JDK_DIR"
    echo "export PATH=$JDK_DIR/bin:\$PATH"
    echo "export GITHUB_ACTOR=\${GITHUB_ACTOR:-x-access-token}"
  } >> "$CLAUDE_ENV_FILE"
fi

# Let Gradle find the JDK 25 toolchain without downloading one.
mkdir -p ~/.gradle
touch ~/.gradle/gradle.properties
grep -q 'java.installations.paths' ~/.gradle/gradle.properties \
  || echo "org.gradle.java.installations.paths=$JDK_DIR" >> ~/.gradle/gradle.properties

(cd site && npm ci --no-audit --no-fund) || echo "npm ci failed in site" >&2
# map's deck.gl packages have mismatched peer ranges (core 9.3.2 vs geo-layers 9.4.0), hence the flag.
(cd map && npm ci --legacy-peer-deps --no-audit --no-fund) || echo "npm ci failed in map" >&2

# Warm the Gradle cache. Needs the Maven hosts allowed in the environment's network policy.
GITHUB_ACTOR="${GITHUB_ACTOR:-x-access-token}" JAVA_HOME=$JDK_DIR \
  ./gradlew --no-daemon -q help >/dev/null 2>&1 \
  || echo "Gradle warm-up failed; check the environment network allowlist for the Maven repos." >&2
exit 0
