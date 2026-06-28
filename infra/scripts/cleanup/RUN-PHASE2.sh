#!/usr/bin/env bash
# ==============================================================================
# RUN-PHASE2.sh — Master runner for all Phase 2 cleanup scripts
# Runs scripts in order with a health gate between each step.
# Stops immediately if any step fails.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
MASTER_LOG="/home/azim/cleanup-phase2-master-${TIMESTAMP}.log"

log()    { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" | tee -a "$MASTER_LOG"; }
log_sep(){ log "════════════════════════════════════════════════════════════════"; }

step() {
    local script="$1"
    local desc="$2"
    log_sep
    log "RUNNING: $desc"
    log "Script:  $script"
    log_sep
    bash "$script" 2>&1 | tee -a "$MASTER_LOG"
    local exit_code=${PIPESTATUS[0]}
    if [[ $exit_code -ne 0 ]]; then
        log "FAILED: $desc exited with code $exit_code. Stopping Phase 2."
        log "Review: $MASTER_LOG"
        exit $exit_code
    fi
    log "PASSED: $desc"
    # Quick HTTP gate between steps
    local http_code
    http_code=$(curl -s --max-time 5 -o /dev/null -w "%{http_code}" "http://localhost:8200/health" 2>/dev/null || echo "000")
    if [[ "$http_code" != "200" ]]; then
        log "HEALTH GATE FAILED after $desc — fazle-core returned HTTP $http_code"
        log "Stopping Phase 2 until service is confirmed healthy."
        exit 1
    fi
    log "Health gate: fazle-core HTTP 200 — continuing."
}

log_sep
log "PHASE 2 MASTER RUN"
log "Master log: $MASTER_LOG"
log_sep

step "${SCRIPT_DIR}/01-phase2-git-tmppack.sh"  "2.1 — Git tmp_pack removal (12 GB)"
step "${SCRIPT_DIR}/02-phase2-log-truncate.sh" "2.2 — Log file truncation (110 MB)"
step "${SCRIPT_DIR}/03-phase2-old-backups.sh"  "2.3 — Old backup removal (~500 MB)"
step "${SCRIPT_DIR}/04-phase2-docker.sh"       "2.4 — Docker container cleanup"
step "${SCRIPT_DIR}/05-phase2-verify-post.sh"  "2.5 — Post-cleanup verification"

log_sep
log "ALL PHASE 2 STEPS COMPLETE"
log "Master log: $MASTER_LOG"
log_sep

echo ""
echo "================================================================"
echo "  PHASE 2 COMPLETE"
echo "  Master log: $MASTER_LOG"
echo "  Run next: scripts/cleanup/00-verify-state.sh post"
echo "================================================================"
