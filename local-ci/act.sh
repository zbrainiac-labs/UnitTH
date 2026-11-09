#!/bin/bash
# -----------------------------------------------------------------------------
# Run .github/workflows/ci.yml locally with nektos/act -- same jobs as GitHub:
# lint, test, sonar. Artifact uploads are skipped; all jobs share this working
# copy (--bind), so uncommitted changes are tested and test -> sonar needs no
# artifact server. The sonar job runs in the DataOpsBackbone runner image on
# sonarnet with the sonar-init token, exactly like on the self-hosted runners.
#
# Usage: bash local-ci/act.sh [--job=<lint|test|sonar>] [-- <extra act args>]
# Env:   GITHUB_TOKEN (else from `gh auth token`), BACKBONE (default ../DataOpsBackbone)
# Requires: act, docker, the running DataOpsBackbone stack (SonarQube, sonarnet)
# Out:   log in local-ci/logs/
# -----------------------------------------------------------------------------
set -Eeuo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKBONE="$(cd "${BACKBONE:-$REPO/../DataOpsBackbone}" && pwd)"
JOB=""
ACT_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
    --job=*) JOB="${1#*=}" ;;
    --) shift; ACT_ARGS=("$@"); break ;;
    -h|--help) sed -n '2,13p' "$0"; exit 0 ;;
    *) echo "Unknown argument: $1"; exit 1 ;;
  esac
  shift
done

for cmd in act docker; do
  command -v "$cmd" >/dev/null || { echo "ERROR: $cmd not found"; exit 1; }
done
NETWORK=$(docker network ls --format '{{.Name}}' | grep -m1 'sonarnet$' || true)
VOLUME=$(docker volume ls --format '{{.Name}}' | grep -m1 'sonar_env$' || true)
[[ -n "$NETWORK" && -n "$VOLUME" ]] || { echo "ERROR: DataOpsBackbone stack not running -- run its ./start.sh first"; exit 1; }

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

# github.event.act marks local runs in ci.yml
cat > "$TMP/event.json" <<JSON
{"act": true,
 "repository": {"name": "UnitTH", "full_name": "zbrainiac-labs/UnitTH",
                "owner": {"login": "zbrainiac-labs"}}}
JSON

# --- secrets (never echoed; the file is removed on exit) ----------------------
echo "GITHUB_TOKEN=${GITHUB_TOKEN:-$(gh auth token 2>/dev/null || true)}" > "$TMP/secrets"

# act derives GITHUB_REPOSITORY from the local git remote, which may still use an
# old owner name; the recorded project must match the GitHub one
echo "UNITTH_PROJECT=${UNITTH_PROJECT:-zbrainiac-labs_UnitTH}" > "$TMP/env"

# same report mounts as the compose runner, so recorded runs show up on http://localhost:8080
mkdir -p "$BACKBONE/unitth-store"
REPORT_MOUNTS="-v sql_unit_report:/home/docker/sql-report-vol -v $BACKBONE/unitth-store:/home/docker/unitth-store"

LOG_DIR="$REPO/local-ci/logs"
mkdir -p "$LOG_DIR"
LOG="$LOG_DIR/$(date +%Y%m%dT%H%M%S)-ci.log"
echo "act: ci.yml ${JOB:+job=$JOB }-- log: $LOG"

# self-hosted = the DataOpsBackbone runner image; ubuntu-latest = act image
set +e
act workflow_dispatch \
  -C "$REPO" \
  -W "$REPO/.github/workflows/ci.yml" \
  -e "$TMP/event.json" \
  ${JOB:+-j "$JOB"} \
  -P self-hosted=brainiac/local-github-runner \
  -P ubuntu-latest=catthehacker/ubuntu:act-24.04 \
  --pull=false \
  --network "$NETWORK" \
  --container-options "-v $VOLUME:/sonar:ro $REPORT_MOUNTS" \
  --bind \
  --secret-file "$TMP/secrets" \
  --env-file "$TMP/env" \
  ${ACT_ARGS[@]+"${ACT_ARGS[@]}"} 2>&1 | tee "$LOG"
RC=${PIPESTATUS[0]}
set -e

if grep -qE 'squ_[A-Za-z0-9]{20,}|gh[ops]_[A-Za-z0-9]{20,}' "$LOG"; then
  echo "ERROR: secret-looking value found in log $LOG"
  RC=1
fi
echo "act exit code: $RC"
exit "$RC"
