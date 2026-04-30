# UnitTH - Open Points

## Open

| # | Issue | Prio | Effort | Status |
|---|-------|------|--------|--------|
| 7 | **Zero unit tests** — `src/test/java/` is completely empty | **P2** | L | OPEN |
| 8 | **Static mutable state** — `UnitTH.java` uses `public static` fields for all config (`rootFolder`, `reportPath`, etc.), not thread-safe | **P2** | L | OPEN |
| 22 | **File DB layer** — store parsed test results in a JSON/SQLite file to enable incremental ingestion, faster re-generation, and retention policies without keeping raw XMLs | **P3** | L | OPEN |
| 26 | **Generate text/PDF summary** — produce a plain-text or PDF report suitable for email notifications from CI | **P3** | L | OPEN |

Effort: **S** = < 30 min, **M** = 1-2 hours, **L** = half day+

## Done

| # | Issue | Status |
|---|-------|--------|
| 1 | **tablesort.js uses ES6 `export`** — removed `export` keywords, works as plain `<script>` | DONE |
| 2 | **GPL headers in all 43 Java files** — replaced with MIT header in all files | DONE |
| 3 | **Tests permanently skipped** — removed `maven.test.skip`, surefire `skip`, `testFailureIgnore`, and stale profiles | DONE |
| 4 | **Dead FitNesse code** — removed 15 files (fitnesse/, html/fitnesse/, FileCheckingParser), cleaned UnitTH.java | DONE |
| 5 | **Unused image resources** — removed 10 unused image constants and extraction from `extractFilesFromJar()` | DONE |
| 6 | **Stale test reference report** — removed `src/test/resources/unithistreport/` (29 old frameset HTML files) | DONE |
| 9 | **`System.exit(0)` in non-main methods** — all removed during FitNesse cleanup; Jenkins handler now uses `System.err` | DONE |
| 10 | **Old hardcoded references** — removed with FitNesse code; UnitTHException cleaned | DONE |
| 11 | **Copyright 2010 + GPL warranty text in help/usage strings** — updated to MIT in UnitTH.java | DONE |
| 12 | **Duplicate `META-INF/MANIFEST.MF`** — removed redundant manifest from `src/main/java/` | DONE |
| 13 | **Old `@author andnyb` tags** — removed all 16 occurrences from 14 files | DONE |
| 14 | **Jenkins parser cleanup** — removed commented-out debug code | DONE |
| 15 | **FIXME/TODO/HACK comments** — previously cleaned up | DONE |
| 16 | **SVN `$Id$` tags** — previously removed from all files | DONE |
| 17 | **HTML5 modernization** — framesets, HTML4, `&nbsp;`, TABLE layout all replaced | DONE |
| 18 | **Stale files cleanup** — Archive.zip, src.zip, out/, temp/, .DS_Store, duplicates removed | DONE |
| 19 | **Version mismatch** — fixed `versionNumber` to "2.2" | DONE |
| 20 | **Sample test reports** — replaced old 2014 XMLs with valid sql-unit-reports | DONE |
| 21 | **Dead `getTrendImage()` in HtmlGenUtils** — removed unused method referencing deleted image constants | DONE |
| 27 | **Dashboard with Chart.js** — added KPI strip, 2-column chart grid (Pass Rates, Test Case Count, Execution Times, Non-Passing) using Chart.js v4.4.7 CDN | DONE |
| 28 | **Table row clipping** — CSS `overflow: hidden` on `<table>` + `position: sticky` on `<thead>` hid last rows/sections; removed both, all 9 runs / 2 modules / 1 package now visible | DONE |
| 23 | **Trend calculation index bug** — fixed off-by-one: trend baseline used spread index equal to item count instead of highest index | DONE |
| 24 | **Pass rate threshold line in graphs** — configurable "ok" pass rate line drawn in pass rate chart | DONE |
| 25 | **Graph axis scaling** — charts now scale to data range instead of starting at 0,0 | DONE |
| 29 | **Last Run KPI overflow** — date+time overflowed card at full font size; split onto two lines with `<br>` between date and time, narrowed first 3 KPI cards (`0.7fr`) to give more space to Avg Pass Rate (`1.2fr`) and Last Run (`1.7fr`) | DONE |
| 30 | **Exec date bug** — runs showed report-generation time (`file.lastModified()`) instead of actual execution time; now parses folder name (e.g. `2026-04-27T182826Z`) as execution date with `lastModified` as fallback | DONE |
| 31 | **JSON/CTRF support in default parser** — `JUnitReportParser` now handles `.json` files inline (CTRF format) alongside JUnit XML; no need to switch parser mode | DONE |
| 32 | **Mixed XML+JSON file filter** — `UnitTH.createJUnitXMLFilter()` accepts both `.xml` and `.json` in default mode, enabling folders with either or both formats | DONE |
| 33 | **JSON-based test samples** — added 3 JSON-only run folders from DataOpsBackbone to `src/test/resources/junitsamplereport/` for E2E regression coverage | DONE |
