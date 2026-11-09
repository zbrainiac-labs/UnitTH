#!/bin/bash
# -----------------------------------------------------------------------------
# Deploy the working copy of UnitTH into the local DataOpsBackbone stack and
# verify it: build unitth.jar, put it into the runner image, restart all runner
# replicas and the report web server, rebuild the report from the run stores
# and run e2e/deploy-e2e.sh against http://localhost:8080.
#
# Usage: bash local-ci/deploy.sh [--quick] [--no-e2e] [--force]
#   --quick   copy the jar into the running runner containers instead of
#             rebuilding the image (fast; lost when a container is recreated)
#   --no-e2e  skip e2e/deploy-e2e.sh
#   --force   restart runners even while one of them is running a job
# Env:   BACKBONE (default ../DataOpsBackbone)
# Note:  github-runner/unitth.jar is tracked in DataOpsBackbone; this leaves it
#        modified there (not committed).
# -----------------------------------------------------------------------------
set -Eeuo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKBONE="$(cd "${BACKBONE:-$REPO/../DataOpsBackbone}" && pwd)"
QUICK=false
E2E=true
FORCE=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --quick) QUICK=true ;;
    --no-e2e) E2E=false ;;
    --force) FORCE=true ;;
    -h|--help) sed -n '2,16p' "$0"; exit 0 ;;
    *) echo "Unknown argument: $1"; exit 1 ;;
  esac
  shift
done

compose() { (cd "$BACKBONE" && docker compose --env-file .env "$@"); }

runners() { docker ps --filter label=com.docker.compose.service=runner --format '{{.Names}}' | sort; }
RUNNERS=$(runners)
[[ -n "$RUNNERS" ]] || { echo "ERROR: no runner container running -- run $BACKBONE/start.sh first"; exit 1; }

if ! $FORCE; then
  for r in $RUNNERS; do
    if docker exec "$r" pgrep -f Runner.Worker >/dev/null 2>&1; then
      echo "ERROR: $r is running a job; wait for it or use --force"; exit 1
    fi
  done
fi

echo "=== Build unitth.jar"
(cd "$REPO" && ./mvnw -B -q package -DskipTests)
cp "$REPO/target/unitth.jar" "$BACKBONE/github-runner/"

if $QUICK; then
  echo "=== Copy jar into running runners (no image rebuild)"
  for r in $RUNNERS; do
    docker cp "$REPO/target/unitth.jar" "$r:/usr/local/bin/unitth.jar"
    # docker cp keeps the host uid and mode (the built jar is owner-only)
    docker exec -u 0 "$r" sh -c 'chown root:root /usr/local/bin/unitth.jar && chmod 0644 /usr/local/bin/unitth.jar'
    echo "  $r"
  done
else
  case "$(uname -m)" in
    arm64|aarch64) ARCH=arm64 ;;
    *) ARCH=amd64 ;;
  esac
  echo "=== Rebuild runner image (linux/$ARCH)"
  docker build -q --platform="linux/$ARCH" --build-arg TARGETARCH="$ARCH" \
    -t brainiac/local-github-runner -f "$BACKBONE/github-runner/Dockerfile" "$BACKBONE/github-runner"
  echo "=== Restart runners and report server"
  # services: runner (all replicas) and nginx-server (container test-unit-reports)
  compose up -d --wait --no-deps --force-recreate runner nginx-server
  RUNNERS=$(runners)
fi

RUNNER=$(echo "$RUNNERS" | head -1)
echo "=== Rebuild report from the run stores ($RUNNER)"
docker exec "$RUNNER" bash -c 'rm -rf /home/docker/sql-report-vol/* && cd /usr/local/bin \
  && java -Dunitth.report.dir=/home/docker/sql-report-vol -jar unitth.jar --store-dir /home/docker/unitth-store' \
  | tail -1

if $E2E; then
  echo "=== e2e/deploy-e2e.sh"
  (cd "$REPO" && RUNNER="$RUNNER" sh e2e/deploy-e2e.sh)
fi
echo "Report: http://localhost:8080/"
