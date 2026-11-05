#!/usr/bin/env bash
# Clones three open-source Java projects at pinned release tags, runs jqa on their
# production sources and writes per-class CSV files plus a Markdown summary to results/.
#
# Usage: scripts/run_case_studies.sh
# Requires: git, a JDK (17+), Maven.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATA="$ROOT/data"
RESULTS="$ROOT/results"
JAR="$ROOT/target/jqa.jar"

# name | repository | tag | license | source directory inside the repository
PROJECTS=(
  "commons-lang|https://github.com/apache/commons-lang.git|rel/commons-lang-3.21.0|Apache-2.0|src/main/java"
  "commons-io|https://github.com/apache/commons-io.git|rel/commons-io-2.22.0|Apache-2.0|src/main/java"
  "junit4|https://github.com/junit-team/junit4.git|r4.13.2|EPL-1.0|src/main/java"
)

echo "==> building jqa"
(cd "$ROOT" && mvn -B -q -DskipTests package)

mkdir -p "$DATA" "$RESULTS"
summary_args=()
notes=()
for entry in "${PROJECTS[@]}"; do
  IFS='|' read -r name url tag license srcdir <<<"$entry"
  checkout="$DATA/$name"
  if [[ ! -d "$checkout/.git" ]]; then
    echo "==> cloning $name at $tag"
    git clone --quiet --depth 1 --branch "$tag" "$url" "$checkout"
  fi
  actual="$(git -C "$checkout" describe --tags --exact-match 2>/dev/null || echo unknown)"
  if [[ "$actual" != "$tag" ]]; then
    echo "error: $checkout is at '$actual', expected '$tag' (delete it and re-run)" >&2
    exit 1
  fi
  echo "==> analysing $name ($tag, $license)"
  java -jar "$JAR" analyze "$checkout/$srcdir" --format csv --output "$RESULTS/$name.csv" \
    --stats "$RESULTS/$name.stats.properties"
  summary_args+=("$name=$RESULTS/$name.csv")
  notes+=(--note "$name: \`$tag\` of $url ($license), directory \`$srcdir\`")
done

echo "==> summarising"
java_version="$(java -version 2>&1 | head -n 1)"
java -jar "$JAR" summarize --output "$RESULTS/summary.md" "${notes[@]}" \
  --note "Analysed with jqa on: $java_version (JDK supertypes are measured on this JDK)" \
  "${summary_args[@]}"
echo "done: see $RESULTS/summary.md"
