#!/usr/bin/env bash
# Runs villager_checks.py on a fresh world for each build, with a real dev server, a real client under Xvfb and the
# mock AI provider:
#   tools/qa/run_matrix.sh [ws:loader ...]     (default: all eleven)
# Results go to $QA_LOGS/results/<ws>-<loader>.txt; a summary line per build is printed.
set -u
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
LOGS=${QA_LOGS:-/tmp/sv-qa}
mkdir -p "$LOGS/results"
BUILDS=${*:-"1.20.1:fabric 1.20.1:forge 1.21.1:fabric 1.21.1:forge 1.21.1:neoforge 26.1.2:fabric 26.1.2:neoforge 26.2:fabric 26.2:neoforge 26.3:fabric 26.3:neoforge"}
QA="$ROOT/tools/qa"
export DISPLAY=${DISPLAY:-:1}

wait_for() {  # file, pattern, seconds
  local deadline=$((SECONDS + $3))
  while [ $SECONDS -lt $deadline ]; do
    grep -qE "$2" "$1" 2>/dev/null && return 0
    sleep 5
  done
  return 1
}

# An X display for the client and the mock AI provider, started once.
if ! xdpyinfo -display "$DISPLAY" >/dev/null 2>&1 && ! pgrep -x Xvfb >/dev/null; then
  Xvfb "$DISPLAY" -screen 0 1920x1080x24 > "$LOGS/xvfb.log" 2>&1 &
  sleep 2
fi
if ! curl -s http://127.0.0.1:18080/qa/fail?on=0 >/dev/null; then
  nohup python3 "$QA/mock_ai.py" --log "$LOGS/ai-requests.jsonl" > "$LOGS/mock_ai.log" 2>&1 &
  sleep 1
fi

for build in $BUILDS; do
  ws=${build%%:*}; loader=${build##*:}
  out="$LOGS/results/$ws-$loader.txt"
  "$QA/devrun.sh" stop-client; "$QA/devrun.sh" stop-server; sleep 10
  "$QA/devrun.sh" prepare-server "$ws" "$loader"
  rm -f "$LOGS/$loader-server.log" "$LOGS/$loader-client.log"
  "$QA/devrun.sh" start-server "$ws" "$loader"
  if ! wait_for "$LOGS/$loader-server.log" 'Done \(|BUILD FAILED|Exception in thread' 1800 \
      || ! grep -q 'Done (' "$LOGS/$loader-server.log"; then
    echo "$ws $loader: SERVER DID NOT START" | tee "$out"; continue
  fi
  "$QA/devrun.sh" start-client "$ws" "$loader"
  if ! wait_for "$LOGS/$loader-server.log" 'Builder joined the game' 2400; then
    echo "$ws $loader: CLIENT DID NOT JOIN" | tee "$out"
  else
    sleep 15
    python3 "$QA/villager_checks.py" --log "$LOGS/$loader-server.log" --client-log "$LOGS/$loader-client.log" \
      --world "$ROOT/$ws/$loader/runs/server/qa" --shots "$LOGS/shots/$ws-$loader" > "$out" 2>&1
  fi
  cp "$LOGS/$loader-server.log" "$LOGS/results/$ws-$loader-server.log"
  cp "$LOGS/$loader-client.log" "$LOGS/results/$ws-$loader-client.log" 2>/dev/null
  echo "$ws $loader: $(tail -n 1 "$out")"
done
"$QA/devrun.sh" stop-client; "$QA/devrun.sh" stop-server
