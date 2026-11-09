# Local CI

The scripts here run the same checks as `.github/workflows/ci.yml`, against the local
[DataOpsBackbone](../../DataOpsBackbone) stack (SonarQube on `sonarnet`, token in the
`sonar_env` volume). Start the stack first with its `./start.sh`.

| Script | What it does |
|--------|--------------|
| `act.sh [--job=<lint\|test\|sonar>]` | Runs `ci.yml` with nektos/act: `ubuntu-latest` = `catthehacker/ubuntu:act-24.04`, `self-hosted` = `brainiac/local-github-runner`. All jobs share the working copy (`--bind`), so uncommitted changes are tested and `test` hands its build output to `sonar` without an artifact server. Log in `logs/`. |
| `sonar-self-scan.sh` | Trivy fs, gitleaks (history), ShellCheck, then `sonar-scanner` on the Maven build output (`target/classes`, `target/dependency`, JaCoCo XML) with `qualitygate.wait`. Fails on a failed gate or any HIGH/BLOCKER/CRITICAL issue. Builds with `./mvnw` first if the build output is missing. |
| `deploy.sh [--quick] [--no-e2e] [--force]` | Builds `unitth.jar`, copies it to `DataOpsBackbone/github-runner/`, rebuilds the runner image, recreates all `runner` replicas and `nginx-server`, rebuilds the report from the run stores and runs `e2e/deploy-e2e.sh`. `--quick` copies the jar into the running containers instead (lost when a container is recreated). Refuses to restart while a runner is in a job unless `--force`. |
| `record-history.sh` | Called by the `sonar` job (master and act runs only): copies the surefire/failsafe XML of the build into one run folder, renames each suite to `<project>.<TestClass>` (UnitTH takes the project from everything before the last dot) and ingests it into `/home/docker/unitth-store/<project>.json`, then rebuilds the shared report. Project = `UNITTH_PROJECT`, else `GITHUB_REPOSITORY` with `/` -> `_`; `act.sh` sets `zbrainiac-labs_UnitTH` because act takes the repo from the local git remote. Never fails the build. |

## UnitTH in its own report

UnitTH's test history appears as project `zbrainiac-labs_UnitTH` next to the SQL
projects on http://localhost:8080, with its test classes as modules (so the Modules
section is shown). `e2e/deploy-e2e.sh` fails if that store exists but the project is
missing, which catches a change of the naming convention.

## Why the build runs outside the runner container

The DataOpsBackbone runner image only has a Java runtime and `sonar-scanner` (it runs
`unitth.jar`), no JDK or Maven. The `test` job therefore builds with `./mvnw` on a
Java 21 runner, and the `sonar` job on the self-hosted runner only reads the build
output. Deployment stays out of `ci.yml`: a job must not restart the runners it runs on.

## SonarQube projects

Community Build has no branch analysis, so there is one project key per stream:
`unitth` (master on GitHub), `unitth-local-pr` (pull requests), `unitth-local`
(act and local runs, the default).

## Notes

- `deploy.sh` leaves `github-runner/unitth.jar` modified in the DataOpsBackbone repo
  (it is tracked there); commit it there when the version should become the default.
- Override the stack location with `BACKBONE=/path/to/DataOpsBackbone`.
