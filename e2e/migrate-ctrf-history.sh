#!/bin/bash
# One-time migration of CTRF run folders into one UnitTH run store per project.
#
# Usage: migrate-ctrf-history.sh [--apply] [SRC_DIR] [STORE_DIR] [JAR]
#   SRC_DIR    run folders (default: ~/workspace/DataOpsBackbone/sql-unit-reports)
#   STORE_DIR  store directory (default: ~/workspace/DataOpsBackbone/unitth-store)
#   JAR        unitth.jar (default: target/unitth.jar of this repository)
#
# Without --apply only the per-project plan is printed. With --apply each
# project's folders are ingested into <STORE_DIR>/<project>.json (same name
# rule as sql_validation_v4.sh) and the folders are deleted only if UnitTH
# reported ingest=ok for that project. Folders without a readable projectName
# are never touched. Only the newest unitth.max.runs (100) folders per project
# are ingested; older ones would be trimmed from the store anyway.
set -u

APPLY=false
if [[ "${1:-}" == "--apply" ]]; then
  APPLY=true
  shift
fi
HERE="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${1:-$HOME/workspace/DataOpsBackbone/sql-unit-reports}"
STORE_DIR="${2:-$HOME/workspace/DataOpsBackbone/unitth-store}"
JAR="${3:-$HERE/target/unitth.jar}"

[[ -d "$SRC" ]] || { echo "No run folder directory: $SRC"; exit 1; }
[[ -f "$JAR" ]] || { echo "No jar: $JAR"; exit 1; }

# folder<TAB>projectName for every run folder (empty name if unreadable)
MAP=$(python3 - "$SRC" <<'PY'
import glob, json, os, sys
src = sys.argv[1]
for d in sorted(os.listdir(src)):
    p = os.path.join(src, d)
    if not os.path.isdir(p):
        continue
    name = ""
    for f in sorted(glob.glob(os.path.join(p, "TEST_*.json"))):
        try:
            with open(f, encoding="utf-8") as fh:
                name = json.load(fh)["results"]["environment"]["projectName"] or ""
            break
        except Exception:
            pass
    print(p + "\t" + name)
PY
)

total=$(printf '%s\n' "$MAP" | grep -c . || true)
unknown=$(printf '%s\n' "$MAP" | awk -F'\t' '$2==""' | grep -c . || true)
echo "Run folders: $total (without projectName: $unknown, kept)"
projects=$(printf '%s\n' "$MAP" | awk -F'\t' '$2!=""{print $2}' | sort -u)

rc=0
while IFS= read -r project; do
  [[ -z "$project" ]] && continue
  name="${project%_SQLValidation}"
  store="$STORE_DIR/${name//[^A-Za-z0-9._-]/_}.json"
  folders=()
  while IFS= read -r f; do folders+=("$f"); done < <(printf '%s\n' "$MAP" | awk -F'\t' -v p="$project" '$2==p{print $1}')
  echo "$project: ${#folders[@]} folder(s) -> $store"
  $APPLY || continue
  mkdir -p "$STORE_DIR"
  log=$(java -jar "$JAR" --store "$store" --ingest-only --ctrf "${folders[@]}" 2>&1)
  echo "$log" | grep -E "Run store|WARN|ERROR" | sed 's/^/  /'
  if echo "$log" | grep -q "ingest=ok"; then
    rm -rf -- "${folders[@]}"
    echo "  removed ${#folders[@]} folder(s)"
  else
    echo "  NOT removed (ingest not ok)"
    rc=1
  fi
done <<< "$projects"

$APPLY || echo "Dry run only; re-run with --apply to ingest and delete."
exit $rc
