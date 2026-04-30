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

This produces `target/unitth-3.0.0-SNAPSHOT.jar`.

### Run

Point UnitTH at folders containing JUnit XML reports. Each folder represents one test run.

```bash
java -jar unitth-3.0.0-SNAPSHOT.jar ./reports/2026-04-25 ./reports/2026-04-26 ./reports/2026-04-27
```

Or use a glob:

```bash
java -jar unitth-3.0.0-SNAPSHOT.jar ./reports/*
```

The report is written to `report.th/` by default. Open `report.th/index.html` in a browser.

### Jenkins integration

```bash
java -jar unitth-3.0.0-SNAPSHOT.jar -j /path/to/jenkins/jobs/my-job/builds
```

### Configuration

Properties are resolved in this order (later overrides earlier):

1. `unitth.properties` in `$HOME`
2. `unitth.properties` in working directory
3. `-D` flags on the command line

| Property | Default | Description |
|----------|---------|-------------|
| `unitth.report.dir` | `report.th` | Output directory for the generated report |
| `unitth.html.report.path` | `.` | Relative path to JUnit HTML reports (for linking) |
| `unitth.xml.report.filter` | `TEST-` | Filename prefix filter for XML files |
| `unitth.json.report.filter` | `TEST_` | Filename prefix filter for JSON files |
| `unitth.report.format` | `json` | File format filter: `xml`, `json`, or `both` |
| `unitth.generate.exectimegraphs` | `false` | Generate execution time graphs |
| `unitth.use.absolute.paths` | `false` | Use absolute paths in report links |

Example with `-D` flags:

```bash
java \
  -Dunitth.report.dir=./output \
  -Dunitth.report.format=json \
  -Dunitth.json.report.filter=TEST_ \
  -Dunitth.generate.exectimegraphs=true \
  -jar unitth-3.0.0-SNAPSHOT.jar ./reports/*
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

## Project structure

```
src/main/java/unitth/
  core/          Main entry point, parsing orchestration, properties
  junit/         JUnit/CTRF data model and parsers (TestRun, TestModule, TestCase, etc.)
  jenkins/       Jenkins report parser
  graphics/      PNG chart generation (pass rate, test count, exec time)
  html/junit/    HTML5 report generator
  html/          Shared HTML utilities
src/main/resources/
  css/           Stylesheet
  javascript/    Table sorting script
  images/        Trend icons, logos
```

## Origin

Forked from [JUnitTH](http://junitth.sourceforge.net/) by Andreas Nyberg. Modernized with HTML5 output, cleaned codebase, and updated toolchain.

## License

[MIT](LICENSE.txt)

## Repository

[github.com/zbrainiac-labs/UnitTH](https://github.com/zbrainiac-labs/UnitTH)
