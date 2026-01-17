#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

: "${GOODMEM_API_KEY:?GOODMEM_API_KEY is required}"

export GOODMEM_BASE_URL="${GOODMEM_BASE_URL:-https://omni-dev.quran.ai:8080}"
export GOODMEM_INSECURE_SSL="${GOODMEM_INSECURE_SSL:-true}"

gradle -p "$ROOT_DIR/server" build

if [[ "${1:-}" == "--frontend" ]]; then
  gradle -p "$ROOT_DIR/server" run &
  SERVER_PID=$!
  trap 'kill $SERVER_PID' EXIT
  (cd "$ROOT_DIR/frontend" && npm run dev)
else
  gradle -p "$ROOT_DIR/server" run
fi
