#!/bin/bash
# -----------------------------------------------------------------------------
# Record this build's test results (surefire + failsafe JUnit XML) as one run
# of the project in the DataOpsBackbone run stores and rebuild the shared report
# (http://localhost:8080), the same way the DataOps pipeline does for SQL repos.
#
# UnitTH takes the project (package) from everything before the last dot of the
# suite / class name, so the copied reports are renamed
#   unitth.core.UnitTHConfigTest -> <project>.UnitTHConfigTest
# giving one project with the test classes as its modules.
#
# Usage: bash local-ci/record-history.sh
# Env:   UNITTH_PROJECT (default: GITHUB_REPOSITORY with '/' -> '_', else zbrainiac-labs_UnitTH)
#        STORE_DIR (default /home/docker/unitth-store), REPORT_DIR (default
#        /home/docker/sql-report-vol), JAR (default /usr/local/bin/unitth.jar)
# Runs on a DataOpsBackbone runner (CI sonar job, act). Never fails the build:
# problems are reported as warnings, exit code 0.
# -----------------------------------------------------------------------------
set -Eeuo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_SLUG="${GITHUB_REPOSITORY:-zbrainiac-labs/UnitTH}"
PROJECT="${UNITTH_PROJECT:-${REPO_SLUG//\//_}}"
STORE_DIR="${STORE_DIR:-/home/docker/unitth-store}"
REPORT_DIR="${REPORT_DIR:-/home/docker/sql-report-vol}"
JAR="${JAR:-/usr/local/bin/unitth.jar}"

warn() { echo "WARN: $1 -- test history not recorded"; exit 0; }

[[ "$PROJECT" =~ ^[A-Za-z0-9_-]+$ ]] || warn "invalid project name '$PROJECT'"
[[ -d "$STORE_DIR" && -w "$STORE_DIR" ]] || warn "store dir $STORE_DIR missing or not writable"
[[ -f "$JAR" ]] || warn "$JAR not found"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
# one build = one run, dated by the folder name
RUN_DIR="$TMP/$(date -u +%Y-%m-%dT%H%M%SZ)"
mkdir -p "$RUN_DIR"

for f in "$REPO"/target/surefire-reports/TEST-*.xml "$REPO"/target/failsafe-reports/TEST-*.xml; do
  [[ -f "$f" ]] || continue
  cls=$(basename "$f" .xml)
  cls="${cls#TEST-}"
  short="${cls##*.}"
  out="$RUN_DIR/TEST-$PROJECT.$short.xml"
  [[ -e "$out" ]] && warn "test class name $short is not unique"
  # suite name and every test case class name -> <project>.<SimpleClassName>
  re="${cls//./\\.}"
  sed -e "s/name=\"$re\"/name=\"$PROJECT.$short\"/" \
      -e "s/classname=\"$re\"/classname=\"$PROJECT.$short\"/g" "$f" > "$out"
done
[[ -n "$(ls "$RUN_DIR")" ]] || warn "no TEST-*.xml in target/surefire-reports or target/failsafe-reports"
echo "Recording $(ls "$RUN_DIR" | wc -l | tr -d ' ') test suite(s) as one run of $PROJECT"

# -D flags win over the runner's unitth.properties (CTRF/json settings of the SQL pipeline)
LOG="$TMP/unitth.log"
if ! java -Dunitth.report.dir="$REPORT_DIR" -Dunitth.report.format=xml -Dunitth.xml.report.filter=TEST- \
    -jar "$JAR" --store "$STORE_DIR/$PROJECT.json" --store-dir "$STORE_DIR" "$RUN_DIR" > "$LOG" 2>&1; then
  tail -5 "$LOG"
  warn "unitth failed"
fi
grep -E "found=|ingest=|report has been generated" "$LOG" || true
echo "Report: http://localhost:8080/th-package-$PROJECT.html"
