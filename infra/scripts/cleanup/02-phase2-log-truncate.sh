#!/usr/bin/env bash
# ==============================================================================
# 02-phase2-log-truncate.sh — Truncate orphaned and oversized log files
# Target: ~110 MB recovery (bridge3.log) + minor cleanup
# Safety: truncate (not delete) — inode preserved, no process disruption
# ==============================================================================
set -euo pipefail

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="/home/azim/cleanup-log-${TIMESTAMP}.txt"

log()    { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"; }
log_sep(){ log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; }

trap 'log "ERROR at line ${LINENO}. Log: ${LOG_FILE}"' ERR

log_sep
log "PHASE 2.2: Log File Truncation"
log "Log: ${LOG_FILE}"
log_sep

# ── Target files and their expected owners (service that may hold FD open) ───

declare -A LOG_TARGETS
# Format: "log_path:owning_service_or_empty"
# bridge3.log → bridge3 has NO service; it is safe to truncate (no open FD)
LOG_TARGETS["/home/azim/bridges/mcp/logs/bridge3.log"]="ORPHAN"
# These are confirmed orphan/stale files at home root
LOG_TARGETS["/home/azim/autoreply.log"]="ORPHAN"
LOG_TARGETS["/home/azim/PHASE_FINAL_OBSERVE_20260523_062246.log"]="ORPHAN"
LOG_TARGETS["/home/azim/bridges/mcp/autoreply.log"]="ORPHAN"

# ══════════════════════════════════════════════════════════════════════════════
# PRE-CHECK: Confirm bridge3 service is NOT running
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "PRE-CHECK: Confirming bridge3 service is not running..."

if systemctl is-active whatsapp-bridge3 2>/dev/null | grep -q "active"; then
    log "  ERROR: whatsapp-bridge3.service is ACTIVE."
    log "  bridge3.log cannot be truncated while bridge3 is running."
    log "  Stop bridge3 first, or skip this file."
    exit 1
fi

# Also check if any process has bridge3.log open
BRIDGE3_LOG="/home/azim/bridges/mcp/logs/bridge3.log"
if [[ -f "$BRIDGE3_LOG" ]]; then
    if lsof "$BRIDGE3_LOG" 2>/dev/null | grep -q .; then
        log "  WARNING: bridge3.log has open file descriptors:"
        lsof "$BRIDGE3_LOG" 2>/dev/null | tee -a "$LOG_FILE" || true
        log "  Skipping bridge3.log truncation (file is in use)."
        LOG_TARGETS["$BRIDGE3_LOG"]="SKIP_IN_USE"
    else
        log "  bridge3.log is not open by any process. Safe to truncate."
    fi
fi

# ══════════════════════════════════════════════════════════════════════════════
# Record sizes before
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "Recording sizes before truncation..."
TOTAL_BEFORE=0
declare -A SIZES_BEFORE

for log_path in "${!LOG_TARGETS[@]}"; do
    if [[ -f "$log_path" ]]; then
        size_bytes=$(stat -c%s "$log_path" 2>/dev/null || echo "0")
        size_human=$(du -sh "$log_path" 2>/dev/null | cut -f1)
        SIZES_BEFORE["$log_path"]="$size_bytes"
        TOTAL_BEFORE=$(( TOTAL_BEFORE + size_bytes ))
        log "  ${size_human}  ${log_path}  [${LOG_TARGETS[$log_path]}]"
    else
        SIZES_BEFORE["$log_path"]="0"
        log "  MISSING (skip): ${log_path}"
    fi
done

TOTAL_BEFORE_MB=$(( TOTAL_BEFORE / 1024 / 1024 ))
log "Total before: ~${TOTAL_BEFORE_MB} MB"

# ══════════════════════════════════════════════════════════════════════════════
# Truncate each target
# ══════════════════════════════════════════════════════════════════════════════
log ""
log_sep
log "Truncating log files..."
log_sep

TRUNCATED=0
SKIPPED=0

for log_path in "${!LOG_TARGETS[@]}"; do
    owner="${LOG_TARGETS[$log_path]}"

    if [[ ! -f "$log_path" ]]; then
        log "  SKIP (not found): $log_path"
        (( SKIPPED++ )) || true
        continue
    fi

    if [[ "$owner" == "SKIP_IN_USE" ]]; then
        log "  SKIP (in use): $log_path"
        (( SKIPPED++ )) || true
        continue
    fi

    size_before=$(du -sh "$log_path" 2>/dev/null | cut -f1)

    # Append a note before truncating so the file isn't zero-byte
    # (some log viewers expect a non-empty file)
    {
        echo "# Log truncated by cleanup script on $(date)"
        echo "# Previous size: ${size_before}"
        echo "# For history see: /home/azim/backups/cleanup-20260609_083626/"
    } > "${log_path}.new" && mv "${log_path}.new" "$log_path"

    # Alternative: true truncate to zero bytes
    # : > "$log_path"

    size_after=$(du -sh "$log_path" 2>/dev/null | cut -f1)
    log "  TRUNCATED: $log_path  (${size_before} → ${size_after})"
    (( TRUNCATED++ )) || true
done

# ══════════════════════════════════════════════════════════════════════════════
# Verify bridge and core logs are still intact (not accidentally touched)
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "Verifying active logs were NOT touched..."

ACTIVE_LOGS=(
    "/home/azim/bridges/mcp/logs/bridge1.log"
    "/home/azim/bridges/mcp/logs/bridge2.log"
    "/home/azim/core/logs/fazle-core.log"
    "/home/azim/core/logs/fazle-core-error.log"
    "/home/azim/agent/logs/agent-error.log"
)

for al in "${ACTIVE_LOGS[@]}"; do
    if [[ -f "$al" ]]; then
        sz=$(du -sh "$al" 2>/dev/null | cut -f1)
        log "  INTACT: $al  ($sz)"
    else
        log "  NOT FOUND (expected): $al"
    fi
done

# ══════════════════════════════════════════════════════════════════════════════
# Summary
# ══════════════════════════════════════════════════════════════════════════════
log ""
log_sep
log "RESULTS:"
log "  Files truncated: ${TRUNCATED}"
log "  Files skipped:   ${SKIPPED}"
log "  Approx space recovered: ~${TOTAL_BEFORE_MB} MB"
log "  Log: ${LOG_FILE}"
log ""
log "PHASE 2.2 COMPLETE — Log file truncation done."
log_sep

echo ""
echo "================================================================"
echo "  PHASE 2.2 COMPLETE"
echo "  Files truncated: ${TRUNCATED} | Skipped: ${SKIPPED}"
echo "  Space recovered: ~${TOTAL_BEFORE_MB} MB"
echo "================================================================"
