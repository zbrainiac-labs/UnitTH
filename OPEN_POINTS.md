# UnitTH - Open Points

## Open

| # | Issue | Prio | Effort | Status |
|---|-------|------|--------|--------|
| 26 | **Generate text/PDF summary** — produce a plain-text or PDF report suitable for email notifications from CI | **P3** | L | OPEN |
| 48 | **Inconsistent project names from DataOps (upstream)** — the same repo arrives as `zBrainiac_mother-of-all-Projects` and `zbrainiac-labs_mother-of-all-Projects`, and runs without metadata as `UnknownOwner_UnknownRepo` / `zbrainiac-labs_UnknownRepo`; fix `GITHUB_OWNER`/`GITHUB_REPO` in the DataOpsBackbone pipeline (`sql-validate.sh`), UnitTH keeps names as delivered | **P2** | S | OPEN |
| 51 | **`e2e/self-history.sh` redundant** — UnitTH's history is now recorded by CI into the shared report (#49); the local self-history store (`~/.unitth/unitth-self-store.json`) and the `SELF_REPORT` check in `deploy-e2e.sh` can be removed | **P3** | S | OPEN |

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
| 7 | **Zero unit tests** — JUnit 5 + AssertJ suite (config, model, parsers, file collection, concurrency), `ReportE2EIT` via Failsafe with golden snapshots, JaCoCo report-only coverage, `e2e/deploy-e2e.sh` for the deployed stack | DONE |
| 8 | **Static mutable state** — replaced with immutable `UnitTHConfig` record injected into parsers, history and HTML classes; `UnitTH.run(args)` returns a status; parallel runs verified by `ConcurrencyTest` | DONE |
| 36 | **Dead PNG graph code** — removed unused `unitth.graphics` package (6 classes, never instantiated since Chart.js) and the 10 unused images in `src/main/resources/images/` | DONE |
| 37 | **Package/module derivation for CTRF** — package taken from `environment.projectName`; test case `className` set to the dotted module name so package pages list test cases and verdict spread | DONE |
| 38 | **Run limit** — at most the 100 most recent run folders are reported | DONE |
| 39 | **Build modernization** — plugin updates, enforcer (Maven 3.6.3+, Java 21+), pinned default plugins, `-Xlint:all`, `maven.compiler.release`, fixed jar name `unitth.jar`, POM license/scm metadata | DONE |
| 34 | **`NaN%` pass rate for all-skipped tests** — pass rates with no executed tests now render `N/A` (text, KPI, progress bar); chart data emits `null` gaps; `ReportE2EIT` and `deploy-e2e.sh` fail on any `NaN` | DONE |
| 35 | **Default JSON path ignores `environment.projectName`** — removed the duplicate CTRF parser inside `JUnitReportParser`; JSON files in default mode are delegated to `CTRFReportParser`, so both modes group by `projectName` | DONE |
| 40 | **SonarQube critical fixes** — HTML-escape all names from input files (XSS) and restrict output file names / chart ids to `[A-Za-z0-9._-]`; fixed NPE on suite-level `<skipped/>`; writers via try-with-resources; removed fields shadowing `TestItem`; day arithmetic in `long`; no `printStackTrace`; typed `toArray`. Sonar: 0 bugs, 0 vulnerabilities (A/A) | DONE |
| 42 | **Sonar quality gate (new code)** — `unitth.core.Log` (java.util.logging, plain stderr line) instead of `System.err`; `StringBuilder`/constants cleanups; tests for trend KPIs, Jenkins mode, error paths; new-code coverage 88%, 0 new issues | DONE |
| 43 | **Locale-dependent number formatting** — on non-US default locales `passPctToString` produced `50,00`, crashing trend calculation (`Double.parseDouble`) and breaking chart JS arrays; all `String.format` now use `Locale.US` | DONE |
| 41 | **Duplicated code (Sonar gate)** — shared `SaxReportParser` base for JUnit/Jenkins parsers (also disables external entities / DTD loading, XXE hardening); `TrendSource` interface for the 1/3/7-day trend getters; `HtmlGen` helpers for page head, KPI cards, dashboard charts, package/module rows and test case rows. Duplication 7.4% -> 0.4%, Sonar quality gate passed | DONE |
| 22 | **File DB layer** — JSON run store (`--store`, `unitth.store.file`): content-hash run ids, incremental ingest incl. existing XML/CTRF history, retention via `unitth.max.runs` (default 100), regenerate without raw reports; UnitTH self-history (`e2e/self-history.sh`). Code simplified on the way (~2k lines less, identical output) | DONE |
| 44 | **SQL comment lines counted as tests** — CTRF/XML entries named `#...` or blank (CTRF message `Commented or empty`) were reported as skipped tests and lowered pass rates; now dropped when parsing and when stored runs are loaded (`unitth.ctrf.skip.comments`, default true), so old and new runs are counted the same way | DONE |
| 45 | **Flat run list (~270 rows)** — Runs and Packages merged into one Projects section: one `<details>` row per package (latest run, pass rate, change since previous run) that expands to all of its runs; no JS needed apart from "Expand all" | DONE |
| 46 | **Packages/Modules duplicated** — the Modules section, nav entry and package-page table are hidden when every package has exactly one module (always the case for DataOps CTRF); JUnit reports with several modules keep them | DONE |
| 47 | **Local CI, workflow and deploy** — `.github/workflows/ci.yml` (lint, test with `./mvnw`, sonar on the DataOpsBackbone self-hosted runner), same workflow locally via `local-ci/act.sh`; `local-ci/sonar-self-scan.sh` (Trivy, gitleaks, ShellCheck, gate, 0 HIGH/BLOCKER/CRITICAL); `local-ci/deploy.sh` (runner image + restart + `deploy-e2e.sh`). The 20 existing HIGH/CRITICAL Sonar issues were fixed on the way (S115 on `TestCaseVerdict` suppressed: names are persisted in the store) | DONE |
| 49 | **UnitTH missing from its own report** — `local-ci/record-history.sh` (called by the `sonar` job on master and under act) renames the surefire/failsafe suites to `<project>.<TestClass>` and ingests them as one run into `zbrainiac-labs_UnitTH.json` in the DataOpsBackbone store dir; `act.sh` mounts the store/report volumes and sets the project name (act reads the owner from the git remote); `deploy-e2e.sh` fails if the store exists but the project is missing. The Modules section is shown again (UnitTH has 16 test classes) | DONE |
| 50 | **Run tables oldest first** — `tablesort.js` sorted every table by its first column on load (`defaultColumn: 0`, text order of `Run-N`), reversing the generated newest-first order; now `null` keeps the generated order, header clicks still sort. Also `deploy.sh --quick` now fixes owner/mode of the copied jar (built jar is owner-only, `docker cp` kept uid 501) | DONE |
