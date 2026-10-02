#!/usr/bin/env zsh
# ────────────────────────────────────────────────────────────────────────────
# RailFlow launcher — always runs with a Java 21-compatible JVM.
# Usage: ./run.sh
# ────────────────────────────────────────────────────────────────────────────

JAVA21_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"

if [ ! -x "$JAVA21_HOME/bin/java" ]; then
  echo "ERROR: Java 21+ not found at $JAVA21_HOME"
  echo "Install it with:  brew install openjdk"
  exit 1
fi

echo "▶ Launching RailFlow with $($JAVA21_HOME/bin/java -version 2>&1 | head -1)"
JAVA_HOME="$JAVA21_HOME" mvn exec:java -q
