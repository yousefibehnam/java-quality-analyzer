#!/usr/bin/env bash
# Downloads the official source releases of three open-source Java projects from
# Maven Central (versions current in 2017-2018), verifies their SHA-1 checksums, runs
# jqa on them and writes per-class CSV files plus a Markdown summary to results/.
#
# Usage: scripts/run_case_studies.sh
# Requires: curl, unzip, shasum (or sha1sum), a JDK (17+), Maven.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATA="$ROOT/data"
RESULTS="$ROOT/results"
JAR="$ROOT/target/jqa.jar"
CENTRAL="https://repo1.maven.org/maven2"

# name | Maven path (group/artifact/version) | release date | license
PROJECTS=(
  "commons-lang|org/apache/commons/commons-lang3/3.7|2017-11-04|Apache-2.0"
  "commons-io|commons-io/commons-io/2.6|2017-10-15|Apache-2.0"
  "junit4|junit/junit/4.12|2014-12-04|EPL-1.0"
)

sha1() {
  if command -v sha1sum >/dev/null 2>&1; then sha1sum "$1" | cut -d' ' -f1; else shasum -a 1 "$1" | cut -d' ' -f1; fi
}

echo "==> building jqa"
(cd "$ROOT" && mvn -B -q -DskipTests package)

mkdir -p "$DATA" "$RESULTS"
summary_args=()
notes=()
for entry in "${PROJECTS[@]}"; do
  IFS='|' read -r name gav released license <<<"$entry"
  version="${gav##*/}"
  artifact="$(basename "$(dirname "$gav")")"
  file="$artifact-$version-sources.jar"
  url="$CENTRAL/$gav/$file"
  jar="$DATA/$file"

  if [[ ! -f "$jar" ]]; then
    echo "==> downloading $file"
    curl -fsSL -o "$jar" "$url"
  fi
  expected="$(curl -fsSL "$url.sha1" | cut -c1-40)"
  actual="$(sha1 "$jar")"
  if [[ "$expected" != "$actual" ]]; then
    echo "error: SHA-1 mismatch for $file (expected $expected, got $actual)" >&2
    exit 1
  fi

  src="$DATA/$name-$version"
  rm -rf "$src"
  mkdir -p "$src"
  unzip -q "$jar" -d "$src"

  echo "==> analysing $name $version ($license, released $released)"
  java -jar "$JAR" analyze "$src" --format csv --output "$RESULTS/$name.csv" \
    --stats "$RESULTS/$name.stats.properties"
  summary_args+=("$name=$RESULTS/$name.csv")
  notes+=(--note "$name $version: \`$file\` from Maven Central, released $released ($license), SHA-1 \`$actual\`")
done

echo "==> summarising"
java_version="$(java -version 2>&1 | head -n 1)"
java -jar "$JAR" summarize --output "$RESULTS/summary.md" "${notes[@]}" \
  --note "Analysed with jqa on: $java_version (JDK supertypes are measured on this JDK)" \
  "${summary_args[@]}"
echo "done: see $RESULTS/summary.md"
