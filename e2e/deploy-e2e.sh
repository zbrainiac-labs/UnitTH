#!/bin/sh
# Deployment E2E regression for UnitTH inside the DataOpsBackbone stack.
# The pipeline ingests each run into one store per project (STORE_DIR) and
# serves the report built from all stores; expectations come from the stores.
# Run from the UnitTH repo root after deploying target/unitth.jar:
#   ./e2e/deploy-e2e.sh
# Env overrides: RUNNER, BASE_URL, MAX_RUNS, JAR, CTRF_DIR, REPORT_DIR, STORE_DIR,
#   SAVE_BASELINE (write metrics to file), BASELINE (diff metrics against file),
#   SELF_REPORT (UnitTH self-history report dir)

RUNNER=${RUNNER:-github-runner-org-zbrainiac-labs-1}
BASE_URL=${BASE_URL:-http://localhost:8080}
MAX_RUNS=${MAX_RUNS:-100}
JAR=${JAR:-./target/unitth.jar}
CTRF_DIR=${CTRF_DIR:-/home/docker/sql-unit-reports}
REPORT_DIR=${REPORT_DIR:-/home/docker/sql-report-vol}
STORE_DIR=${STORE_DIR:-/home/docker/unitth-store}

FAILS=0
WARNS=0
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

pass() { echo "  PASS: $1"; }
fail() { echo "  FAIL: $1"; FAILS=$((FAILS + 1)); }
warn() { echo "  WARN: $1"; WARNS=$((WARNS + 1)); }
fetch() { curl -sf "$BASE_URL/$1" -o "$TMP/$(basename "$1")"; }
# Runs a shell command in the runner container.
in_runner() { docker exec "$RUNNER" sh -c "$1"; }

# Counts <tr> rows in the <tbody> of the table with the given id.
table_rows() {
  awk -v id="id=\"$2\"" '
    index($0, id) { intable = 1 }
    intable && /<tbody>/ { inbody = 1 }
    inbody && /<tr/ { n++ }
    inbody && /<\/tbody>/ { print n + 0; exit }
    END { if (!inbody) print 0 }' "$1"
}

# Writes runs, page list and per-page table row counts of the fetched report.
metrics() {
  echo "runs=$(grep -o 'kpi-value accent">[0-9]*' "$TMP/index.html" | head -1 | grep -o '[0-9]*$')"
  for p in "$TMP"/th-*.html; do
    [ -e "$p" ] || continue
    f=$(basename "$p")
    echo "$f testcases=$(table_rows "$TMP/$f" testcase_table) spread=$(table_rows "$TMP/$f" spread_table)"
  done
}

# Fetches index plus all package and module pages into $TMP.
fetch_all() {
  rm -f "$TMP"/*.html
  fetch index.html || return 1
  for p in $(grep -o 'th-package-[^"]*\.html' "$TMP/index.html" | sort -u); do fetch "$p"; done
  for m in $(grep -oh 'th-module-[^"]*\.html' "$TMP"/th-package-*.html 2>/dev/null | sort -u); do fetch "$m"; done
}

echo "=== 1. Jar identity ==="
LOCAL_SUM=$(shasum "$JAR" | cut -d' ' -f1)
REMOTE_SUM=$(docker exec "$RUNNER" sha1sum /usr/local/bin/unitth.jar 2>/dev/null | cut -d' ' -f1)
if [ -n "$REMOTE_SUM" ] && [ "$LOCAL_SUM" = "$REMOTE_SUM" ]; then
  pass "container jar matches $JAR ($LOCAL_SUM)"
else
  fail "container jar '$REMOTE_SUM' != local '$LOCAL_SUM'"
fi

echo "=== 2. Regenerate report from the run stores ==="
# The report served now was written by the last pipeline run; regenerating
# it from the stores alone must give the same pages.
if fetch_all; then metrics > "$TMP/served.txt"; else : > "$TMP/served.txt"; fi
OUT=$(docker exec "$RUNNER" bash -c "rm -rf $REPORT_DIR/* && cd /usr/local/bin && java -Dunitth.report.dir=$REPORT_DIR -jar unitth.jar --store-dir $STORE_DIR" 2>&1)
RC=$?
if [ $RC -eq 0 ] && echo "$OUT" | grep -q "report has been generated"; then
  pass "unitth --store-dir generated a report"
else
  fail "generation failed (rc=$RC)"; echo "$OUT" | tail -5
fi
fetch_all; metrics > "$TMP/regen.txt"
if [ ! -s "$TMP/served.txt" ]; then
  warn "no report was served before regeneration"
elif diff "$TMP/served.txt" "$TMP/regen.txt" > /dev/null; then
  pass "regenerated report equals the served pipeline report"
else
  fail "regenerated report differs from the served one:"; diff "$TMP/served.txt" "$TMP/regen.txt" | head -10
fi

echo "=== 3. Served by nginx ==="
for p in index.html css/style.css js/tablesort.js; do
  if fetch "$p"; then pass "$p"; else fail "$p not served"; fi
done

echo "=== 4. Runs ==="
# runs per store file: "<runs> <file>" (run objects carry "id":"sha256:...)
in_runner "for f in $STORE_DIR/*.json; do head -c 25 \"\$f\" | grep -q unitth-store && echo \"\$(grep -o '\"id\":\"sha256:' \"\$f\" | wc -l) \$(basename \"\$f\")\"; done" > "$TMP/stores.txt"
NSTORES=$(grep -c . "$TMP/stores.txt")
EXPECTED_RUNS=$(awk '{s += $1} END {print s + 0}' "$TMP/stores.txt")
RUNS=$(grep -o 'kpi-value accent">[0-9]*' "$TMP/index.html" | head -1 | grep -o '[0-9]*$')
if [ "$RUNS" = "$EXPECTED_RUNS" ]; then
  pass "runs=$RUNS (sum over $NSTORES store(s))"
else
  fail "runs=$RUNS, expected $EXPECTED_RUNS (sum over $NSTORES store(s))"
fi
OVER=$(awk -v m="$MAX_RUNS" '$1 > m {print $2}' "$TMP/stores.txt" | tr '\n' ' ')
if [ -z "$OVER" ]; then pass "every store holds <= $MAX_RUNS runs"; else fail "stores over $MAX_RUNS runs: $OVER"; fi

echo "=== 5. Packages ==="
PKGS=$(grep -o 'th-package-[^"]*\.html' "$TMP/index.html" | sort -u)
NPKG=$(echo "$PKGS" | grep -c . )
# Expected: one package per project store that holds runs.
EXPECTED_PKGS=$(awk '$1 > 0' "$TMP/stores.txt" | grep -c .)
if [ "$NPKG" -gt 1 ]; then pass "$NPKG packages"; else fail "only $NPKG package(s)"; fi
if [ "$NPKG" = "$EXPECTED_PKGS" ]; then
  pass "package count matches project stores ($EXPECTED_PKGS)"
else
  fail "package count $NPKG != project stores $EXPECTED_PKGS"
fi
if echo "$PKGS" | grep -q "th-package-not-in-package.html"; then
  fail "found not-in-package"
else
  pass "no not-in-package"
fi

echo "=== 5b. Projects ==="
# One <details class="project"> per package; their run rows add up to all runs
# (each stored run belongs to exactly one project).
NPROJ=$(grep -c '<details class="project">' "$TMP/index.html")
PROJ_RUNS=$(awk '
  /<details class="project">/ { inproj = 1; header = 1 }
  inproj && /<tr>/ { if (header) header = 0; else n++ }
  /<\/details>/ { inproj = 0 }
  END { print n + 0 }' "$TMP/index.html")
if [ "$NPROJ" = "$NPKG" ]; then pass "$NPROJ projects"; else fail "$NPROJ projects, expected $NPKG"; fi
if [ "$PROJ_RUNS" = "$RUNS" ]; then pass "project run rows=$PROJ_RUNS"; else fail "project run rows=$PROJ_RUNS, expected $RUNS"; fi
if grep -q 'href="#modules"' "$TMP/index.html"; then
  pass "Modules section shown (some package has several modules)"
else
  pass "Modules section hidden (one module per package)"
fi
# UnitTH's own history (local-ci/record-history.sh) relies on "project = name before the last dot"
SELF=zbrainiac-labs_UnitTH
if grep -q "^[0-9]* $SELF.json\$" "$TMP/stores.txt"; then
  if grep -q "th-package-$SELF.html" "$TMP/index.html"; then pass "project $SELF shown"; else fail "store $SELF.json exists but project $SELF is missing"; fi
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
# newest stored run date, stored as shown in the report (2026-11-01&nbsp;12:06:45)
NEWEST=$(in_runner "cat $STORE_DIR/*.json" | grep -o '"date":"[0-9][^"]*"' | cut -d'"' -f4 | sort | tail -1)
WANT="$NEWEST"
if [ -n "$NEWEST" ] && grep -q "$WANT" "$TMP/index.html"; then
  pass "newest stored run $NEWEST shown"
else
  fail "newest stored run '$NEWEST' not found in index"
fi

echo "=== 9. Broken values ==="
if grep -l '>null<' "$TMP"/*.html >/dev/null 2>&1; then
  fail "literal null in: $(grep -l '>null<' "$TMP"/*.html | xargs -n1 basename | tr '\n' ' ')"
else
  pass "no null cells"
fi
# NaN as a value (cell, percentage, chart data), not inside test names like ...ForNaN
NAN=$(grep -lE '>NaN|NaN%|[[,]NaN[],]' "$TMP"/*.html 2>/dev/null | xargs -n1 basename 2>/dev/null | tr '\n' ' ')
if [ -n "$NAN" ]; then fail "NaN in: $NAN"; else pass "no NaN"; fi

echo "=== 10. Metrics ==="
metrics > "$TMP/metrics.txt"
echo "  $(grep -c . "$TMP/metrics.txt") metric line(s)"
if [ -n "$SAVE_BASELINE" ]; then
  cp "$TMP/metrics.txt" "$SAVE_BASELINE" && pass "baseline saved to $SAVE_BASELINE"
fi
if [ -n "$BASELINE" ]; then
  if diff "$BASELINE" "$TMP/metrics.txt" > "$TMP/metrics.diff"; then
    pass "metrics equal to baseline $BASELINE"
  else
    fail "metrics differ from baseline:"; head -20 "$TMP/metrics.diff"
  fi
fi

echo "=== 11. Store hygiene ==="
LOCKS=$(in_runner "ls $STORE_DIR" | grep '\.lock' | tr '\n' ' ')
if [ -z "$LOCKS" ]; then pass "no stale lock file"; else fail "lock file(s) left: $LOCKS"; fi
# Run folders are deleted once stored; any folder still holding a report was not ingested.
LEFT=$(in_runner "for d in $CTRF_DIR/*/; do ls \"\$d\" 2>/dev/null | grep -q '^TEST_.*\.json\$' && basename \"\$d\"; done" | tr '\n' ' ')
if [ -z "$LEFT" ]; then pass "no unstored run folder in $CTRF_DIR"; else warn "run folder(s) not yet stored: $LEFT"; fi
FOREIGN=$(in_runner "ls $STORE_DIR" | grep -v '\.json$' | tr '\n' ' ')
if [ -z "$FOREIGN" ]; then pass "store dir holds only stores"; else warn "other files in $STORE_DIR: $FOREIGN"; fi
if in_runner "grep -rlF '\"format\":\"unitth-store\"' $REPORT_DIR" > /dev/null 2>&1; then
  fail "store file found in served report dir"
else
  pass "store file not in served report dir"
fi

if [ -n "$SELF_REPORT" ]; then
  echo "=== 12. Self-history ==="
  if [ -f "$SELF_REPORT/index.html" ]; then
    SR=$(grep -o 'kpi-value accent">[0-9]*' "$SELF_REPORT/index.html" | head -1 | grep -o '[0-9]*$')
    if [ "${SR:-0}" -ge 1 ]; then pass "self-history runs=$SR"; else warn "self-history has no runs"; fi
  else
    warn "no self-history report at $SELF_REPORT"
  fi
fi

echo ""
echo "=== $FAILS failure(s), $WARNS warning(s) ==="
[ $FAILS -eq 0 ]
