# UnitTH - Unit Test History

**Track how your test results evolve over time.**

UnitTH parses JUnit XML and CTRF JSON reports from multiple test runs and generates a single HTML dashboard showing pass rates, trends, and per-test-case history across all runs.

## Why

CI pipelines produce test reports, but each report is a snapshot. When a test starts flaking, or pass rates slowly degrade, you need history. UnitTH fills that gap:

- **Spot regressions early** - see pass rate trends over the last 3/7 days or N runs
- **Find flaky tests** - the verdict spread shows exactly which runs a test failed in
- **Track progress** - watch test count and pass rate improve over time
- **Zero infrastructure** - no database, no server, just a JAR and your XML/JSON reports

## What

UnitTH reads standard JUnit XML (`<testsuite>/<testcase>`) and CTRF JSON from one or more timestamped folders and produces a self-contained HTML report with:

| Feature | Description |
|---------|-------------|
| Dashboard | Overview with pass rate, run count, best/worst run, trends |
| Run history | Per-run breakdown of pass/fail/error/ignored counts |
| Package view | Aggregated stats by Java package |
| Module view | Per-test-class history with charts |
| Test case spread | Color-coded grid showing pass/fail per test per run |
| Trend charts | Chart.js interactive graphs for pass rate, test count, execution time, failures |

The output is a static HTML5 site (responsive, dark mode) that can be opened locally or published to any web server.

## How

### Prerequisites

- Java 21+
- Maven 3.8+ (to build from source)

### Build

```bash
mvn clean package
```

This produces `target/unitth.jar`.

### Run

Point UnitTH at folders containing JUnit XML reports. Each folder represents one test run.

```bash
java -jar unitth.jar ./reports/2026-04-25 ./reports/2026-04-26 ./reports/2026-04-27
```

Or use a glob:

```bash
java -jar unitth.jar ./reports/*
```

The report is written to `report.th/` by default. Open `report.th/index.html` in a browser.

### Jenkins integration

```bash
java -jar unitth.jar -j /path/to/jenkins/jobs/my-job/builds
```

### Configuration

Properties are resolved in this order (first match wins):

1. `-D` flags on the command line
2. `unitth.properties` in the working directory
3. `unitth.properties` in `$HOME` (only read if there is no file in the working directory)
4. Built-in defaults

At most the 100 most recent run folders are included in a report.

| Property | Default | Description |
|----------|---------|-------------|
| `unitth.report.dir` | `report.th` | Output directory for the generated report |
| `unitth.html.report.path` | `.` | Relative path to JUnit HTML reports (for linking) |
| `unitth.xml.report.filter` | `TEST-` | Filename prefix filter for XML files |
| `unitth.json.report.filter` | `TEST_` | Filename prefix filter for JSON files |
| `unitth.report.format` | `json` | File format filter: `xml`, `json`, or `both` |
| `unitth.generate.exectimegraphs` | `false` | Generate execution time graphs |
| `unitth.use.absolute.paths` | `false` | Use absolute paths in report links |
| `unitth.debug` | `false` | Verbose parser output (`-D` flag only) |

Example with `-D` flags:

```bash
java \
  -Dunitth.report.dir=./output \
  -Dunitth.report.format=json \
  -Dunitth.json.report.filter=TEST_ \
  -Dunitth.generate.exectimegraphs=true \
  -jar unitth.jar ./reports/*
```

### Expected input format

Each run folder should contain one or more JUnit XML or CTRF JSON files:

```
reports/
  2026-04-25T110025Z/
    TEST_my-suite.xml
  2026-04-26T131707Z/
    TEST_my-suite.json
  2026-04-27T182826Z/
    TEST_my-suite.xml
    TEST_my-suite.json
```

Folder names are parsed as execution timestamps (e.g. `2026-04-27T182826Z` → `2026-04-27 18:28:26`). If the folder name is not a recognizable timestamp, `file.lastModified()` is used as fallback.

Both XML and JSON files can coexist in the same folder.

**JUnit XML format:**

```xml
<testsuite name="my.TestSuite" tests="3" failures="1" errors="0" skipped="0" time="1.234">
  <testcase name="testOne" classname="my.TestSuite" time="0.5"/>
  <testcase name="testTwo" classname="my.TestSuite" time="0.3">
    <failure message="expected true">assertion failed</failure>
  </testcase>
  <testcase name="testThree" classname="my.TestSuite" time="0.4"/>
</testsuite>
```

**CTRF JSON format:**

```json
{
  "results": {
    "tool": { "name": "sql-unit" },
    "summary": { "tests": 6, "passed": 6, "failed": 0, "pending": 0, "skipped": 0, "other": 0 },
    "tests": [
      { "name": "test_one", "status": "passed", "duration": 500 },
      { "name": "test_two", "status": "failed", "duration": 300, "message": "assertion failed" }
    ]
  }
}
```

In both `--ctrf` and default mode, CTRF JSON files are grouped by `results.environment.projectName` (e.g. `org_repo_SQLValidation` -> package `org_repo`, module `org_repo.SQLValidation`), falling back to the per-test `suite` field.

## Testing

| What | Command | Notes |
|------|---------|-------|
| Unit tests | `mvn test` | JUnit 5 + AssertJ (`src/test/java`) |
| Unit + E2E regression | `mvn verify` | `ReportE2EIT` runs the full pipeline on `src/test/resources/junitsamplereport` (XML, JSON, both, `--ctrf`) |
| Coverage | `mvn verify` | Report only, no gate: `target/site/jacoco/` (unit), `target/site/jacoco-it/` (E2E) |
| Golden snapshots | `mvn verify -Dunitth.updateGolden=true -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false` | Regenerates `src/test/resources/golden/*.txt` after an intended output change (unit tests are skipped because `ConcurrencyTest` also reads the golden files) |
| Manual smoke check | `./runtest.sh` | Shell script, not part of the Maven build |
| Deployment E2E | `sh e2e/deploy-e2e.sh` | Against the running DataOpsBackbone stack: jar checksum, in-container `--ctrf` run, nginx pages, run limit, packages, test-case tables, dates. Env overrides: `RUNNER`, `BASE_URL`, `MAX_RUNS` |

## Project structure

```
src/main/java/unitth/
  core/          Entry point (UnitTH), immutable run configuration (UnitTHConfig), parser base
  junit/         JUnit/CTRF data model and parsers (TestRun, TestModule, TestCase, etc.)
  jenkins/       Jenkins report parser
  html/junit/    HTML5 report generator (charts rendered client-side with Chart.js)
  html/          Shared HTML utilities
src/main/resources/
  css/           Stylesheet
  javascript/    Table sorting script
src/test/java/   Unit tests and ReportE2EIT (in-process E2E regression)
src/test/resources/
  junitsamplereport/  Sample XML and CTRF runs used by the tests
  golden/             Golden metric snapshots for ReportE2EIT
e2e/deploy-e2e.sh    Deployment E2E check against the DataOpsBackbone stack
runtest.sh           Manual smoke check
```

## Origin

Forked from [JUnitTH](http://junitth.sourceforge.net/) by Andreas Nyberg. Modernized with HTML5 output, cleaned codebase, and updated toolchain.

## License

[MIT](LICENSE.txt)
