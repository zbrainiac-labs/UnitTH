#!/bin/sh
# Records UnitTH's own test results (mvn verify) in a run store and renders
# the self-history report. Run from the repo root after `mvn verify`:
#   ./e2e/self-history.sh
# Env overrides: UNITTH_SELF_STORE, SELF_REPORT, JAR
# Never fails the build: problems are reported as warnings, exit code is 0.

STORE=${UNITTH_SELF_STORE:-$HOME/.unitth/unitth-self-store.json}
SELF_REPORT=${SELF_REPORT:-target/unitth-self-report}
JAR=${JAR:-target/unitth.jar}

if [ ! -f "$JAR" ]; then
  echo "WARN: $JAR not found, run mvn verify first"; exit 0
fi
# Surefire and failsafe write separate folders; one build is one run, dated by the folder name.
RUN_DIR=$(mktemp -d)/$(date -u +%Y-%m-%dT%H%M%SZ)
mkdir -p "$RUN_DIR"
trap 'rm -rf "$(dirname "$RUN_DIR")"' EXIT
for d in target/surefire-reports target/failsafe-reports; do
  [ -d "$d" ] && cp "$d"/TEST-*.xml "$RUN_DIR"/ 2>/dev/null
done
if [ -z "$(ls "$RUN_DIR")" ]; then
  echo "WARN: no TEST-*.xml in target/surefire-reports or target/failsafe-reports"; exit 0
fi

mkdir -p "$(dirname "$STORE")"
if java -Dunitth.report.dir="$SELF_REPORT" -Dunitth.report.format=xml -Dunitth.xml.report.filter=TEST- \
     -Dunitth.generate.exectimegraphs=true -jar "$JAR" --store "$STORE" "$RUN_DIR" > "$RUN_DIR.log" 2>&1; then
  grep "Run store" "$RUN_DIR.log"
  echo "Self-history report: $SELF_REPORT/index.html"
else
  echo "WARN: self-history generation failed:"; tail -5 "$RUN_DIR.log"
fi
rm -f "$RUN_DIR.log"
exit 0
