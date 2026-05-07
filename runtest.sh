#!/bin/sh
set -e

JAR=./target/unitth-3.0.0-SNAPSHOT.jar
SAMPLE=./src/test/resources/junitsamplereport
REPORT=./src/test/resources/unithistreport

echo "=== Building ==="
mvn clean package -q

clean() {
  rm -rf "$REPORT"
}

verify() {
  local label=$1 exp_runs=$2 exp_modules=$3 exp_packages=$4 exp_dates=$5

  RUNS=$(grep -o 'kpi-value accent">[0-9]*' "$REPORT/index.html" | head -1 | grep -o '[0-9]*')
  MODULES=$(ls "$REPORT"/th-module-*.html 2>/dev/null | wc -l | tr -d ' ')
  PACKAGES=$(ls "$REPORT"/th-package-*.html 2>/dev/null | wc -l | tr -d ' ')
  DATES=$(grep -o '[0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}&nbsp;[0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}' "$REPORT/index.html" | sort -u | wc -l | tr -d ' ')

  echo "  Runs:        $RUNS (expected $exp_runs)"
  echo "  Modules:     $MODULES (expected $exp_modules)"
  echo "  Packages:    $PACKAGES (expected $exp_packages)"
  echo "  Unique dates: $DATES (expected $exp_dates)"

  PASS=true
  [ "$RUNS" != "$exp_runs" ] && echo "  FAIL: run count" && PASS=false
  [ "$MODULES" != "$exp_modules" ] && echo "  FAIL: module count" && PASS=false
  [ "$PACKAGES" != "$exp_packages" ] && echo "  FAIL: package count" && PASS=false
  [ "$DATES" != "$exp_dates" ] && echo "  FAIL: unique date count" && PASS=false

  if [ "$PASS" = true ]; then
    echo "  PASSED: $label"
  else
    echo "  FAILED: $label"
    exit 1
  fi
}

echo ""
echo "=== Test 1: XML only (format=xml) ==="
clean
java \
  -Dunitth.report.format=xml \
  -Dunitth.xml.report.filter=TEST_ \
  -Dunitth.generate.exectimegraphs=true \
  -jar "$JAR" "$SAMPLE"/*
verify "XML only" 9 2 1 9

echo ""
echo "=== Test 2: JSON only (format=json) ==="
clean
java \
  -Dunitth.report.format=json \
  -Dunitth.json.report.filter=TEST_ \
  -Dunitth.generate.exectimegraphs=true \
  -jar "$JAR" "$SAMPLE"/*
verify "JSON only" 3 3 1 3

echo ""
echo "=== Test 3: Both (format=both) ==="
clean
java \
  -Dunitth.report.format=both \
  -Dunitth.xml.report.filter=TEST_ \
  -Dunitth.generate.exectimegraphs=true \
  -jar "$JAR" "$SAMPLE"/*
verify "Both XML+JSON" 12 5 1 12

echo ""
echo "=== ALL 3 TESTS PASSED ==="
