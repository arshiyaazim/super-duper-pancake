#!/usr/bin/env bash
# ==============================================================================
# 00-verify-state.sh — Pre/post cleanup state verification
# Records the state of all active services and produces a JSON health report.
# Safe to run any time. Does NOT modify any live file.
# ==============================================================================
set -euo pipefail

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="/home/azim/cleanup-pre-state.json"
LOG_FILE="/home/azim/cleanup-log-${TIMESTAMP}.txt"

# Accept optional label argument: "pre" (default) or "post"
LABEL="${1:-pre}"
if [[ "$LABEL" == "post" ]]; then
    REPORT_FILE="/home/azim/cleanup-post-state.json"
fi

# ── Helpers ────────────────────────────────────────────────────────────────────
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"; }
header() { log ""; log "── $* ──"; }

# JSON builder helpers (pure bash — no jq dependency for writing)
json_str()  { printf '"%s"' "$(echo "$1" | sed 's/"/\\"/g')"; }
json_bool() { [[ "$1" == "true" || "$1" == "0" ]] && echo "true" || echo "false"; }

# ── Probe functions ─────────────────────────────────────────────────────────────

check_service() {
    local name="$1"
    systemctl is-active "$name" 2>/dev/null || echo "inactive"
}

check_http() {
    local url="$1"
    local timeout="${2:-10}"
    local code
    code=$(curl -sk --max-time "$timeout" -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    echo "$code"
}

check_http_local() {
    local url="$1"
    local timeout="${2:-5}"
    local code
    code=$(curl -s --max-time "$timeout" -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    echo "$code"
}

docker_status() {
    local name="$1"
    local result
    result=$(docker ps --filter "name=^${name}$" --format "{{.Status}}" 2>/dev/null | head -1 || true)
    echo "${result:-not-found}" | tr -d '\n\r'
}

docker_health() {
    local name="$1"
    local result
    result=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$name" 2>/dev/null || echo "not-found")
    echo "${result}" | tr -d '\n\r'
}

# ══════════════════════════════════════════════════════════════════════════════
header "Systemd services"
# ══════════════════════════════════════════════════════════════════════════════

SVC_FAZLE_CORE=$(check_service "fazle-core")
SVC_FAZLE_AGENT=$(check_service "fazle-agent")
SVC_BRIDGE1=$(check_service "whatsapp-bridge")
SVC_BRIDGE2=$(check_service "whatsapp-bridge2")
SVC_MEDIA=$(check_service "media-processor")
SVC_NGINX=$(check_service "nginx")
SVC_DOCKER=$(check_service "docker")

log "fazle-core:        $SVC_FAZLE_CORE"
log "fazle-agent:       $SVC_FAZLE_AGENT"
log "whatsapp-bridge:   $SVC_BRIDGE1"
log "whatsapp-bridge2:  $SVC_BRIDGE2"
log "media-processor:   $SVC_MEDIA"
log "nginx:             $SVC_NGINX"
log "docker:            $SVC_DOCKER"

# ══════════════════════════════════════════════════════════════════════════════
header "Local HTTP health endpoints"
# ══════════════════════════════════════════════════════════════════════════════

HTTP_FAZLE_CORE=$(check_http_local "http://localhost:8200/health")
HTTP_FAZLE_AGENT=$(check_http_local "http://localhost:8300/health")
HTTP_MEDIA=$(check_http_local "http://localhost:8090/health")
HTTP_LOCATIONWHERE=$(check_http_local "http://localhost:8310/health")
HTTP_OPENWEBUI=$(check_http_local "http://localhost:8501/" "5")

log "fazle-core  (:8200/health):     HTTP $HTTP_FAZLE_CORE"
log "fazle-agent (:8300/health):     HTTP $HTTP_FAZLE_AGENT"
log "media-proc  (:8090/health):     HTTP $HTTP_MEDIA"
log "locationwhere (:8310/health):   HTTP $HTTP_LOCATIONWHERE"
log "open-webui  (:8501/):           HTTP $HTTP_OPENWEBUI"

# Capture health body for fazle-core (for deeper inspection)
FAZLE_CORE_HEALTH_BODY=$(curl -s --max-time 5 "http://localhost:8200/health" 2>/dev/null || echo '{"status":"unreachable"}')

# ══════════════════════════════════════════════════════════════════════════════
header "Public HTTPS endpoints"
# ══════════════════════════════════════════════════════════════════════════════

HTTPS_FAZLE=$(check_http "https://fazle.iamazim.com/health" "15")
HTTPS_API=$(check_http "https://api.iamazim.com/health" "15")
HTTPS_LOCATIONWHERE=$(check_http "https://locationwhere.iamazim.com/health" "15")
HTTPS_CHAT=$(check_http "https://chat.iamazim.com/" "15")
HTTPS_IAMAZIM=$(check_http "https://iamazim.com/" "15")

log "fazle.iamazim.com/health:         HTTP $HTTPS_FAZLE"
log "api.iamazim.com/health:           HTTP $HTTPS_API"
log "locationwhere.iamazim.com/health: HTTP $HTTPS_LOCATIONWHERE"
log "chat.iamazim.com/:                HTTP $HTTPS_CHAT"
log "iamazim.com/:                     HTTP $HTTPS_IAMAZIM"

# ══════════════════════════════════════════════════════════════════════════════
header "Docker container health"
# ══════════════════════════════════════════════════════════════════════════════

DOCK_POSTGRES_STATUS=$(docker_status "ai-postgres")
DOCK_POSTGRES_HEALTH=$(docker_health "ai-postgres")
DOCK_REDIS_STATUS=$(docker_status "ai-redis")
DOCK_REDIS_HEALTH=$(docker_health "ai-redis")
DOCK_OLLAMA_STATUS=$(docker_status "ollama")
DOCK_OLLAMA_HEALTH=$(docker_health "ollama")
DOCK_OPENWEBUI_STATUS=$(docker_status "open-webui")
DOCK_OPENWEBUI_HEALTH=$(docker_health "open-webui")
DOCK_QDRANT_STATUS=$(docker_status "qdrant")
DOCK_QDRANT_HEALTH=$(docker_health "qdrant")
DOCK_MINIO_STATUS=$(docker_status "minio")
DOCK_MINIO_HEALTH=$(docker_health "minio")
DOCK_GRAFANA_STATUS=$(docker_status "grafana")
DOCK_GRAFANA_HEALTH=$(docker_health "grafana")
DOCK_BRAIN_STATUS=$(docker_status "fazle-brain")
DOCK_BRAIN_HEALTH=$(docker_health "fazle-brain")

log "ai-postgres:  status=$DOCK_POSTGRES_STATUS  health=$DOCK_POSTGRES_HEALTH"
log "ai-redis:     status=$DOCK_REDIS_STATUS  health=$DOCK_REDIS_HEALTH"
log "ollama:       status=$DOCK_OLLAMA_STATUS  health=$DOCK_OLLAMA_HEALTH"
log "open-webui:   status=$DOCK_OPENWEBUI_STATUS  health=$DOCK_OPENWEBUI_HEALTH"
log "qdrant:       status=$DOCK_QDRANT_STATUS  health=$DOCK_QDRANT_HEALTH"
log "minio:        status=$DOCK_MINIO_STATUS  health=$DOCK_MINIO_HEALTH"
log "grafana:      status=$DOCK_GRAFANA_STATUS  health=$DOCK_GRAFANA_HEALTH"
log "fazle-brain:  status=$DOCK_BRAIN_STATUS  health=$DOCK_BRAIN_HEALTH"

# ══════════════════════════════════════════════════════════════════════════════
header "Database connectivity"
# ══════════════════════════════════════════════════════════════════════════════

DB_READY=$(docker exec ai-postgres pg_isready -U postgres 2>/dev/null | tail -1 || echo "pg_isready failed")
log "pg_isready: $DB_READY"

DB_TABLE_COUNT=$(docker exec ai-postgres psql -U postgres -tAc \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public'" \
    2>/dev/null || echo "0")
log "public schema table count: $DB_TABLE_COUNT"

DB_INCIDENTS=$(docker exec ai-postgres psql -U postgres -tAc \
    "SELECT COUNT(*) FROM agent.incidents" \
    2>/dev/null || echo "unknown")
log "agent.incidents (open): $DB_INCIDENTS"

# ══════════════════════════════════════════════════════════════════════════════
header "Disk usage"
# ══════════════════════════════════════════════════════════════════════════════

DISK_TOTAL=$(df -BG /dev/sda1 | awk 'NR==2{gsub("G",""); print $2}')
DISK_USED=$(df -BG /dev/sda1 | awk 'NR==2{gsub("G",""); print $3}')
DISK_FREE=$(df -BG /dev/sda1 | awk 'NR==2{gsub("G",""); print $4}')
DISK_PCT=$(df /dev/sda1 | awk 'NR==2{print $5}' | tr -d '%')

log "Disk total: ${DISK_TOTAL}G  used: ${DISK_USED}G  free: ${DISK_FREE}G  pct: ${DISK_PCT}%"

# Specific targets
GIT_SIZE=$(du -sB1 /home/azim/.git 2>/dev/null | awk '{print $1}' || echo "0")
TMPPACK1_SIZE=0
TMPPACK2_SIZE=0
BRIDGE3_LOG_SIZE=0

[[ -f "/home/azim/.git/objects/pack/tmp_pack_v3r9Fa" ]] && \
    TMPPACK1_SIZE=$(stat -c%s "/home/azim/.git/objects/pack/tmp_pack_v3r9Fa" 2>/dev/null || echo "0")
[[ -f "/home/azim/.git/objects/pack/tmp_pack_DKU8rF" ]] && \
    TMPPACK2_SIZE=$(stat -c%s "/home/azim/.git/objects/pack/tmp_pack_DKU8rF" 2>/dev/null || echo "0")
[[ -f "/home/azim/bridges/mcp/logs/bridge3.log" ]] && \
    BRIDGE3_LOG_SIZE=$(stat -c%s "/home/azim/bridges/mcp/logs/bridge3.log" 2>/dev/null || echo "0")

log ".git total size: $(numfmt --to=iec-i --suffix=B "$GIT_SIZE" 2>/dev/null || echo "${GIT_SIZE}B")"
log "tmp_pack_v3r9Fa: $(numfmt --to=iec-i --suffix=B "$TMPPACK1_SIZE" 2>/dev/null || echo "${TMPPACK1_SIZE}B")"
log "tmp_pack_DKU8rF: $(numfmt --to=iec-i --suffix=B "$TMPPACK2_SIZE" 2>/dev/null || echo "${TMPPACK2_SIZE}B")"
log "bridge3.log:     $(numfmt --to=iec-i --suffix=B "$BRIDGE3_LOG_SIZE" 2>/dev/null || echo "${BRIDGE3_LOG_SIZE}B")"

# ══════════════════════════════════════════════════════════════════════════════
header "RAM usage"
# ══════════════════════════════════════════════════════════════════════════════

RAM_TOTAL=$(free -m | awk '/^Mem:/{print $2}')
RAM_USED=$(free -m | awk '/^Mem:/{print $3}')
RAM_FREE=$(free -m | awk '/^Mem:/{print $4}')
RAM_AVAILABLE=$(free -m | awk '/^Mem:/{print $7}')
SWAP_TOTAL=$(free -m | awk '/^Swap:/{print $2}')
SWAP_USED=$(free -m | awk '/^Swap:/{print $3}')

log "RAM total: ${RAM_TOTAL}MB  used: ${RAM_USED}MB  free: ${RAM_FREE}MB  available: ${RAM_AVAILABLE}MB"
log "Swap total: ${SWAP_TOTAL}MB  used: ${SWAP_USED}MB"

# ══════════════════════════════════════════════════════════════════════════════
header "Ollama model inventory"
# ══════════════════════════════════════════════════════════════════════════════

OLLAMA_MODELS=$(curl -s --max-time 5 "http://172.22.0.7:11434/api/tags" 2>/dev/null \
    | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    models = [{'name': m['name'], 'size_bytes': m['size']} for m in d.get('models', [])]
    print(json.dumps(models))
except:
    print('[]')
" 2>/dev/null || echo "[]")

log "Ollama models: $OLLAMA_MODELS"

# ══════════════════════════════════════════════════════════════════════════════
# Build JSON report
# ══════════════════════════════════════════════════════════════════════════════
header "Writing JSON report → $REPORT_FILE"

# Determine overall health
ALL_OK=true
[[ "$SVC_FAZLE_CORE" != "active" ]] && ALL_OK=false
[[ "$SVC_FAZLE_AGENT" != "active" ]] && ALL_OK=false
[[ "$SVC_BRIDGE1" != "active" ]] && ALL_OK=false
[[ "$SVC_BRIDGE2" != "active" ]] && ALL_OK=false
[[ "$SVC_MEDIA" != "active" ]] && ALL_OK=false
[[ "$HTTP_FAZLE_CORE" != "200" ]] && ALL_OK=false
[[ "$DOCK_POSTGRES_HEALTH" != "healthy" ]] && ALL_OK=false

python3 - << PYEOF
import json, datetime

report = {
    "report_type": "${LABEL}",
    "generated_at": "$(date -Iseconds)",
    "overall_healthy": ("${ALL_OK}" == "true"),
    "systemd_services": {
        "fazle-core":        "${SVC_FAZLE_CORE}",
        "fazle-agent":       "${SVC_FAZLE_AGENT}",
        "whatsapp-bridge":   "${SVC_BRIDGE1}",
        "whatsapp-bridge2":  "${SVC_BRIDGE2}",
        "media-processor":   "${SVC_MEDIA}",
        "nginx":             "${SVC_NGINX}",
        "docker":            "${SVC_DOCKER}"
    },
    "local_http": {
        "fazle_core_8200":      "${HTTP_FAZLE_CORE}",
        "fazle_agent_8300":     "${HTTP_FAZLE_AGENT}",
        "media_processor_8090": "${HTTP_MEDIA}",
        "locationwhere_8310":   "${HTTP_LOCATIONWHERE}",
        "open_webui_8501":      "${HTTP_OPENWEBUI}"
    },
    "https_endpoints": {
        "fazle_iamazim_com":        "${HTTPS_FAZLE}",
        "api_iamazim_com":          "${HTTPS_API}",
        "locationwhere_iamazim_com":"${HTTPS_LOCATIONWHERE}",
        "chat_iamazim_com":         "${HTTPS_CHAT}",
        "iamazim_com":              "${HTTPS_IAMAZIM}"
    },
    "docker_containers": {
        "ai-postgres":  {"status": "${DOCK_POSTGRES_STATUS}", "health": "${DOCK_POSTGRES_HEALTH}"},
        "ai-redis":     {"status": "${DOCK_REDIS_STATUS}",    "health": "${DOCK_REDIS_HEALTH}"},
        "ollama":       {"status": "${DOCK_OLLAMA_STATUS}",   "health": "${DOCK_OLLAMA_HEALTH}"},
        "open-webui":   {"status": "${DOCK_OPENWEBUI_STATUS}","health": "${DOCK_OPENWEBUI_HEALTH}"},
        "qdrant":       {"status": "${DOCK_QDRANT_STATUS}",   "health": "${DOCK_QDRANT_HEALTH}"},
        "minio":        {"status": "${DOCK_MINIO_STATUS}",    "health": "${DOCK_MINIO_HEALTH}"},
        "grafana":      {"status": "${DOCK_GRAFANA_STATUS}",  "health": "${DOCK_GRAFANA_HEALTH}"},
        "fazle-brain":  {"status": "${DOCK_BRAIN_STATUS}",    "health": "${DOCK_BRAIN_HEALTH}"}
    },
    "database": {
        "pg_isready": "${DB_READY}",
        "public_table_count": "${DB_TABLE_COUNT}",
        "agent_open_incidents": "${DB_INCIDENTS}"
    },
    "disk": {
        "device": "/dev/sda1",
        "total_gb": ${DISK_TOTAL},
        "used_gb":  ${DISK_USED},
        "free_gb":  ${DISK_FREE},
        "used_pct": ${DISK_PCT},
        "git_dir_bytes":       ${GIT_SIZE},
        "tmp_pack_v3r9Fa_bytes": ${TMPPACK1_SIZE},
        "tmp_pack_DKU8rF_bytes": ${TMPPACK2_SIZE},
        "bridge3_log_bytes":   ${BRIDGE3_LOG_SIZE}
    },
    "ram_mb": {
        "total":     ${RAM_TOTAL},
        "used":      ${RAM_USED},
        "free":      ${RAM_FREE},
        "available": ${RAM_AVAILABLE},
        "swap_total": ${SWAP_TOTAL},
        "swap_used":  ${SWAP_USED}
    },
    "ollama_models": ${OLLAMA_MODELS},
    "fazle_core_health": $(echo '${FAZLE_CORE_HEALTH_BODY}' | python3 -c "import sys,json; d=json.load(sys.stdin); print(json.dumps(d))" 2>/dev/null || echo '{}')
}

with open("${REPORT_FILE}", "w") as f:
    json.dump(report, f, indent=2)

print("JSON report written successfully.")
PYEOF

log "Report written to: $REPORT_FILE"

# ══════════════════════════════════════════════════════════════════════════════
# Final console summary
# ══════════════════════════════════════════════════════════════════════════════

echo ""
echo "================================================================"
echo "  STATE VERIFICATION COMPLETE (${LABEL})"
echo "  Report: ${REPORT_FILE}"
echo "================================================================"
echo ""
echo "  SERVICES:"
printf "  %-30s %s\n" "fazle-core"        "$SVC_FAZLE_CORE"
printf "  %-30s %s\n" "fazle-agent"       "$SVC_FAZLE_AGENT"
printf "  %-30s %s\n" "whatsapp-bridge"   "$SVC_BRIDGE1"
printf "  %-30s %s\n" "whatsapp-bridge2"  "$SVC_BRIDGE2"
printf "  %-30s %s\n" "media-processor"   "$SVC_MEDIA"
echo ""
echo "  HTTP ENDPOINTS:"
printf "  %-30s HTTP %s\n" "fazle-core (local)"    "$HTTP_FAZLE_CORE"
printf "  %-30s HTTP %s\n" "fazle-agent (local)"   "$HTTP_FAZLE_AGENT"
printf "  %-30s HTTP %s\n" "fazle.iamazim.com"      "$HTTPS_FAZLE"
printf "  %-30s HTTP %s\n" "api.iamazim.com"        "$HTTPS_API"
printf "  %-30s HTTP %s\n" "locationwhere"          "$HTTPS_LOCATIONWHERE"
echo ""
echo "  DISK: ${DISK_USED}G / ${DISK_TOTAL}G (${DISK_PCT}%)"
echo "  RAM:  ${RAM_USED}MB used / ${RAM_TOTAL}MB total"
echo ""
echo "  Overall healthy: $ALL_OK"
echo "================================================================"

# Exit non-zero if critical services are down, so callers can detect failure
if [[ "$ALL_OK" != "true" ]]; then
    log "WARNING: One or more health checks failed. Review report before proceeding with cleanup."
    exit 1
fi

log "All checks passed."
exit 0
