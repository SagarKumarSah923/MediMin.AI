#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

mvn -q package -DskipTests

cleanup() {
  kill "${PROFILE_PID:-}" "${CARE_PID:-}" "${AGENT_PID:-}" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

PORT=18081 java -jar profile-service/target/profile-service-0.1.0-SNAPSHOT.jar &
PROFILE_PID=$!
PORT=18082 java -jar care-service/target/care-service-0.1.0-SNAPSHOT.jar &
CARE_PID=$!
PORT=18083 java -jar agent-service/target/agent-service-0.1.0-SNAPSHOT.jar &
AGENT_PID=$!

exec java -jar gateway/target/gateway-0.1.0-SNAPSHOT.jar