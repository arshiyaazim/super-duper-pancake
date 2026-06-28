#!/usr/bin/env bash
# ==============================================================================
# 01-phase2-git-tmppack.sh — Remove interrupted git tmp_pack files
# Target: ~12 GB recovery from /home/azim/.git/objects/pack/
# Safety: quarantine-first pattern — move before delete, fsck before final rm
# ==============================================================================
set -euo pipefail

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="/home/azim/cleanup-log-${TIMESTAMP}.txt"
QUARANTINE_DIR="/home/azim/cleanup-quarantine/git-tmppack"
GIT_DIR="/home/azim/.git"
PACK_DIR="${GIT_DIR}/objects/pack"
TMPPACK1="${PACK_DIR}/tmp_pack_v3r9Fa"
TMPPACK2="${PACK_DIR}/tmp_pack_DKU8rF"

# ── Helpers ────────────────────────────────────────────────────────────────────
log()    { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"; }
log_sep(){ log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; }

trap 'log "ERROR: script exited unexpectedly at line ${LINENO}. Log: ${LOG_FILE}"' ERR

log_sep
log "PHASE 2.1: Git tmp_pack Removal"
log "Target: ~12 GB recovery from interrupted git pack operations"
log "Log: ${LOG_FILE}"
log_sep

# ══════════════════════════════════════════════════════════════════════════════
# PRE-CHECK 1 — Verify at least one target exists
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "PRE-CHECK 1: Confirming target files exist..."

FOUND_ANY=false
for f in "$TMPPACK1" "$TMPPACK2"; do
    if [[ -f "$f" ]]; then
        SIZE=$(du -sh "$f" 2>/dev/null | cut -f1)
        log "  EXISTS: $f  ($SIZE)"
        FOUND_ANY=true
    else
        log "  MISSING (already gone or never existed): $f"
    fi
done

if [[ "$FOUND_ANY" == "false" ]]; then
    log "No tmp_pack files found. Nothing to clean. Exiting cleanly."
    log_sep
    exit 0
fi

# ══════════════════════════════════════════════════════════════════════════════
# PRE-CHECK 2 — Files must not be open by any process
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "PRE-CHECK 2: Checking that tmp_pack files are not open by any process..."

IN_USE=false
for f in "$TMPPACK1" "$TMPPACK2"; do
    if [[ -f "$f" ]]; then
        if lsof "$f" 2>/dev/null | grep -q .; then
            log "  ERROR: $f is currently open by a process:"
            lsof "$f" 2>/dev/null | tee -a "$LOG_FILE"
            IN_USE=true
        fi
    fi
done

if [[ "$IN_USE" == "true" ]]; then
    log "ABORT: One or more tmp_pack files are in use."
    log "  Wait for the process to finish, then re-run this script."
    log "  To identify: lsof | grep tmp_pack"
    exit 1
fi
log "  Files are not in use. Safe to proceed."

# ══════════════════════════════════════════════════════════════════════════════
# PRE-CHECK 3 — No git operation currently running
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "PRE-CHECK 3: Checking for active git operations (lock files)..."

LOCK_FILES=("${GIT_DIR}/index.lock" "${GIT_DIR}/MERGE_HEAD" "${GIT_DIR}/CHERRY_PICK_HEAD" "${GIT_DIR}/REBASE_HEAD")
for lock in "${LOCK_FILES[@]}"; do
    if [[ -f "$lock" ]]; then
        log "  ERROR: Git lock file exists: $lock"
        log "  A git operation may be in progress. Aborting."
        exit 1
    fi
done
log "  No git lock files found. Safe to proceed."

# ══════════════════════════════════════════════════════════════════════════════
# PRE-CHECK 4 — Record sizes before
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "PRE-CHECK 4: Recording current .git directory size..."
# du -sh can take time on 29GB .git — run with brief status message
log "  (This may take 10-30 seconds for a large .git directory...)"
GIT_SIZE_BEFORE=$(du -sh "${GIT_DIR}" 2>/dev/null | cut -f1)
DISK_FREE_BEFORE=$(df -BG /home/azim | awk 'NR==2{print $4}' | tr -d 'G')

log "  .git size before:  ${GIT_SIZE_BEFORE}"
log "  Disk free before:  ${DISK_FREE_BEFORE} GB"

TMPPACK1_SIZE="0"
TMPPACK2_SIZE="0"
[[ -f "$TMPPACK1" ]] && TMPPACK1_SIZE=$(du -sh "$TMPPACK1" 2>/dev/null | cut -f1)
[[ -f "$TMPPACK2" ]] && TMPPACK2_SIZE=$(du -sh "$TMPPACK2" 2>/dev/null | cut -f1)
log "  tmp_pack_v3r9Fa:   ${TMPPACK1_SIZE}"
log "  tmp_pack_DKU8rF:   ${TMPPACK2_SIZE}"

# ══════════════════════════════════════════════════════════════════════════════
# PRE-CHECK 5 — git fsck BEFORE moving files
# (tmp_pack files themselves may cause expected dangling-object warnings)
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "PRE-CHECK 5: Running git fsck BEFORE cleanup (may take 1-3 minutes)..."
log "  NOTE: 'dangling commit/blob/tree' messages are EXPECTED and non-fatal."
log "  Only 'error:' or 'missing' messages indicate real corruption."

FSCK_BEFORE_LOG="${LOG_FILE%.txt}-fsck-before.txt"
FSCK_BEFORE_OK=true

git -C /home/azim fsck --no-progress 2>&1 | tee "$FSCK_BEFORE_LOG" | tee -a "$LOG_FILE" || true

# Check for actual errors (not just dangling warnings)
if grep -qE "^error:|^missing|broken link|corrupt" "$FSCK_BEFORE_LOG" 2>/dev/null; then
    log "  WARNING: git fsck found errors BEFORE cleanup."
    log "  This may indicate pre-existing corruption, not caused by tmp_pack files."
    log "  Logging the errors and proceeding cautiously (tmp_pack files are the likely cause)."
    grep -E "^error:|^missing|broken link|corrupt" "$FSCK_BEFORE_LOG" | head -20 | tee -a "$LOG_FILE" || true
else
    log "  git fsck before: CLEAN (only dangling objects, if any — expected)"
fi

# ══════════════════════════════════════════════════════════════════════════════
# STEP 1 — Create quarantine directory
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "STEP 1: Creating quarantine directory..."
mkdir -p "${QUARANTINE_DIR}"
log "  Quarantine: ${QUARANTINE_DIR}"

# Write rollback script into quarantine dir immediately
cat > "${QUARANTINE_DIR}/ROLLBACK.sh" << ROLLBACK
#!/usr/bin/env bash
# Auto-generated rollback script — restores quarantined tmp_pack files
echo "Restoring quarantined tmp_pack files to ${PACK_DIR}/"
for f in ${QUARANTINE_DIR}/tmp_pack_*; do
    [ -f "\$f" ] && mv -v "\$f" "${PACK_DIR}/"
done
echo "Restore complete. Run: git -C /home/azim fsck --no-progress"
ROLLBACK
chmod +x "${QUARANTINE_DIR}/ROLLBACK.sh"
log "  Rollback script created: ${QUARANTINE_DIR}/ROLLBACK.sh"

# ══════════════════════════════════════════════════════════════════════════════
# STEP 2 — Move tmp_pack files to quarantine (not delete yet)
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "STEP 2: Moving tmp_pack files to quarantine (NOT deleting yet)..."

MOVED_COUNT=0
for f in "$TMPPACK1" "$TMPPACK2"; do
    if [[ -f "$f" ]]; then
        fname=$(basename "$f")
        log "  Moving: $f → ${QUARANTINE_DIR}/${fname}"
        mv "$f" "${QUARANTINE_DIR}/${fname}"
        log "  Moved successfully."
        (( MOVED_COUNT++ )) || true
    fi
done
log "  Total files moved: ${MOVED_COUNT}"

# ══════════════════════════════════════════════════════════════════════════════
# STEP 3 — git fsck AFTER move — this is the integrity gate
# ══════════════════════════════════════════════════════════════════════════════
log ""
log "STEP 3: Running git fsck AFTER moving files (integrity gate)..."
log "  (This may take 1-3 minutes for a large repository...)"

FSCK_AFTER_LOG="${LOG_FILE%.txt}-fsck-after.txt"
FSCK_AFTER_OK=true

git -C /home/azim fsck --no-progress 2>&1 | tee "$FSCK_AFTER_LOG" | tee -a "$LOG_FILE" || true

if grep -qE "^error:|^missing|broken link|corrupt" "$FSCK_AFTER_LOG" 2>/dev/null; then
    log "  ERROR: git fsck found errors AFTER moving tmp_pack files."
    log "  This is unexpected. Rolling back immediately..."
    FSCK_AFTER_OK=false
else
    log "  git fsck after: CLEAN — repository integrity confirmed."
fi

# ══════════════════════════════════════════════════════════════════════════════
# STEP 4 — Decision: delete or rollback
# ══════════════════════════════════════════════════════════════════════════════
log ""
log_sep

if [[ "$FSCK_AFTER_OK" == "true" ]]; then
    # ── Success path: delete from quarantine ──────────────────────────────────
    log "STEP 4 (SUCCESS PATH): Deleting quarantine directory..."
    log "  Removing: ${QUARANTINE_DIR}"
    rm -rf "${QUARANTINE_DIR}"
    log "  Quarantine deleted."

    # Record final sizes
    log ""
    log "Recording final .git directory size..."
    log "  (This may take 10-30 seconds...)"
    GIT_SIZE_AFTER=$(du -sh "${GIT_DIR}" 2>/dev/null | cut -f1)
    DISK_FREE_AFTER=$(df -BG /home/azim | awk 'NR==2{print $4}' | tr -d 'G')
    DISK_RECOVERED=$(( DISK_FREE_AFTER - DISK_FREE_BEFORE ))

    log_sep
    log "RESULTS:"
    log "  .git size before:  ${GIT_SIZE_BEFORE}"
    log "  .git size after:   ${GIT_SIZE_AFTER}"
    log "  Disk free before:  ${DISK_FREE_BEFORE} GB"
    log "  Disk free after:   ${DISK_FREE_AFTER} GB"
    log "  Disk recovered:    ~${DISK_RECOVERED} GB"
    log ""
    log "PHASE 2.1 COMPLETE — Git tmp_pack removal successful."
    log "Log: ${LOG_FILE}"
    log_sep

    echo ""
    echo "================================================================"
    echo "  PHASE 2.1 COMPLETE"
    echo "  .git: ${GIT_SIZE_BEFORE} → ${GIT_SIZE_AFTER}"
    echo "  Disk recovered: ~${DISK_RECOVERED} GB"
    echo "================================================================"

else
    # ── Failure path: restore from quarantine ─────────────────────────────────
    log "STEP 4 (ROLLBACK PATH): git fsck failed — restoring quarantine files..."
    bash "${QUARANTINE_DIR}/ROLLBACK.sh" 2>&1 | tee -a "$LOG_FILE"
    log ""
    log "ROLLBACK COMPLETE. Files restored to ${PACK_DIR}/"
    log "Repository is in the same state as before this script ran."
    log ""
    log "PHASE 2.1 FAILED — Manual investigation required."
    log "  Review fsck output: ${FSCK_AFTER_LOG}"
    log "  Review full log:    ${LOG_FILE}"
    log_sep
    exit 1
fi
