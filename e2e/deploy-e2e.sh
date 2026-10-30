#!/bin/sh
# Deployment E2E regression for UnitTH inside the DataOpsBackbone stack.
# Run from the UnitTH repo root after deploying target/unitth.jar:
#   ./e2e/deploy-e2e.sh
# Env overrides: RUNNER, BASE_URL, MAX_RUNS, JAR, CTRF_DIR, REPORT_DIR

RUNNER=${RUNNER:-github-runner-org-zbrainiac-labs-1}
BASE_URL=${BASE_URL:-http://localhost:8080}
MAX_RUNS=${MAX_RUNS:-100}
JAR=${JAR:-./target/unitth.jar}
CTRF_DIR=${CTRF_DIR:-/home/docker/sql-unit-reports}
REPORT_DIR=${REPORT_DIR:-/home/docker/sql-report-vol}

FAILS=0
WARNS=0
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

pass() { echo "  PASS: $1"; }
fail() { echo "  FAIL: $1"; FAILS=$((FAILS + 1)); }
warn() { echo "  WARN: $1"; WARNS=$((WARNS + 1)); }
fetch() { curl -sf "$BASE_URL/$1" -o "$TMP/$(basename "$1")"; }

# Counts <tr> rows in the <tbody> of the table with the given id.
table_rows() {
  awk -v id="id=\"$2\"" '
    index($0, id) { intable = 1 }
    intable && /<tbody>/ { inbody = 1 }
    inbody && /<tr/ { n++ }
    inbody && /<\/tbody>/ { print n + 0; exit }
    END { if (!inbody) print 0 }' "$1"
}

echo "=== 1. Jar identity ==="
LOCAL_SUM=$(shasum "$JAR" | cut -d' ' -f1)
REMOTE_SUM=$(docker exec "$RUNNER" sha1sum /usr/local/bin/unitth.jar 2>/dev/null | cut -d' ' -f1)
if [ -n "$REMOTE_SUM" ] && [ "$LOCAL_SUM" = "$REMOTE_SUM" ]; then
  pass "container jar matches $JAR ($LOCAL_SUM)"
else
  fail "container jar '$REMOTE_SUM' != local '$LOCAL_SUM'"
fi

echo "=== 2. Generate report in container ==="
OUT=$(docker exec "$RUNNER" bash -c "rm -rf $REPORT_DIR/* && cd /usr/local/bin && java -Dunitth.report.dir=$REPORT_DIR -jar unitth.jar --ctrf $CTRF_DIR/*" 2>&1)
RC=$?
if [ $RC -eq 0 ] && echo "$OUT" | grep -q "report has been generated"; then
  pass "unitth --ctrf generated a report"
else
  fail "generation failed (rc=$RC)"; echo "$OUT" | tail -5
fi

echo "=== 3. Served by nginx ==="
for p in index.html css/style.css js/tablesort.js; do
  if fetch "$p"; then pass "$p"; else fail "$p not served"; fi
done

echo "=== 4. Run limit ==="
INPUT_RUNS=$(docker exec "$RUNNER" sh -c "ls -d $CTRF_DIR/*/ 2>/dev/null | wc -l" | tr -d ' ')
# UnitTH takes the last MAX_RUNS folders, then skips those without CTRF files.
EXPECTED_RUNS=$(docker exec "$RUNNER" sh -c "for d in \$(ls -d $CTRF_DIR/*/ | tail -n $MAX_RUNS); do ls \"\$d\" | grep -q '^TEST_.*\.json\$' && echo x; done | wc -l" | tr -d ' ')
RUNS=$(grep -o 'kpi-value accent">[0-9]*' "$TMP/index.html" | head -1 | grep -o '[0-9]*$')
if [ "$RUNS" = "$EXPECTED_RUNS" ] && [ "$RUNS" -le "$MAX_RUNS" ]; then
  pass "runs=$RUNS (non-empty among last $MAX_RUNS of $INPUT_RUNS folders)"
else
  fail "runs=$RUNS, expected $EXPECTED_RUNS (non-empty among last $MAX_RUNS of $INPUT_RUNS folders)"
fi

echo "=== 5. Packages ==="
PKGS=$(grep -o 'th-package-[^"]*\.html' "$TMP/index.html" | sort -u)
NPKG=$(echo "$PKGS" | grep -c . )
# Expected: distinct environment.projectName among the runs actually reported (last MAX_RUNS folders).
EXPECTED_PKGS=$(docker exec "$RUNNER" sh -c "ls -d $CTRF_DIR/*/ | tail -n $MAX_RUNS | while read d; do cat \"\$d\"TEST_*.json 2>/dev/null; done" \
  | grep -o '"projectName"[^,}]*' | sed 's/.*: *"//; s/"$//; s/_[^_]*$//' | sort -u | grep -c .)
if [ "$NPKG" -gt 1 ]; then pass "$NPKG packages"; else fail "only $NPKG package(s)"; fi
if [ "$NPKG" = "$EXPECTED_PKGS" ]; then
  pass "package count matches distinct projectName values ($EXPECTED_PKGS)"
else
  fail "package count $NPKG != distinct projectName values $EXPECTED_PKGS"
fi
if echo "$PKGS" | grep -q "th-package-not-in-package.html"; then
  fail "found not-in-package"
else
  pass "no not-in-package"
fi

echo "=== 6. Package pages ==="
for p in $PKGS; do
  if ! fetch "$p"; then fail "$p not served"; continue; fi
  TC=$(table_rows "$TMP/$p" testcase_table)
  SP=$(table_rows "$TMP/$p" spread_table)
  if [ "$TC" -gt 0 ] && [ "$SP" -gt 0 ]; then
    pass "$p testcases=$TC spread=$SP"
  else
    fail "$p testcases=$TC spread=$SP"
  fi
done

echo "=== 7. Module pages ==="
MODS=$(grep -oh 'th-module-[^"]*\.html' "$TMP"/th-package-*.html 2>/dev/null | sort -u)
for m in $MODS; do
  if ! fetch "$m"; then fail "$m not served"; continue; fi
  TC=$(table_rows "$TMP/$m" testcase_table)
  if [ "$TC" -gt 0 ]; then pass "$m testcases=$TC"; else fail "$m has no test cases"; fi
done

echo "=== 8. Latest run date ==="
NEWEST=$(docker exec "$RUNNER" sh -c "ls -d $CTRF_DIR/*/ | tail -1 | xargs basename")
# 2026-05-10T061234Z -> 2026-05-10&nbsp;06:12:34
WANT=$(echo "$NEWEST" | sed -E 's/^([0-9]{4}-[0-9]{2}-[0-9]{2})T([0-9]{2})([0-9]{2})([0-9]{2})Z$/\1\&nbsp;\2:\3:\4/')
if grep -q "$WANT" "$TMP/index.html"; then
  pass "newest folder $NEWEST shown"
else
  fail "newest folder $NEWEST ($WANT) not found in index"
fi

echo "=== 9. Broken values ==="
if grep -l '>null<' "$TMP"/*.html >/dev/null 2>&1; then
  fail "literal null in: $(grep -l '>null<' "$TMP"/*.html | xargs -n1 basename | tr '\n' ' ')"
else
  pass "no null cells"
fi
NAN=$(grep -l 'NaN' "$TMP"/*.html 2>/dev/null | xargs -n1 basename 2>/dev/null | tr '\n' ' ')
if [ -n "$NAN" ]; then fail "NaN in: $NAN"; else pass "no NaN"; fi

echo ""
echo "=== $FAILS failure(s), $WARNS warning(s) ==="
[ $FAILS -eq 0 ]
