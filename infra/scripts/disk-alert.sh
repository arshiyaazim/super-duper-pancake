#!/bin/bash
# Disk + RAM health check — sends WhatsApp alert if disk > THRESHOLD%
# Runs hourly via cron

THRESHOLD=80
DISK_USAGE=$(df / | awk 'NR==2 {print $5}' | tr -d '%')

if [ "$DISK_USAGE" -lt "$THRESHOLD" ]; then
  exit 0
fi

# ── Disk details ─────────────────────────────────────────────────────────────
DISK_USED=$(df -h / | awk 'NR==2 {print $3}')
DISK_TOTAL=$(df -h / | awk 'NR==2 {print $2}')
DISK_FREE=$(df -h / | awk 'NR==2 {print $4}')

# ── RAM details ──────────────────────────────────────────────────────────────
RAM_TOTAL=$(free -h | awk '/^Mem:/ {print $2}')
RAM_USED=$(free -h  | awk '/^Mem:/ {print $3}')
RAM_FREE=$(free -h  | awk '/^Mem:/ {print $4}')
RAM_PCT=$(free | awk '/^Mem:/ {printf "%.0f", $3/$2*100}')

# ── Top 5 space consumers ─────────────────────────────────────────────────────
TOP5=$(du -sh /home/azim/.[^.]* /home/azim/*/ 2>/dev/null | sort -rh | head -5 | awk '{print "  " $1 "  " $2}')

# ── Git garbage check ─────────────────────────────────────────────────────────
GIT_GARBAGE=$(git -C /home/azim count-objects -vH 2>/dev/null | grep "size-garbage" | awk '{print $2, $3}')
TMP_PACKS=$(find /home/azim/.git/objects/pack/ -name "tmp_pack_*" 2>/dev/null | wc -l)

# ── Safe cleanup suggestions ──────────────────────────────────────────────────
SUGGESTIONS=""
if [ "$TMP_PACKS" -gt "0" ]; then
  SUGGESTIONS="${SUGGESTIONS}
  rm -f ~/.git/objects/pack/tmp_pack_*   [git garbage: ${TMP_PACKS} files]"
fi
if [ -d /home/azim/.aitk ]; then
  AITK_SIZE=$(du -sh /home/azim/.aitk 2>/dev/null | cut -f1)
  SUGGESTIONS="${SUGGESTIONS}
  rm -rf ~/.aitk   [AI toolkit cache: ${AITK_SIZE}]"
fi
if [ -d /home/azim/.vscode-server/data/CachedExtensionVSIXs ]; then
  SUGGESTIONS="${SUGGESTIONS}
  rm -rf ~/.vscode-server/data/CachedExtensionVSIXs"
fi
SUGGESTIONS="${SUGGESTIONS}
  npm cache clean --force
  docker system prune -a --volumes"

# ── Build message ─────────────────────────────────────────────────────────────
MESSAGE="🚨 *Server Disk Alert — ${DISK_USAGE}% Full*

💾 *Disk*
  Used : ${DISK_USED} / ${DISK_TOTAL}
  Free : ${DISK_FREE}

🧠 *RAM — ${RAM_PCT}%*
  Used : ${RAM_USED} / ${RAM_TOTAL}
  Free : ${RAM_FREE}

📂 *Top Space Users:*
${TOP5}

🗑️ *Git Garbage:* ${GIT_GARBAGE:-none}

🛠️ *Safe to delete:*
${SUGGESTIONS}

Run these commands on the server (ssh) to free space."

# ── Send via Bridge2 (OPS) ────────────────────────────────────────────────────
BRIDGE_URL="http://localhost:8081"
OWNER_JID="8801880446111@s.whatsapp.net"
JSON_MSG=$(python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))' <<< "$MESSAGE")

RESPONSE=$(curl -s -o /tmp/wa_alert_resp.txt -w "%{http_code}" \
  -X POST "${BRIDGE_URL}/api/send" \
  -H "Content-Type: application/json" \
  -d "{\"recipient\": \"${OWNER_JID}\", \"message\": ${JSON_MSG}}")

echo "[$(date)] Disk alert fired: ${DISK_USAGE}% used | bridge_http=${RESPONSE}" >> /home/azim/logs/disk-alert.log
