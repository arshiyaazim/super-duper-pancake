# VPS Audit — Non-fazle-core Reply/Agent Service Disable Log
Date: 2026-06-09
Performed by: Claude Code (automated audit)

## Objective
Ensure only `fazle-core.service` can generate and send WhatsApp/social replies.
Disable all other services that independently read WhatsApp messages, generate replies,
or send messages without going through fazle-core's draft approval flow.

## Safety Rule
Do NOT disable: fazle-core, whatsapp-bridge, whatsapp-bridge2, media-processor,
PostgreSQL, Redis, Ollama, nginx, locationwhere-backend.

---

## Services Audited

| Service | Pre-audit State | Classification | Action |
|---|---|---|---|
| fazle-core.service | ACTIVE/RUNNING | KEEP — this IS the core | none |
| whatsapp-bridge.service | ACTIVE/RUNNING | KEEP — bridge1 transport | none |
| whatsapp-bridge2.service | ACTIVE/RUNNING | KEEP — bridge2 transport | none |
| media-processor.service | ACTIVE/RUNNING | KEEP — OCR/STT required by core | none |
| locationwhere-backend (pm2) | RUNNING | KEEP — unrelated location app | none |
| fazle-agent.service | ACTIVE/RUNNING | KEEP — management API (dry-run, no WA send) | none |
| fazle-social-auto-reply.service | ACTIVE/RUNNING | **DISABLE** — generates+sends social replies | stop+disable+mask |
| facebook_supervisor_agent (orphan PID 2673) | RUNNING | **KILL** — deprecated, no systemd unit | kill |
| fazle-recruitment-agent.service | disabled/enabled | **MASK** — live send agent, RECRUITMENT_AGENT_SEND_ENABLED=true | mask |
| fazle-recruitment-bridge3.service | disabled/enabled | **MASK** — bridge3 recruitment send | mask |
| whatsapp-autoreply.service | disabled/enabled | **MASK** — Al-Aqsa WhatsApp Auto-Reply Daemon | mask |
| whatsapp-bridge3.service | disabled/enabled | **MASK** — bridge3 transport (no core dependency) | mask |
| fazle-agent-dev.service | disabled/inactive | KEEP-DISABLED — dev testing agent | none |
| github-model/connector.js | NOT RUNNING | Dormant — last run April 2026, HTTP 500s, no restart mechanism | none |

## Cron Jobs Audited

| Entry | Classification | Action |
|---|---|---|
| @reboot run_bridge3_stack.sh | **DANGEROUS** — script missing but would start recruitment agent | remove |
| 0 * * * * sync_bridge2_cron.sh | Dead — script missing | remove |
| 5 * * * * sync_bridge1_cron.sh | Dead — script missing | remove |
| 0 3 * * * backup.sh | Safe — backup only | keep |
| 0 3 * * * certbot renew | Safe — TLS renewal | keep |
| 0 3 * * 0 docker-cleanup.sh | Safe — cleanup | keep |
| 30 3 * * * daily_backup.sh | Safe — backup | keep |
| 0 2 * * * auto_maintain.py | Safe — maintenance | keep |

## Actions Taken

### COMPLETED (no sudo required)

1. **facebook_supervisor_agent orphan process killed**
   - PID 2673: `/home/azim/.venv/bin/python /home/azim/facebook_supervisor_agent/service_runtime.py`
   - Was running from `/home/azim/archive/deprecated/20260602_facebook_supervisor_agent` (deprecated)
   - `kill -TERM 2673` — confirmed dead: `ps` no longer shows process
   - No systemd unit managing it → no mask needed

2. **Crontab cleaned** (3 entries removed)
   - Removed: `@reboot /home/azim/external_recruitment_agent/run_bridge3_stack.sh` (DANGEROUS — would auto-start recruitment agents on reboot; script is missing)
   - Removed: `0 * * * * sync_bridge2_cron.sh` (dead — script missing)
   - Removed: `5 * * * * sync_bridge1_cron.sh` (dead — script missing)
   - Original crontab backed up at `/tmp/fazle-vps-audit/crontab-backup.txt`

### PENDING (requires `sudo bash /home/azim/disable-conflicting-services.sh`)

The following 5 service operations require sudo password. A ready-to-run script has been
created at `/home/azim/disable-conflicting-services.sh`.

1. `systemctl stop fazle-social-auto-reply` (PID 3423 still RUNNING — Restart=on-failure prevents simple kill)
2. `systemctl disable fazle-social-auto-reply`
3. `systemctl mask fazle-social-auto-reply`
4. `systemctl mask whatsapp-autoreply`
5. `systemctl mask fazle-recruitment-agent`
6. `systemctl mask fazle-recruitment-bridge3`
7. `systemctl mask whatsapp-bridge3`

**Run this command to complete the disable:**
```
sudo bash /home/azim/disable-conflicting-services.sh
```

### VERIFICATION AFTER SUDO SCRIPT

After running the sudo script, confirm:
- `systemctl is-active fazle-core` → active
- `systemctl is-active fazle-social-auto-reply` → inactive
- `systemctl is-enabled fazle-social-auto-reply` → masked
- `systemctl is-enabled whatsapp-autoreply` → masked
- `systemctl is-enabled fazle-recruitment-agent` → masked
- `systemctl is-enabled fazle-recruitment-bridge3` → masked
- `systemctl is-enabled whatsapp-bridge3` → masked

