#!/usr/bin/env bash
# ==============================================================================
# 04-phase2-docker.sh — Remove stopped/exited Docker containers
# Target: fazle-brain container (exited 13 days ago) + prune stopped containers
# NOTE: Does NOT remove any images or volumes (those are Phase 3+).
# Safety: only removes containers with status=exited, never running ones.
# ==============================================================================
set -euo pipefail

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="/home/azim/cleanup-log-${TIMESTAMP}.txt"

log()    { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"; }
log_sep(){ log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; }

trap 'log "ERROR at line ${LINENO}. Log: ${LOG_FILE}"' ERR

log_sep
log "PHASE 2.4: Docker Container Cleanup"
log "Target: Remove exited/stopped containers only. No images or volumes."
log "Log: ${LOG_FILE}"
log_sep

# ── Protected containers (must NOT be removed even if exited) ─────────────────
# Add names here if a container is expected to cycle (e.g., one-shot init jobs)
PROTECTED_CONTAINERS=()

# ══════════════════════════════════════════════════════════════════════════════
# PRE-CHECK 1 — List all running containers (never touch these)
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "PRE-CHECK 1: Active running containers (WILL NOT be touched)..."
docker ps --format "  RUNNING: {{.Names}}  ({{.Status}})" 2>/dev/null | tee -a "$LOG_FILE" || true

# ══════════════════════════════════════════════════════════════════════════════
# PRE-CHECK 2 — List exited containers (candidates)
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "PRE-CHECK 2: Exited/stopped containers (CANDIDATES for removal)..."

EXITED_NAMES=()
while IFS= read -r line; do
    container_name=$(echo "$line" | awk '{print $NF}')
    log "  CANDIDATE: $line"
    EXITED_NAMES+=("$container_name")
done < <(docker ps -a --filter "status=exited" --format "  {{.Names}}  ({{.Status}})  image={{.Image}}" 2>/dev/null || true)

if [[ ${#EXITED_NAMES[@]} -eq 0 ]]; then
    log "  No exited containers found. Nothing to remove."
    log_sep
    log "PHASE 2.4 COMPLETE — No exited containers."
    echo ""
    echo "================================================================"
    echo "  PHASE 2.4 COMPLETE — No exited containers to remove."
    echo "================================================================"
    exit 0
fi

log "  Found ${#EXITED_NAMES[@]} exited container(s)."

# ══════════════════════════════════════════════════════════════════════════════
# STEP 1 — Record container details before removal
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "STEP 1: Recording container details before removal..."

SNAPSHOT_FILE="/home/azim/cleanup-quarantine/docker-containers-${TIMESTAMP}.json"
mkdir -p "$(dirname "$SNAPSHOT_FILE")"

docker inspect "${EXITED_NAMES[@]}" 2>/dev/null > "$SNAPSHOT_FILE" || true
log "  Container snapshots saved: ${SNAPSHOT_FILE}"

# ══════════════════════════════════════════════════════════════════════════════
# STEP 2 — Remove each exited container
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "STEP 2: Removing exited containers..."

REMOVED=0
SKIPPED=0

for name in "${EXITED_NAMES[@]}"; do
    # Check against protected list
    is_protected=false
    for p in "${PROTECTED_CONTAINERS[@]}"; do
        [[ "$name" == "$p" ]] && is_protected=true && break
    done

    if [[ "$is_protected" == "true" ]]; then
        log "  SKIP (protected): $name"
        (( SKIPPED++ )) || true
        continue
    fi

    # Final safety: confirm it's still exited, not restarted since our check
    current_status=$(docker inspect --format '{{.State.Status}}' "$name" 2>/dev/null || echo "missing")
    if [[ "$current_status" == "running" ]]; then
        log "  SKIP (now running — restarted since our check): $name"
        (( SKIPPED++ )) || true
        continue
    fi

    exit_code=$(docker inspect --format '{{.State.ExitCode}}' "$name" 2>/dev/null || echo "?")
    finished_at=$(docker inspect --format '{{.State.FinishedAt}}' "$name" 2>/dev/null || echo "?")
    image=$(docker inspect --format '{{.Config.Image}}' "$name" 2>/dev/null || echo "?")

    log "  Removing: $name  (image=$image  exitCode=$exit_code  finishedAt=$finished_at)"
    docker rm "$name" 2>&1 | tee -a "$LOG_FILE" || { log "  WARNING: docker rm failed for $name"; (( SKIPPED++ )) || true; continue; }
    log "  Removed: $name"
    (( REMOVED++ )) || true
done

# ══════════════════════════════════════════════════════════════════════════════
# STEP 3 — Verify running containers are still running
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "STEP 3: Verifying critical containers are still running..."

CRITICAL_CONTAINERS=("ai-postgres" "ai-redis" "ollama" "open-webui" "qdrant" "minio" "grafana" "prometheus" "loki")
ALL_OK=true

for c in "${CRITICAL_CONTAINERS[@]}"; do
    status=$(docker inspect --format '{{.State.Status}}' "$c" 2>/dev/null || echo "missing")
    if [[ "$status" == "running" ]]; then
        log "  OK: $c → running"
    else
        log "  WARNING: $c → $status"
        ALL_OK=false
    fi
done

# ══════════════════════════════════════════════════════════════════════════════
# STEP 4 — Docker system disk usage after cleanup
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "STEP 4: Docker system disk summary after cleanup..."
docker system df 2>/dev/null | tee -a "$LOG_FILE" || true

# ══════════════════════════════════════════════════════════════════════════════
# Summary
# ══════════════════════════════════════════════════════════════════════════════
log ""
log_sep
log "RESULTS:"
log "  Containers removed: ${REMOVED}"
log "  Containers skipped: ${SKIPPED}"
log "  Critical containers all running: ${ALL_OK}"
log "  Log: ${LOG_FILE}"
log ""
if [[ "$ALL_OK" == "true" ]]; then
    log "PHASE 2.4 COMPLETE — Docker container cleanup done."
else
    log "PHASE 2.4 COMPLETE WITH WARNINGS — review container statuses above."
fi
log_sep

echo ""
echo "================================================================"
echo "  PHASE 2.4 COMPLETE"
echo "  Containers removed: ${REMOVED} | Skipped: ${SKIPPED}"
echo "  Critical containers healthy: ${ALL_OK}"
echo "================================================================"
