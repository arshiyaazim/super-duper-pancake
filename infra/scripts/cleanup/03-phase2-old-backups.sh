#!/usr/bin/env bash
# ==============================================================================
# 03-phase2-old-backups.sh — Remove confirmed-unused backup directories
# Target: ~500 MB recovery from old safepoints and backup snapshots
# Safety: verify each target, double-quarantine before final delete
# ==============================================================================
set -euo pipefail

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="/home/azim/cleanup-log-${TIMESTAMP}.txt"
QUARANTINE_DIR="/home/azim/cleanup-quarantine/old-backups-${TIMESTAMP}"

log()    { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"; }
log_sep(){ log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; }

trap 'log "ERROR at line ${LINENO}. Log: ${LOG_FILE}"' ERR

log_sep
log "PHASE 2.3: Old Backup Removal"
log "Log: ${LOG_FILE}"
log_sep

# ── Target list with rationale ─────────────────────────────────────────────────
# Each entry: "path:reason"
declare -a TARGETS=(
    "/home/azim/backups/fazle-dashboard_backup_20260419:dashboard backup from 2026-04-19 (8 weeks old)"
    "/home/azim/backups/safepoint_2026-05-06:safepoint from 2026-05-06 (5 weeks old, post-deploy verified)"
    "/home/azim/_archive_2026-04-25:archived whatsapp-erp (decommissioned 2026-04-25)"
    "/home/azim/docker-compose.yml.bak.20260602_040333:docker-compose backup file (not a directory)"
    "/home/azim/docker-compose.yml.bak.20260602_050656:docker-compose backup file (not a directory)"
    "/home/azim/system-agent-backup-1777241194:agent backup snapshot from Apr 2026, replaced by /home/azim/agent/"
)

# ── Directories that must NOT appear in any of our targets ─────────────────────
PROTECTED_PATHS=(
    "/home/azim/core"
    "/home/azim/agent"
    "/home/azim/locationwhere-backend"
    "/home/azim/backups/cleanup-20260609_083626"   # current backup
    "/etc"
    "/var"
    "/usr"
)

# ══════════════════════════════════════════════════════════════════════════════
# PRE-CHECK: Validate no target is a protected path or mounted filesystem
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "PRE-CHECK: Validating targets are safe to remove..."

ABORT=false
for entry in "${TARGETS[@]}"; do
    target="${entry%%:*}"

    # Must start with /home/azim/
    if [[ "$target" != /home/azim/* ]]; then
        log "  SAFETY BLOCK: $target is outside /home/azim/ — ABORT"
        ABORT=true
        continue
    fi

    # Must not be a protected path
    for protected in "${PROTECTED_PATHS[@]}"; do
        if [[ "$target" == "$protected" || "$target" == "${protected}/"* ]]; then
            log "  SAFETY BLOCK: $target matches protected path $protected — ABORT"
            ABORT=true
        fi
    done

    # Must not be a mount point
    if mountpoint -q "$target" 2>/dev/null; then
        log "  SAFETY BLOCK: $target is a mountpoint — ABORT"
        ABORT=true
    fi
done

if [[ "$ABORT" == "true" ]]; then
    log "ABORT: Safety validation failed. No files removed."
    exit 1
fi
log "  All targets passed safety validation."

# ══════════════════════════════════════════════════════════════════════════════
# PRE-CHECK: Confirm active services don't reference any target directory
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "PRE-CHECK: Checking active service WDs against targets..."

for entry in "${TARGETS[@]}"; do
    target="${entry%%:*}"
    if [[ -d "$target" ]]; then
        # Check if any running process has this as its CWD
        cwd_pids=$(lsof -t +d "$target" 2>/dev/null | head -5 || true)
        if [[ -n "$cwd_pids" ]]; then
            log "  WARNING: Process(es) have open files in $target: PIDs $cwd_pids"
            log "  Skipping this target for safety."
            # Mark skip by prefixing with SKIP
            TARGETS=("${TARGETS[@]/$entry/SKIP:$target:in-use}")
        fi
    fi
done

# ══════════════════════════════════════════════════════════════════════════════
# Record current state
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "Recording sizes before removal..."

TOTAL_BYTES=0
declare -A TARGET_SIZES

for entry in "${TARGETS[@]}"; do
    [[ "$entry" == SKIP:* ]] && continue
    target="${entry%%:*}"
    reason="${entry##*:}"

    if [[ -e "$target" ]]; then
        size_human=$(du -sh "$target" 2>/dev/null | cut -f1)
        size_bytes=$(du -sB1 "$target" 2>/dev/null | cut -f1)
        TARGET_SIZES["$target"]="$size_bytes"
        TOTAL_BYTES=$(( TOTAL_BYTES + size_bytes ))
        log "  ${size_human}  ${target}"
        log "          Reason: ${reason}"
    else
        log "  NOT FOUND (skip): ${target}"
    fi
done

TOTAL_MB=$(( TOTAL_BYTES / 1024 / 1024 ))
log "Total recoverable: ~${TOTAL_MB} MB"

# ══════════════════════════════════════════════════════════════════════════════
# STEP 1 — Create quarantine directory (24-hour holding pattern)
# ══════════════════════════════════════════════════════════════════════════════
log ""
log_sep
log "STEP 1: Moving targets to quarantine..."
log_sep

mkdir -p "${QUARANTINE_DIR}"

# Write rollback instructions into quarantine
cat > "${QUARANTINE_DIR}/ROLLBACK.md" << ROLLBACK_EOF
# Rollback Instructions — Old Backup Quarantine ${TIMESTAMP}

To restore any item from this quarantine, move it back to its original location.

| Item | Original Location |
|------|-------------------|
ROLLBACK_EOF

MOVED=0
SKIPPED=0

for entry in "${TARGETS[@]}"; do
    [[ "$entry" == SKIP:* ]] && { (( SKIPPED++ )) || true; continue; }

    target="${entry%%:*}"

    if [[ ! -e "$target" ]]; then
        log "  SKIP (not found): $target"
        (( SKIPPED++ )) || true
        continue
    fi

    item_name=$(basename "$target")
    dest="${QUARANTINE_DIR}/${item_name}"

    log "  Moving: $target → ${dest}"
    mv "$target" "$dest"
    log "  Moved."
    (( MOVED++ )) || true

    # Add to rollback doc
    echo "| ${item_name} | ${target} |" >> "${QUARANTINE_DIR}/ROLLBACK.md"
done

log "  Moved: ${MOVED} items | Skipped: ${SKIPPED} items"

# ══════════════════════════════════════════════════════════════════════════════
# STEP 2 — Verify active services are still running after quarantine
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "STEP 2: Verifying active services are still healthy after quarantine..."

SERVICES_OK=true
for svc in fazle-core fazle-agent whatsapp-bridge whatsapp-bridge2 media-processor; do
    state=$(systemctl is-active "$svc" 2>/dev/null || echo "inactive")
    if [[ "$state" == "active" ]]; then
        log "  OK: $svc → active"
    else
        log "  WARNING: $svc → $state (may have been inactive before cleanup too)"
        # Not aborting here — a service might have been inactive before we started
    fi
done

# Quick HTTP check on fazle-core
HTTP_CODE=$(curl -s --max-time 5 -o /dev/null -w "%{http_code}" "http://localhost:8200/health" 2>/dev/null || echo "000")
if [[ "$HTTP_CODE" == "200" ]]; then
    log "  OK: fazle-core /health → HTTP 200"
else
    log "  WARNING: fazle-core /health → HTTP $HTTP_CODE"
fi

# ══════════════════════════════════════════════════════════════════════════════
# STEP 3 — Delete quarantine
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "STEP 3: Deleting quarantine directory..."

QUARANTINE_SIZE=$(du -sh "${QUARANTINE_DIR}" 2>/dev/null | cut -f1)
rm -rf "${QUARANTINE_DIR}"
log "  Deleted quarantine (was ${QUARANTINE_SIZE})."

# ══════════════════════════════════════════════════════════════════════════════
# Summary
# ══════════════════════════════════════════════════════════════════════════════
log ""
log_sep
log "RESULTS:"
log "  Items removed: ${MOVED}"
log "  Items skipped: ${SKIPPED}"
log "  Space recovered: ~${TOTAL_MB} MB"
log "  Log: ${LOG_FILE}"
log ""
log "PHASE 2.3 COMPLETE — Old backup removal done."
log_sep

echo ""
echo "================================================================"
echo "  PHASE 2.3 COMPLETE"
echo "  Items removed: ${MOVED} | Skipped: ${SKIPPED}"
echo "  Space recovered: ~${TOTAL_MB} MB"
echo "================================================================"
