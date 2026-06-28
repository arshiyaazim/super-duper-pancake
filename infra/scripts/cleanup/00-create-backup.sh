#!/usr/bin/env bash
# ==============================================================================
# 00-create-backup.sh — Pre-cleanup safety snapshot
# Creates a backup of all critical configs before any cleanup action is taken.
# Safe to re-run. Does NOT modify any live file.
# ==============================================================================
set -euo pipefail

# ── Config ─────────────────────────────────────────────────────────────────────
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_ROOT="/home/azim/backups/cleanup-${TIMESTAMP}"
LOG_FILE="/home/azim/cleanup-log-${TIMESTAMP}.txt"
MIN_FREE_GB=5   # Refuse to run if less than this many GB are free

# ── Helpers ────────────────────────────────────────────────────────────────────
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"; }
die() { log "FATAL: $*"; exit 1; }
header() { log ""; log "══════════════════════════════════════════"; log "  $*"; log "══════════════════════════════════════════"; }

# ── Trap: on any failure, report where we stopped ──────────────────────────────
trap 'log "ERROR: script exited unexpectedly at line ${LINENO}. Backup may be incomplete."' ERR

# ══════════════════════════════════════════════════════════════════════════════
# STEP 0 — Disk space pre-check
# ══════════════════════════════════════════════════════════════════════════════
header "STEP 0 — Disk space pre-check"

FREE_GB=$(df -BG /home/azim | awk 'NR==2{gsub("G",""); print $4}')
log "Free disk space: ${FREE_GB} GB (minimum required: ${MIN_FREE_GB} GB)"

if (( FREE_GB < MIN_FREE_GB )); then
    die "Insufficient free space (${FREE_GB} GB < ${MIN_FREE_GB} GB). Aborting backup."
fi
log "Disk space OK."

# ══════════════════════════════════════════════════════════════════════════════
# STEP 1 — Create backup directory structure
# ══════════════════════════════════════════════════════════════════════════════
header "STEP 1 — Creating backup directory: ${BACKUP_ROOT}"

mkdir -p \
    "${BACKUP_ROOT}/git-config" \
    "${BACKUP_ROOT}/env-files" \
    "${BACKUP_ROOT}/systemd-services" \
    "${BACKUP_ROOT}/db-dumps" \
    "${BACKUP_ROOT}/manifests"

log "Directory structure created."

# ══════════════════════════════════════════════════════════════════════════════
# STEP 2 — Backup git config and refs
# ══════════════════════════════════════════════════════════════════════════════
header "STEP 2 — Backing up git config + refs"

GIT_DIR="/home/azim/.git"

if [[ -f "${GIT_DIR}/config" ]]; then
    cp "${GIT_DIR}/config" "${BACKUP_ROOT}/git-config/git-config"
    log "Backed up: .git/config"
else
    log "WARNING: .git/config not found — skipping"
fi

if [[ -d "${GIT_DIR}/refs" ]]; then
    cp -r "${GIT_DIR}/refs" "${BACKUP_ROOT}/git-config/refs"
    log "Backed up: .git/refs/"
fi

if [[ -f "${GIT_DIR}/HEAD" ]]; then
    cp "${GIT_DIR}/HEAD" "${BACKUP_ROOT}/git-config/HEAD"
    log "Backed up: .git/HEAD"
fi

if [[ -f "${GIT_DIR}/packed-refs" ]]; then
    cp "${GIT_DIR}/packed-refs" "${BACKUP_ROOT}/git-config/packed-refs"
    log "Backed up: .git/packed-refs"
fi

# Record current branch and remote info
{
    echo "=== Current branch ==="
    git -C /home/azim branch --show-current 2>/dev/null || echo "detached HEAD or unknown"
    echo ""
    echo "=== Remote ==="
    git -C /home/azim remote -v 2>/dev/null || echo "no remotes"
    echo ""
    echo "=== Last 10 commits ==="
    git -C /home/azim log --oneline -10 2>/dev/null || echo "no commits"
    echo ""
    echo "=== .git pack files ==="
    ls -lh /home/azim/.git/objects/pack/ 2>/dev/null || echo "no pack files"
} > "${BACKUP_ROOT}/git-config/git-state-snapshot.txt"
log "Backed up git state snapshot."

# ══════════════════════════════════════════════════════════════════════════════
# STEP 3 — Backup all .env files
# ══════════════════════════════════════════════════════════════════════════════
header "STEP 3 — Backing up .env files"

ENV_PATHS=(
    "/home/azim/core/.env"
    "/home/azim/agent/.env"
    "/home/azim/ai-call-platform/.env"
    "/home/azim/locationwhere-backend/.env"
    "/home/azim/github-model/.env"
    "/home/azim/.env"
    "/home/azim/fazle-agent-dev/.env"
    "/home/azim/fazle-diagnostic-agent/.env"
)

for env_path in "${ENV_PATHS[@]}"; do
    if [[ -f "$env_path" ]]; then
        # Create subdirectory matching original path structure
        dest_dir="${BACKUP_ROOT}/env-files$(dirname "$env_path")"
        mkdir -p "$dest_dir"
        cp "$env_path" "${dest_dir}/"
        log "Backed up: ${env_path}"
    else
        log "Skipping (not found): ${env_path}"
    fi
done

# ══════════════════════════════════════════════════════════════════════════════
# STEP 4 — Backup systemd service files
# ══════════════════════════════════════════════════════════════════════════════
header "STEP 4 — Backing up systemd service files"

SERVICES=(
    "fazle-core.service"
    "fazle-agent.service"
    "whatsapp-bridge.service"
    "whatsapp-bridge2.service"
    "media-processor.service"
)

for svc in "${SERVICES[@]}"; do
    svc_path="/etc/systemd/system/${svc}"
    if [[ -f "$svc_path" ]]; then
        cp "$svc_path" "${BACKUP_ROOT}/systemd-services/${svc}"
        log "Backed up: ${svc_path}"
    else
        log "Skipping (not found): ${svc_path}"
    fi
done

# Also save current service status
for svc in "${SERVICES[@]}"; do
    systemctl status "$svc" --no-pager 2>/dev/null \
        > "${BACKUP_ROOT}/systemd-services/${svc%.service}-status.txt" \
        || true
done
log "Saved service status snapshots."

# ══════════════════════════════════════════════════════════════════════════════
# STEP 5 — Backup database schemas (non-blocking, no table locks)
# ══════════════════════════════════════════════════════════════════════════════
header "STEP 5 — Backing up PostgreSQL schemas (schema-only, no data)"

PG_CONTAINER="ai-postgres"

if docker ps --filter "name=${PG_CONTAINER}" --filter "status=running" --format "{{.Names}}" | grep -q "${PG_CONTAINER}"; then
    # Schema-only dump of all databases (fast, no row locks)
    for db in postgres waerp fazle_test; do
        dump_file="${BACKUP_ROOT}/db-dumps/${db}-schema-${TIMESTAMP}.sql"
        docker exec "${PG_CONTAINER}" pg_dump -U postgres --schema-only "$db" \
            > "$dump_file" 2>/dev/null \
            && log "Schema dump: ${db} → $(basename "$dump_file") ($(du -sh "$dump_file" | cut -f1))" \
            || log "WARNING: Schema dump failed for ${db}"
    done
    # Also dump the agent and locationwhere schemas
    docker exec "${PG_CONTAINER}" pg_dump -U postgres --schema-only -n agent postgres \
        > "${BACKUP_ROOT}/db-dumps/postgres-agent-schema-${TIMESTAMP}.sql" 2>/dev/null \
        && log "Schema dump: postgres/agent schema" \
        || log "WARNING: agent schema dump failed"
    docker exec "${PG_CONTAINER}" pg_dump -U postgres --schema-only -n locationwhere postgres \
        > "${BACKUP_ROOT}/db-dumps/postgres-locationwhere-schema-${TIMESTAMP}.sql" 2>/dev/null \
        && log "Schema dump: postgres/locationwhere schema" \
        || log "WARNING: locationwhere schema dump failed"
else
    log "WARNING: ai-postgres container not running — skipping DB backup"
fi

# ══════════════════════════════════════════════════════════════════════════════
# STEP 6 — Create deletion manifests
# ══════════════════════════════════════════════════════════════════════════════
header "STEP 6 — Creating deletion manifests"

# Phase 1 targets (tmp_pack files)
cat > "${BACKUP_ROOT}/manifests/phase1-targets.txt" << 'EOF'
# Phase 1 — Git tmp_pack files (17 GB potential savings)
# These are INTERRUPTED git pack operations. They are NOT valid pack files.
# Deleting them does not affect any git history or any working tree file.
/home/azim/.git/objects/pack/tmp_pack_v3r9Fa
/home/azim/.git/objects/pack/tmp_pack_DKU8rF

# Phase 1 — bridge3.log (bridge3 is NOT running)
/home/azim/bridges/mcp/logs/bridge3.log
EOF
log "Created: phase1-targets.txt"

# Phase 2 targets (safe deletes)
cat > "${BACKUP_ROOT}/manifests/phase2-targets.txt" << 'EOF'
# Phase 2 — Confirmed-unused directories and files
# VERIFY each item independently before deleting.

# Old agent backup (Apr 2026 snapshot, replaced by /home/azim/agent/)
/home/azim/system-agent-backup-1777241194/

# Dev copy of agent (production = /home/azim/agent/)
/home/azim/fazle-agent-dev/

# Empty fazle-core dir (real code at /home/azim/core/)
/home/azim/fazle-core/

# Old whatsapp-erp archive
/home/azim/_archive_2026-04-25/

# Old safepoint backups
/home/azim/backups/fazle-dashboard_backup_20260419/
/home/azim/backups/safepoint_2026-05-06/

# Home-root orphan files
/home/azim/autoreply.log
/home/azim/PHASE_FINAL_OBSERVE_20260523_062246.log
/home/azim/docker-compose.yml.bak.20260602_040333
/home/azim/docker-compose.yml.bak.20260602_050656
/home/azim/bridges/mcp/autoreply.log

# Stopped Docker container
# docker rm fazle-brain
EOF
log "Created: phase2-targets.txt"

# Phase 3 targets (require verification)
cat > "${BACKUP_ROOT}/manifests/phase3-targets.txt" << 'EOF'
# Phase 3 — Larger cleanups. EACH requires independent verification before deleting.

# Unused React frontend build (NOT the active iamazim.com website)
# VERIFY: grep -r "frontend" /etc/nginx/sites-enabled/ /home/azim/core/.env
/home/azim/frontend/

# Home-root orphan node_modules
# VERIFY: cat /home/azim/package.json (check if any script uses these)
/home/azim/node_modules/

# Home-root flat module stubs (production = /home/azim/core/modules/)
/home/azim/modules/

# Old call platform framework (predates current architecture, last modified 2026-04-21)
# VERIFY: grep -r "ai-call-platform" /etc/systemd/system/ /etc/nginx/
/home/azim/ai-call-platform/

# Archive deprecated content
/home/azim/archive/

# github-model node_modules (source code kept, just removing node_modules)
/home/azim/github-model/node_modules/
EOF
log "Created: phase3-targets.txt"

# ══════════════════════════════════════════════════════════════════════════════
# STEP 7 — Verify existence of Phase 1 targets and record sizes
# ══════════════════════════════════════════════════════════════════════════════
header "STEP 7 — Recording current sizes of cleanup targets"

{
    echo "=== Phase 1 targets ==="
    for f in \
        "/home/azim/.git/objects/pack/tmp_pack_v3r9Fa" \
        "/home/azim/.git/objects/pack/tmp_pack_DKU8rF" \
        "/home/azim/bridges/mcp/logs/bridge3.log"
    do
        if [[ -e "$f" ]]; then
            echo "EXISTS   $(du -sh "$f" 2>/dev/null | cut -f1)   $f"
        else
            echo "MISSING  0        $f"
        fi
    done

    echo ""
    echo "=== Phase 2 targets ==="
    for f in \
        "/home/azim/system-agent-backup-1777241194" \
        "/home/azim/fazle-agent-dev" \
        "/home/azim/fazle-core" \
        "/home/azim/_archive_2026-04-25" \
        "/home/azim/backups/fazle-dashboard_backup_20260419" \
        "/home/azim/backups/safepoint_2026-05-06" \
        "/home/azim/autoreply.log" \
        "/home/azim/PHASE_FINAL_OBSERVE_20260523_062246.log" \
        "/home/azim/docker-compose.yml.bak.20260602_040333" \
        "/home/azim/docker-compose.yml.bak.20260602_050656" \
        "/home/azim/bridges/mcp/autoreply.log"
    do
        if [[ -e "$f" ]]; then
            echo "EXISTS   $(du -sh "$f" 2>/dev/null | cut -f1)   $f"
        else
            echo "MISSING  0        $f"
        fi
    done

    echo ""
    echo "=== Phase 3 targets ==="
    for f in \
        "/home/azim/frontend" \
        "/home/azim/node_modules" \
        "/home/azim/modules" \
        "/home/azim/ai-call-platform" \
        "/home/azim/archive" \
        "/home/azim/github-model/node_modules"
    do
        if [[ -e "$f" ]]; then
            echo "EXISTS   $(du -sh "$f" 2>/dev/null | cut -f1)   $f"
        else
            echo "MISSING  0        $f"
        fi
    done
} > "${BACKUP_ROOT}/manifests/target-sizes.txt"
log "Recorded target sizes → manifests/target-sizes.txt"
cat "${BACKUP_ROOT}/manifests/target-sizes.txt" | tee -a "$LOG_FILE"

# ══════════════════════════════════════════════════════════════════════════════
# STEP 8 — Generate md5 checksums of backed-up files
# ══════════════════════════════════════════════════════════════════════════════
header "STEP 8 — Computing checksums"

find "${BACKUP_ROOT}" -type f ! -name "checksums.md5" | sort | xargs md5sum \
    > "${BACKUP_ROOT}/checksums.md5" 2>/dev/null
log "Checksums written to: ${BACKUP_ROOT}/checksums.md5"

# ══════════════════════════════════════════════════════════════════════════════
# STEP 9 — Write restore instructions
# ══════════════════════════════════════════════════════════════════════════════
header "STEP 9 — Writing restore instructions"

cat > "${BACKUP_ROOT}/RESTORE-INSTRUCTIONS.md" << RESTORE_EOF
# Restore Instructions — Backup ${TIMESTAMP}

This backup was created by: 00-create-backup.sh
Backup location: ${BACKUP_ROOT}
Created at: $(date)

## What is backed up

- \`git-config/\` — .git/config, refs, HEAD, packed-refs, state snapshot
- \`env-files/\` — all .env files mirroring original directory structure
- \`systemd-services/\` — all active systemd unit files + status snapshots
- \`db-dumps/\` — schema-only SQL dumps of all databases
- \`manifests/\` — lists of files targeted for deletion + recorded sizes

## How to restore

### Restore git config
\`\`\`bash
cp ${BACKUP_ROOT}/git-config/git-config /home/azim/.git/config
cp -r ${BACKUP_ROOT}/git-config/refs /home/azim/.git/refs
\`\`\`

### Restore .env files
\`\`\`bash
cp ${BACKUP_ROOT}/env-files/home/azim/core/.env /home/azim/core/.env
cp ${BACKUP_ROOT}/env-files/home/azim/agent/.env /home/azim/agent/.env
# ... repeat for each .env as needed
\`\`\`

### Restore a systemd service
\`\`\`bash
sudo cp ${BACKUP_ROOT}/systemd-services/fazle-core.service /etc/systemd/system/
sudo systemctl daemon-reload
\`\`\`

### Restore database schema
\`\`\`bash
docker exec -i ai-postgres psql -U postgres < ${BACKUP_ROOT}/db-dumps/postgres-schema-${TIMESTAMP}.sql
\`\`\`

## Verify checksum integrity
\`\`\`bash
md5sum -c ${BACKUP_ROOT}/checksums.md5
\`\`\`
RESTORE_EOF

log "Restore instructions written to: ${BACKUP_ROOT}/RESTORE-INSTRUCTIONS.md"

# ══════════════════════════════════════════════════════════════════════════════
# STEP 10 — Final summary
# ══════════════════════════════════════════════════════════════════════════════
header "STEP 10 — Summary"

BACKUP_SIZE=$(du -sh "${BACKUP_ROOT}" 2>/dev/null | cut -f1)
log "Backup complete."
log "  Location : ${BACKUP_ROOT}"
log "  Size     : ${BACKUP_SIZE}"
log "  Log file : ${LOG_FILE}"
log ""
log "Next steps:"
log "  1. Review manifests/target-sizes.txt to confirm targets"
log "  2. Run 00-verify-state.sh to record current service state"
log "  3. Run 01-phase1-git-tmppack.sh to remove tmp_pack files"
log "  4. Run 01-phase1-bridge3-log.sh to truncate bridge3.log"
log ""
log "To restore from this backup, see: ${BACKUP_ROOT}/RESTORE-INSTRUCTIONS.md"

echo ""
echo "================================================================"
echo "  BACKUP COMPLETE: ${BACKUP_ROOT}"
echo "  Size: ${BACKUP_SIZE}"
echo "================================================================"
