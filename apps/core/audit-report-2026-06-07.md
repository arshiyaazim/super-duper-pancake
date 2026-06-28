# VPS "iamazim" — Comprehensive System Audit Report
**Date:** 2026-06-07
**Auditor:** Claude Code (Read-Only Mode — no files modified, no services restarted)
**VPS IP:** 5.189.131.48
**Scope:** Full VPS ecosystem — containers, systemd services, apps, modules, databases, frontends, backends, .env files, git repos, nginx, overlaps, conflicts, gaps, broken/working workflows

---

> **Note:** A prior audit exists at `/home/azim/core/audit-report-2026-06-01.md` (with a 2026-06-04 addendum).
> This report is a **fully independent re-audit** from scratch, reflecting the live state on 2026-06-07.
> Where the prior report conflicts with current observed state, this report takes precedence.

---

## 1. Executive Summary

The VPS hosts a multi-layered production system for **Al-Aqsa Security Service & Trading Centre** (Chittagong, Bangladesh), structured around two distinct product tracks:

1. **Fazle Core** — WhatsApp AI operations backend (escort/HR/payroll/recruitment workflows)
2. **LocationWhere** — Employee GPS monitoring SaaS app
3. **AI Call Platform** — Planned AI voice agent SaaS (partially deployed infrastructure)

**System Health at Audit Time:**
- 14 of 15 Docker containers running; 1 exited cleanly (fazle-brain)
- 8 of 8 systemd services running; 0 dead/failed (includes 6 application services + nginx + docker; excluding other standard system services)
- 1 standalone Node.js process (locationwhere-backend) running outside systemd
- **CRITICAL BREAKAGE:** Fazle Core `status: critical` — bridge SQLite databases not found, inbound message ingestion fully broken
- **Configuration Conflict:** `AUTO_REPLY_ENABLED=false` in live `.env` (safe-mode), contradicting README which states it is `true`
- Disk: 58% used (112G / 194G), free 83G — healthy
- RAM: 8.4G used / 23G total, 14G available — healthy
- Load: 2.52 (11-day uptime) — moderate

---

## 2. Docker Containers — Full Inventory

### 2A. Container Status Summary

| # | Container Name | Image | Status | Uptime | Ports (exposed) |
|---|---|---|---|---|---|
| 1 | `ollama` | ollama/ollama:latest | ✅ Running (healthy) | 6 days | 127.0.0.1:11434 (internal only) |
| 2 | `open-webui` | ghcr.io/open-webui/open-webui:main | ✅ Running (healthy) | 4 days | 127.0.0.1:8501→8080 |
| 3 | `code-server` | codercom/code-server:latest | ✅ Running (healthy) | 11 days | 127.0.0.1:8443→8080 |
| 4 | `grafana` | grafana/grafana:11.4.0 | ✅ Running (healthy) | 11 days | 127.0.0.1:3030 |
| 5 | `promtail` | grafana/promtail:3.2.1 | ✅ Running | 11 days | — |
| 6 | `cadvisor` | gcr.io/cadvisor/cadvisor:v0.49.1 | ✅ Running (healthy) | 11 days | 8080 (internal) |
| 7 | `loki` | grafana/loki:3.2.1 | ✅ Running (healthy) | 11 days | 3100 (internal) |
| 8 | `prometheus` | prom/prometheus:v2.55.0 | ✅ Running (healthy) | 11 days | 9090 (internal) |
| 9 | `ai-postgres` | pgvector/pgvector:pg17 | ✅ Running (healthy) | 11 days | 5432 (internal) |
| 10 | `minio` | minio/minio:... | ✅ Running (healthy) | 11 days | 9000 (internal) |
| 11 | `qdrant` | qdrant/qdrant:v1.17.0 | ✅ Running (healthy) | 11 days | 6333–6334 (internal) |
| 12 | `node-exporter` | prom/node-exporter:v1.8.2 | ✅ Running (healthy) | 11 days | 9100 (internal) |
| 13 | `ai-redis` | redis:8.0.2-alpine | ✅ Running (healthy) | 11 days | 6379 (internal) |
| 14 | `fazle-otel-collector` | otel/opentelemetry-collector-contrib:0.96.0 | ✅ Running | 11 days | 0.0.0.0:4317–4318 |
| 15 | `fazle-brain` | fazle-ai-fazle-brain | ❌ Exited (0) | 11 days ago | — |

**Total: 15 containers — 14 Running, 1 Exited**

### 2B. Independent vs. Dependent Containers

**Independent containers (no `depends_on`, self-contained):**

| Container | Why Independent |
|---|---|
| `ollama` | Pure AI model server; no dependencies |
| `ai-postgres` | Core database; all others depend on it, not vice versa |
| `ai-redis` | Cache layer; no service dependencies |
| `minio` | Object storage; no service dependencies |
| `qdrant` | Vector DB; no service dependencies |
| `loki` | Log aggregator; receives from promtail |
| `node-exporter` | Host metrics exporter; no dependencies |
| `cadvisor` | Container metrics exporter; no dependencies |
| `code-server` | Standalone VS Code IDE |

**Dependent containers (require other containers to function):**

| Container | Depends On | Network |
|---|---|---|
| `open-webui` | `ollama` (reads models) | ai-network + app-network |
| `grafana` | `prometheus`, `loki` (datasources) | monitoring-network |
| `prometheus` | `cadvisor`, `node-exporter` (scrape targets) | monitoring-network + app-network |
| `promtail` | `loki` (push destination) | monitoring-network |
| `fazle-otel-collector` | `ai-postgres`, `ai-redis` (telemetry routing) | ai-network + app-network + db-network |
| `fazle-brain` | `ai-postgres`, `ai-redis` (was defined in fazle-ai compose with `depends_on`) | — (exited) |

**Summary:**
- **9 containers are fully independent**
- **5 containers are dependent** on others (grafana, prometheus, promtail, open-webui, fazle-otel-collector)
- **1 container exited** (fazle-brain — dependency-requiring container from fazle-ai compose, exited cleanly code 0)

### 2C. Docker Networks

| Network | Connected Containers | Purpose |
|---|---|---|
| `ai-network` (172.22.0.0/16) | ollama (172.22.0.7), open-webui (172.22.0.2), fazle-otel-collector (172.22.0.3) | AI model routing |
| `monitoring-network` (172.21.0.0/16) | promtail, prometheus, node-exporter, grafana, loki, cadvisor | Metrics/logging stack |
| `app-network` (172.19.0.0/16) | prometheus (172.19.0.2), fazle-otel-collector (172.19.0.4), ai-postgres, minio, qdrant, ai-redis, open-webui | Application cross-connect |
| `db-network` (172.20.0.0/16) | ai-postgres (172.20.0.3), minio (172.20.0.2), qdrant (172.20.0.4), ai-redis (172.20.0.5), open-webui (172.20.0.6) | Database isolation |
| `al-aqsa_openwebui-bridge` | open-webui (primary home), temporarily used by ollama for model pulls | External model pull network |
| `code-server_default` | code-server | VS Code isolated network |

### 2D. Docker Volumes (all external, named)

| Volume | Used By |
|---|---|
| `ai-call-platform_grafana_data` | grafana |
| `ai-call-platform_loki_data` | loki |
| `ai-call-platform_minio-data` | minio |
| `ai-call-platform_ollama_data` | ollama |
| `ai-call-platform_postgres_data` | ai-postgres |
| `ai-call-platform_prometheus_data` | prometheus |
| `ai-call-platform_qdrant_data` | qdrant |
| `ai-call-platform_redis_data` | ai-redis |
| `al-aqsa_open-webui-data` | open-webui |
| `code_server_data` | code-server |
| `code_server_root_data` | code-server |

### 2E. Planned Containers NOT Running

The `fazle-ai` compose file (`/home/azim/ai-call-platform/fazle-ai/docker-compose.yaml`) defines **13 services** that are currently **not running** (only `fazle-brain` attempted and exited):

- `fazle-api` (API gateway)
- `fazle-brain` (core reasoning engine — exited)
- `fazle-memory` (memory service)
- `fazle-voice` (voice profile only)
- `fazle-ui` (frontend)
- `fazle-learning-engine`
- `fazle-queue`
- `fazle-workers`
- `fazle-autonomy-engine`
- `fazle-tool-engine`
- `fazle-workflow-engine`
- `fazle-social-engine`
- `fazle-wbom` (WhatsApp bridge on mobile)

---

## 3. Systemd Services — Full Inventory

| Service | Description | Status | Port | Path |
|---|---|---|---|---|
| `fazle-core.service` | WhatsApp AI Backend (main app) | ✅ Active/Running (2 days) | 8200 | `/home/azim/core` |
| `whatsapp-bridge.service` | WA Bridge 1 — HR (8801958122300) | ✅ Active/Running (11 days) | 8082 | `/home/azim/whatsapp-mcp/whatsapp-bridge` |
| `whatsapp-bridge2.service` | WA Bridge 2 — OPS (8801880446111) | ✅ Active/Running (11 days) | 8081 | WD: `/home/azim/whatsapp2` (MISSING) |
| `fazle-agent.service` | System Agent (dry-run mode) | ✅ Active/Running (6 days) | 8300 | `/home/azim/system-agent` |
| `media-processor.service` | STT/OCR/PDF Processor | ✅ Active/Running (5 days) | 8090 | `/home/azim/shared/media/media-processor` |
| `fazle-social-auto-reply.service` | Legacy Social Auto-Reply Daemon | ✅ Active/Running (11 days) | — | `/home/azim/core/venv/bin/python` |
| `nginx.service` | Reverse Proxy | ✅ Active/Running | 80/443 | System |
| `docker.service` | Docker Engine | ✅ Active/Running | — | System |

**Plus standard system services:** `ssh`, `fail2ban`, `cron`, `rsyslog`, `systemd-*`

**Non-systemd process (running outside service management):**

| Process | Command | Port | PID File |
|---|---|---|---|
| `locationwhere-backend` | `node /home/azim/locationwhere-backend/dist/app.js` | 8310 | `/home/azim/locationwhere-backend.pid` |

> **Gap:** `locationwhere-backend` is running as a bare process (likely started manually or via PM2 at some point), but there is no systemd service for it. If VPS reboots, this process will NOT restart automatically.

---

## 4. Application Inventory

### 4A. APP 1 — Fazle Core (Primary Production App)

| Item | Value |
|---|---|
| **Path** | `/home/azim/core` |
| **Type** | FastAPI (Python 3.10) |
| **Service** | `fazle-core.service` |
| **Port** | 8200 |
| **DB** | PostgreSQL (`waerp` DB on `ai-postgres:5432`) |
| **LLM** | Ollama `qwen2.5:3b` (container `ollama` at 172.22.0.7:11434) |
| **Git remote** | `origin → https://github.com/arshiyaazim/fazle-core` |
| **Health** | `GET /health → {"status":"critical"}` |

**Production Capability: ~55%**
- ✅ FastAPI server running (PID 2170714, 79.9 MB RAM)
- ✅ PostgreSQL connection OK
- ✅ Ollama OK (`qwen2.5:3b`, `qwen3:8b`, `qwen3:14b` loaded)
- ✅ Outbound HTTP to both bridge `/api/send-status` OK (200)
- ✅ Disk OK (57% used, 88 GB free)
- ✅ Memory OK (14.8 GB available)
- ❌ **CRITICAL: Bridge1 SQLite not found** — `/home/azim/whatsapp1/store/messages.db` does not exist (store dir has individual LID contact subdirs, no combined messages.db)
- ❌ **CRITICAL: Bridge2 SQLite not found** — `/home/azim/whatsapp2/store/messages.db` — entire `/home/azim/whatsapp2/` directory is MISSING
- ❌ Inbound message ingestion: fully stopped (`last_message_id: null` on both pollers)
- ❌ `AUTO_REPLY_ENABLED=false` — system in safe mode, no messages sent
- ❌ `OUTBOUND_ENABLED=false` — outbound queue also disabled
- ❌ Overall health probe returns `status: critical` (no inbound path working)

> **Root Cause of Critical Status:** WhatsApp bridges use individual per-contact file storage in their store directories (`whatsapp1/store/{contact_lid}/`), but fazle-core's `bridge_poller` expects a single `messages.db` SQLite file at the same path. These are different storage formats. Bridge2's working directory (`/home/azim/whatsapp2/`) doesn't exist at all — it may have been renamed to `/home/azim/bridges/bridge2/` during a migration that was never completed for the service unit file.

---

### 4B. APP 2 — LocationWhere Backend

| Item | Value |
|---|---|
| **Path** | `/home/azim/locationwhere-backend` |
| **Type** | Node.js / TypeScript / Express / Prisma |
| **Service** | No systemd service — bare process |
| **Port** | 8310 |
| **DB** | PostgreSQL (`locationwhere` schema on same `ai-postgres`) |
| **Cache** | Redis DB 15 |
| **Health** | `GET /health → {"status":"UP","firebaseAdminInitialized":true}` |
| **Git remote** | `origin → https://github.com/arshiyaazim/fazle-core` (⚠️ WRONG REMOTE) |

**Production Capability: ~65%**
- ✅ Node.js process running (PID 1978392, 102 MB RAM)
- ✅ Health endpoint returns UP
- ✅ Firebase Admin initialized
- ✅ PostgreSQL connected (via `ai-postgres` container, `locationwhere` schema)
- ✅ Redis connected (DB 15, optional — `REDIS_OPTIONAL=true`)
- ✅ Nginx serves frontend at `locationwhere.iamazim.com` → backend at `/api/`
- ✅ Frontend static files at `/var/www/locationwhere.iamazim.com/` (has `downloads/`, `assets/`, `index.html`)
- ⚠️ No systemd service — will not survive VPS reboot
- ⚠️ Firebase service account JSON at `/home/azim/locationwhere-backend/firebase-service-account.json` (hardcoded path, security risk)
- ⚠️ AWS S3 not configured (`AWS_S3_BUCKET=""`)
- ⚠️ SMS gateway not configured (`SMS_SID=""`, `SMS_DOMAIN=""`)
- ⚠️ Wrong git remote (points to fazle-core repo, not a locationwhere repo)

**Modules (9):** `auth`, `employee`, `location`, `sim`, `call`, `device`, `alert`, `report`, `gateway`

---

### 4C. APP 3 — iamazim.com Company Website (Static)

| Item | Value |
|---|---|
| **Path** | `/home/azim/iamazim-web` (source) → `/var/www/iamazim.com/` (deployed) |
| **Type** | Static HTML/CSS |
| **Service** | nginx (static file serve) |
| **Hostname** | `iamazim.com`, `www.iamazim.com` |

**Production Capability: ~90%**
- ✅ Serving via nginx
- ✅ SSL certificate active
- ✅ Full company website deployed (fixed 2026-06-04)
- ✅ Legal pages: `privacy.html`, `terms.html`, `contact.html`
- ⚠️ No git remote / version control for deployed files
- ⚠️ Source in `/home/azim/iamazim-web/` and deployed in `/var/www/iamazim.com/` — dual-location, manual deploy process

---

### 4D. APP 4 — Open WebUI (Private AI Chat)

| Item | Value |
|---|---|
| **Container** | `open-webui` |
| **Hostname** | `chat.iamazim.com` |
| **Backend** | Ollama (`ollama` container) |
| **Networks** | ai-network + al-aqsa_openwebui-bridge + app-network |

**Production Capability: ~95%**
- ✅ Running 4 days
- ✅ Healthy
- ✅ Connected to Ollama
- ✅ SSL via nginx
- Minor: Uptime shorter than other containers (suggests recent restart)

---

### 4E. APP 5 — VS Code Server (Dev IDE)

| Item | Value |
|---|---|
| **Container** | `code-server` |
| **Hostname** | `vscode.iamazim.com` |
| **Port** | 127.0.0.1:8443 |

**Production Capability: ~95%**
- ✅ Running 11 days
- ✅ Healthy
- ✅ SSL via nginx

---

### 4F. APP 6 — Grafana Monitoring Stack

| Item | Value |
|---|---|
| **Container** | `grafana` |
| **URL** | `iamazim.com/grafana/` (sub-path) |
| **Port** | 127.0.0.1:3030 |
| **Stack** | Grafana + Prometheus + Loki + Promtail + cAdvisor + Node-Exporter |

**Production Capability: ~85%**
- ✅ All 6 monitoring containers running
- ✅ Grafana healthy
- ✅ Prometheus scraping cadvisor + node-exporter
- ✅ Loki receiving logs from promtail
- ⚠️ No nginx site-enabled config for `grafana.iamazim.com` subdomain (served from main domain subpath only)
- ⚠️ Grafana configured for Telegram alerts but `TELEGRAM_BOT_TOKEN` may be empty

---

### 4G. APP 7 — AI Call Platform / Fazle AI (Planned SaaS)

| Item | Value |
|---|---|
| **Path** | `/home/azim/ai-call-platform` |
| **Git remotes** | `origin → MuradulAzim/ai-call-platform`, `upstream → arshiyaazim/ai-call-platform` |
| **Planned Services** | 13 microservices (fazle-api, fazle-brain, fazle-memory, etc.) |

**Production Capability: ~15%**
- ✅ Infrastructure layer deployed and running: PostgreSQL, Redis, MinIO, Qdrant, Ollama (shared with Fazle Core)
- ✅ `fazle-brain` was built (image exists) but exited cleanly (code 0) 11 days ago
- ❌ 12 of 13 application services never started
- ❌ No active build for most services (`fazle-system/` subdirs contain source but no live containers)
- ❌ LiveKit not configured (`livekit.iamazim.com` nginx config exists but not symlinked)
- ⚠️ Two GitHub accounts both have forks — coordination risk

---

### 4H. APP 8 — Fazle Agent (System Operator)

| Item | Value |
|---|---|
| **Path** | `/home/azim/fazle-agent-dev` (source) + `/home/azim/system-agent` (deployed) |
| **Service** | `fazle-agent.service` |
| **Port** | 8300 |
| **Health** | `{"status":"ok","open_incidents":4863,"dry_run":false,"internet_allowed":false}` |

**Production Capability: ~50%**
- ✅ Service running (6 days)
- ✅ Health endpoint OK
- ✅ 4,863 incidents tracked
- ⚠️ Service description says "dry-run" but health API reports `dry_run: false` — inconsistency
- ⚠️ `internet_allowed: false` — agent cannot call external APIs
- ⚠️ 4,863 open incidents suggests backlog not being resolved

---

### 4I. APP 9 — Media Processor

| Item | Value |
|---|---|
| **Path** | `/home/azim/shared/media/media-processor` |
| **Service** | `media-processor.service` |
| **Port** | 8090 |

**Production Capability: ~85%**
- ✅ Running 5 days (737 MB RAM — STT/OCR models loaded)
- ✅ Health endpoint OK
- ✅ Connected to both WhatsApp bridges via `MEDIA_PROCESSOR_URL`
- ⚠️ High memory (737 MB) — second heaviest process after Docker

---

### 4J. APP 10 — Fazle Social Auto-Reply (Legacy)

| Item | Value |
|---|---|
| **Service** | `fazle-social-auto-reply.service` |
| **Path** | `/home/azim/core/modules/social_auto_reply/` |
| **Config** | `SOCIAL_AUTO_REPLY_SINGLE_ENGINE=false` |

**Production Capability: ~40%**
- ✅ Service running (11 days)
- ⚠️ Legacy pipeline — `SOCIAL_AUTO_REPLY_SINGLE_ENGINE=false` means it runs alongside the new engine (potential overlap)
- ⚠️ Recruitement autoreply is disabled (`RECRUITMENT_AUTOREPLY_ENABLED=false`)

---

### 4K. APP 11 — Fazle Payroll Engine

| Item | Value |
|---|---|
| **Path** | `/home/azim/fazle-payroll-engine` (standalone repo) + `/home/azim/core/modules/fazle_payroll_engine/` (in core) |
| **Git remote** | `fazle-payroll-engine → arshiyaazim/fazle-payroll-engine` |
| **Status** | Runs inside `fazle-core.service` as a module |

**Production Capability: ~65%**
- ✅ Loaded as module inside fazle-core
- ✅ Has its own FPE routes, hsync, gapscan routines
- ❌ FPE GAP scan failing: SQLite errors on both bridge stores (same root cause as bridge polling)
- ❌ FPE hsync failing: same SQLite error
- ⚠️ Duplicate code risk — standalone repo + embedded module

---

### 4L. APP 12 — Fazle Diagnostic Agent

| Item | Value |
|---|---|
| **Path** | `/home/azim/fazle-diagnostic-agent` |
| **Type** | Python / Microsoft Agent Framework |
| **Status** | Not running (no active service/process) |

**Production Capability: ~20%**
- ✅ Code present, README documented, 7 diagnostic tools implemented
- ❌ No running service — tool only, launched on demand
- ❌ No systemd service unit

---

## 5. Module Inventory

### 5A. Two Parallel Modules Directories — CONFLICT

| Directory | Module Count | In Active Service? |
|---|---|---|
| `/home/azim/core/modules/` | **45 modules** | ✅ YES — `fazle-core.service` imports from here |
| `/home/azim/modules/` | **30 modules** | ❌ NO — appears to be an older/parallel copy |

**Modules present in `/home/azim/core/modules/` but NOT in `/home/azim/modules/` (15 extra in core):**

| Module | Status in Core |
|---|---|
| `accountant_summary` | Active |
| `admin_employees` | Active (router registered in main.py) |
| `admin_transactions` | Active (router registered in main.py) |
| `contact_sync` | Dormant (0 callers per prior audit) |
| `conversation_layer` | Shadow-test only |
| `escort_roster` | Active (router registered in main.py) |
| `fazle_payroll_engine` | Active |
| `media_normalization` | Dormant |
| `message_archive` | Recovery scripts only |
| `number_identity` | Unknown |
| `payment_correction` | Dormant |
| `recruitment_ai` | Active (qwen-backed recruitment) |
| `reply_templates` | Shadow-only |
| `reviewed_reply_memory` | Active |
| `social_auto_reply` | Active |

> **Gap:** `/home/azim/modules/` is stale and should be archived or removed to avoid confusion.

### 5B. Active Modules in `/home/azim/core/modules/`

| Module | Status | Purpose |
|---|---|---|
| `bridge_poller` | ✅ Active | Polls bridge SQLite stores (BROKEN — stores missing) |
| `message_router` | ✅ Active | Central routing + admin command detection |
| `admin_commands` | ✅ Active | APPROVE/REJECT/EDIT/PAID/ADVANCE/STATUS |
| `escort` | ✅ Active | Escort slip parsing |
| `escort_lifecycle` | ✅ Active | Escort assignment lifecycle |
| `escort_slip_extractor` | ✅ Active | Extract payment data from slips |
| `escort_roster` | ✅ Active | Roster management API |
| `recruitment_flow` | ✅ Active | Candidate intake workflow |
| `recruitment_ai` | ✅ Active | LLM-backed recruitment replies |
| `payment_workflow` | ✅ Active | Payment finalization |
| `payment_ingest` | ✅ Active | Payment ingestion from SMS/accountant |
| `knowledge_base` | ✅ Active | Company rules + DB-backed facts |
| `intent` | ✅ Active | LLM intent classification |
| `identity_brain` | ✅ Active | Admin/employee/candidate identity |
| `user_role` | ✅ Active | RBAC |
| `rbac` | ✅ Active | Role-based access control |
| `outbound` | ✅ Active (disabled) | Queue-based send (OUTBOUND_ENABLED=false) |
| `scheduler` | ✅ Active | Cron-based tasks |
| `backup` | ✅ Active | Automated backup routines |
| `reports` | ✅ Active | Daily digest, admin reports |
| `observability` | ✅ Active | Health + metrics endpoints |
| `social_auto_reply` | ✅ Active | Facebook/Messenger auto-reply |
| `reviewed_reply_memory` | ✅ Active | Admin-edited draft reuse |
| `draft_quality` | ✅ Active | Draft quality gate |
| `attendance` | ✅ Active | Attendance tracking |
| `attendance_parser` | ✅ Active | Parse attendance data |
| `ocr_processor` | ✅ Active | OCR for images |
| `image_hash` | ✅ Active | Image dedup |
| `voice_processor` | ✅ Active | Voice message STT |
| `rag` | ✅ Active | RAG retrieval |
| `payroll` | ✅ Active | Payroll processing |
| `payroll_logic` | ✅ Active | Payroll calculations |
| `fazle_payroll_engine` | ✅ Active | FPE module (broken — SQLite) |
| `admin_employees` | ✅ Active | Employee admin API |
| `admin_transactions` | ✅ Active | Transaction admin API |
| `employee_verification` | ✅ Active | Employee identity verification |
| `contact_sync` | ⚠️ Dormant | 0 active callers |
| `conversation_layer` | ⚠️ Shadow | Test-only, not in production path |
| `media_normalization` | ⚠️ Dormant | 0 active callers |
| `message_archive` | ⚠️ Recovery | Recovery scripts only |
| `number_identity` | ⚠️ Unknown | Status unverified |
| `payment` | ⚠️ Dormant | Re-export stub only |
| `payment_correction` | ⚠️ Dormant | No active callers |
| `reply_templates` | ⚠️ Shadow | Only called from shadow `conversation_layer` |
| `accountant_summary` | ✅ Active | Accountant report generation |

---

## 6. Database Inventory

### 6A. PostgreSQL (`ai-postgres` container)

| Database | Owner | Purpose | Health |
|---|---|---|---|
| `waerp` | postgres | Fazle Core main DB (WhatsApp ERP) | ✅ Active |
| `postgres` | postgres | Default / LocationWhere (schema: `locationwhere`) | ✅ Active |
| `fazle_test` | postgres | Test DB for CI/integration tests | ✅ Present |
| `template0`, `template1` | postgres | System templates | System |

> **Overlap Risk:** Both Fazle Core and LocationWhere share the same `ai-postgres` container. LocationWhere uses schema `locationwhere` in the `postgres` database. If the container fails, both apps lose their database simultaneously.

### 6B. Redis (`ai-redis` container)

| App | Redis DB | Purpose |
|---|---|---|
| Fazle Core | DB 9 | Outbound queue, dedup |
| LocationWhere | DB 15 | Session/cache (optional) |
| Fazle AI (planned) | DB 7 | Rate limiting |

### 6C. SQLite (bridge stores — BROKEN)

| Bridge | Expected Path | Actual State |
|---|---|---|
| Bridge 1 | `/home/azim/whatsapp1/store/messages.db` | ❌ File not found — directory has individual LID subdirs |
| Bridge 2 | `/home/azim/whatsapp2/store/messages.db` | ❌ `/home/azim/whatsapp2/` does not exist |
| whatsapp3 | `/home/azim/whatsapp3/store/messages.db` | ✅ File exists (bridge3, not in production path) |
| core/store | `/home/azim/core/store/messages.db` | ✅ File exists (purpose unclear — may be legacy) |

### 6D. Object Storage (MinIO)

- `ai-call-platform_minio-data` volume
- Not actively used by any running service (Fazle AI platform not deployed)

### 6E. Vector DB (Qdrant)

- `ai-call-platform_qdrant_data` volume
- Not actively used by any running service (Fazle AI platform not deployed)

---

## 7. Frontend Inventory

| Frontend | Path | Served By | Hostname | Status |
|---|---|---|---|---|
| **iamazim.com** (company site) | `/var/www/iamazim.com/` | nginx static | `iamazim.com`, `www.iamazim.com` | ✅ Live |
| **LocationWhere Web App** | `/var/www/locationwhere.iamazim.com/` (with APK in `downloads/`) | nginx → `/api/*` proxied to :8310 | `locationwhere.iamazim.com` | ✅ Live |
| **LocationWhere Frontend Source** | `/home/azim/locationwhere-frontend/` | Source only (has `assets/`, `index.html`) | — | ⚠️ Stale source |
| **iamazim-web Source** | `/home/azim/iamazim-web/` | Source only | — | ⚠️ Stale source |
| **Fazle AI UI** | `/home/azim/ai-call-platform/fazle-ai/ui/` | Not deployed | — | ❌ Not deployed |
| **Open WebUI** | Docker container | nginx → :8501 | `chat.iamazim.com` | ✅ Live |
| **VS Code Server** | Docker container | nginx → :8443 | `vscode.iamazim.com` | ✅ Live |

---

## 8. Backend Inventory

| Backend | Path | Type | Port | Status |
|---|---|---|---|---|
| **Fazle Core** | `/home/azim/core` | FastAPI / Python | 8200 | ✅ Running (degraded) |
| **LocationWhere API** | `/home/azim/locationwhere-backend` | Express / Node.js | 8310 | ✅ Running |
| **Fazle Agent** | `/home/azim/system-agent` | FastAPI / Python | 8300 | ✅ Running (dry-run) |
| **Media Processor** | `/home/azim/shared/media/media-processor` | Python | 8090 | ✅ Running |
| **WhatsApp Bridge 1** | `/home/azim/whatsapp-mcp/whatsapp-bridge` | Go binary | 8082 | ✅ Running (send OK, read broken) |
| **WhatsApp Bridge 2** | `/home/azim/whatsapp-mcp/whatsapp-bridge` | Go binary | 8081 | ✅ Running (send OK, read broken) |
| **Ollama** | Docker: `ollama` | AI model server | 11434 (internal) | ✅ Running |
| **PostgreSQL** | Docker: `ai-postgres` | Database | 5432 (internal) | ✅ Running |
| **Redis** | Docker: `ai-redis` | Cache | 6379 (internal) | ✅ Running |
| **Fazle AI (planned)** | `/home/azim/ai-call-platform/fazle-system/` | Planned microservices | various | ❌ Not running |

---

## 9. .env Files Inventory

| File | Owner | Secrets Status | Notes |
|---|---|---|---|
| `/home/azim/core/.env` | Fazle Core | Has real credentials | Single source of truth for fazle-core |
| `/home/azim/.env` | Root/misc | Has real credentials | Global env, unclear owner |
| `/home/azim/locationwhere-backend/.env` | LocationWhere | Has real credentials | Has hardcoded Firebase path |
| `/home/azim/ai-call-platform/.env` | AI Platform | Has real credentials | Multiple backup variants present |
| `/home/azim/ai-call-platform/.env.bak`, `.env.bak.*`, `.env.local`, `.env.meta_reset_backup` | AI Platform | Stale backups | Should be purged |
| `/home/azim/code-server/.env` | VS Code Server | Unknown | Dev env |
| `/home/azim/fazle-agent-dev/.env` | Fazle Agent | Unknown | Dev env |
| `/home/azim/fazle-diagnostic-agent/.env` | Diagnostic | Unknown | Tool env |
| `/home/azim/agent/.env` | Agent | Unknown | Purpose unclear |
| `/home/azim/ai-call-platform/ai-infra/.env` | AI Infra | Has real credentials | Database/Redis passwords |
| `/home/azim/ai-call-platform/fazle-ai/.env` | Fazle AI | Placeholder (`.env.example` content) | Not yet real secrets |
| `/home/azim/core/.env.bak.phoneidfix_20260523_004413`, `.env.save` | Fazle Core | Stale backups | Should be purged |
| `/home/azim/backups/fazle-backup-20260531-2150/.env` | Backup | Real credentials | In backup dir — secure? |
| `/home/azim/system-agent-backup-1777241194/.env` | Backup | Real credentials | Old backup — audit access |

> **Security Risk:** Multiple `.env` backups scattered in non-gitignored, potentially world-readable paths. The `secure-env-backup/` directory exists (`drwx------`) and is properly restricted, but backup `.env` files in `/home/azim/backups/` and stale dirs may not be.

---

## 10. README Files Inventory

| File | Quality | Accuracy |
|---|---|---|
| `/home/azim/README.md` | ✅ Good | ✅ Current (updated 2026-06-04) — single source of truth |
| `/home/azim/core/README.md` | ✅ Good | ✅ Current (2026-06-04) — comprehensive |
| `/home/azim/fazle-core.service` (README embedded) | N/A | ⚠️ References old path `/home/azim/fazle-core` |
| `/home/azim/ai-call-platform/README.md` | ✅ Good | ⚠️ May be out of date (platform not deployed) |
| `/home/azim/locationwhere-backend/package.json` | N/A | ✅ Current |
| `/home/azim/fazle-diagnostic-agent/README.md` | ✅ Good | ✅ Current |
| `/home/azim/RECRUITMENT_BOT_COORDINATION.md` | ✅ Good | ✅ Current (2026-06-01) |
| `/home/azim/fazle-agent-dev/README.md` | Not read fully | — |

---

## 11. Git Repos & GitHub Accounts

### GitHub Accounts

| Account | URL | Role |
|---|---|---|
| **arshiyaazim** | github.com/arshiyaazim | Primary developer account |
| **MuradulAzim** | github.com/MuradulAzim | Second account (owner?) |

### Repos per Account

**arshiyaazim:**
- `arshiyaazim/fazle-core` — primary fazle-core repo (SSH: `github-arshiyaazim`)
- `arshiyaazim/fazle-core-updated` — updated fork
- `arshiyaazim/fazle-payroll-engine` — payroll engine standalone
- `arshiyaazim/ai-call-platform` — upstream AI platform

**MuradulAzim:**
- `MuradulAzim/fazle-core` — copy/fork (HTTPS only)
- `MuradulAzim/ai-call-platform` — origin for AI platform work

### Local Repos & Their Remotes

| Local Path | Remote (origin) | Notes |
|---|---|---|
| `/home/azim/` (root) | `origin → arshiyaazim/fazle-core` | ⚠️ Home dir tracked as fazle-core repo |
| `/home/azim/core` | `origin → arshiyaazim/fazle-core` (+ 3 others) | Main production repo |
| `/home/azim/locationwhere-backend` | `origin → arshiyaazim/fazle-core` | ❌ WRONG — points to fazle-core, not locationwhere |
| `/home/azim/ai-call-platform` | `origin → MuradulAzim/ai-call-platform`, `upstream → arshiyaazim/ai-call-platform` | Two-account fork setup |
| `/home/azim/fazle-agent-dev` | `origin → arshiyaazim/fazle-core` | ⚠️ Same wrong remote as locationwhere |

### SSH Config (inferred)
- `github-arshiyaazim` host alias → `arshiyaazim` account
- `github-muradulazim` host alias → `MuradulAzim` account

---

## 12. Nginx — Public Hostnames

| Hostname | Backend | SSL | Status |
|---|---|---|---|
| `iamazim.com` | `/var/www/iamazim.com/` (static) | ✅ Let's Encrypt | ✅ Live |
| `www.iamazim.com` | Redirect → `iamazim.com` | ✅ | ✅ Live |
| `api.iamazim.com` | → `127.0.0.1:8200` (fazle-core) | ✅ | ✅ Live |
| `fazle.iamazim.com` | → `127.0.0.1:8200` (fazle-core) | ✅ | ✅ Live |
| `chat.iamazim.com` | → `172.22.0.2:8080` (open-webui) | ✅ | ✅ Live |
| `vscode.iamazim.com` | → `127.0.0.1:8443` (code-server) | ✅ | ✅ Live |
| `locationwhere.iamazim.com` | Static + `/api/` → `:8310` | ✅ | ✅ Live |
| `livekit.iamazim.com` | Not symlinked | — | ❌ Config exists, not enabled |

> **10 stale `.bak` files** in `/etc/nginx/sites-available/` — not symlinked, not serving, but cluttering the directory.

---

## 13. Critical Issues, Conflicts, Overlaps & Gaps

### 🔴 CRITICAL — Broken Workflows

| # | Issue | Impact | Root Cause |
|---|---|---|---|
| C1 | **Bridge SQLite stores not found** — `messages.db` doesn't exist at configured paths for bridge1 and bridge2 | Inbound message processing fully stopped; no auto-replies possible; fazle-core health = `critical` | Bridge1 store dir has individual LID subdirs (not a `messages.db`); bridge2's entire working directory (`/home/azim/whatsapp2/`) is MISSING |
| C2 | **whatsapp2 working directory missing** — `whatsapp-bridge2.service` has `WorkingDirectory=/home/azim/whatsapp2` which doesn't exist | Bridge2 is running but its store is inaccessible | Directory likely renamed to `/home/azim/bridges/bridge2/` during a migration but service unit was never updated |
| C3 | **AUTO_REPLY_ENABLED=false** in live `.env` | No auto-replies sent even if bridging were fixed | Intentional or forgotten safe-mode setting |

### 🟡 WARNINGS — Configuration Conflicts & Inconsistencies

| # | Issue | Impact |
|---|---|---|
| W1 | **README says `AUTO_REPLY_ENABLED=true`** but live `.env` has `false` | Documentation lag — operators may believe system is auto-replying when it isn't |
| W2 | **`/home/azim/fazle-core.service`** references `WorkingDirectory=/home/azim/fazle-core` (old path) but installed service uses `/home/azim/core` | Service file in home dir is stale/orphaned |
| W3 | **locationwhere-backend git remote** points to `arshiyaazim/fazle-core` (wrong repo) | Cannot push locationwhere code to correct origin |
| W4 | **Fazle Agent `dry_run` inconsistency** — service description says "(dry-run)" but health API reports `dry_run: false` | Unclear operational state |
| W5 | **Two `modules/` directories** at `/home/azim/modules/` and `/home/azim/core/modules/` — 15-module divergence | Stale older copy may confuse developers |
| W6 | **`core/store/messages.db`** exists at unknown purpose | May be residual test artifact |
| W7 | **`fazle-social-auto-reply.service` runs in parallel** with `recruitment_ai` module inside `fazle-core.service` with `SOCIAL_AUTO_REPLY_SINGLE_ENGINE=false` | Risk of double-processing social messages |

### 🟡 GAPS — Missing Components

| # | Gap | Impact |
|---|---|---|
| G1 | **No systemd service for `locationwhere-backend`** | Will not restart on VPS reboot |
| G2 | **`/home/azim/whatsapp2/` directory missing** | Bridge2 SQLite store path broken |
| G3 | **13 of 14 Fazle AI platform services not running** | AI Call Platform SaaS entirely inactive |
| G4 | **LiveKit not configured** — `livekit.iamazim.com` nginx exists but disabled | No real-time voice for AI Call Platform |
| G5 | **AWS S3 not configured** for LocationWhere | File/media upload will fail |
| G6 | **SMS gateway not configured** for LocationWhere | SMS onboarding/alerts will fail |
| G7 | **No systemd service for Fazle Diagnostic Agent** | Must be run manually |
| G8 | **MinIO and Qdrant containers running but not used** by any active app | Wasting ~memory, volumes filling |

### 🔵 OVERLAPS — Duplicate / Redundant Components

| # | Overlap | Risk |
|---|---|---|
| O1 | **`/home/azim/modules/` vs `/home/azim/core/modules/`** | Developer confusion, stale code imported if wrong path used |
| O2 | **`/home/azim/fazle-payroll-engine/` standalone repo** vs `/home/azim/core/modules/fazle_payroll_engine/` embedded module | Dual maintenance, sync drift |
| O3 | **Multiple recruitment systems** documented in `RECRUITMENT_BOT_COORDINATION.md` — internal core vs external agent v1/v2/v3 | Properly decommissioned (external agents stopped), but old code at `/home/azim/external_recruitment_agent/` still present |
| O4 | **Two GitHub accounts** (`arshiyaazim`, `MuradulAzim`) with forks of same repos | Code may diverge silently; unclear which is canonical |
| O5 | **`/home/azim/core/` and `/home/azim/` both tracked as `fazle-core` git repo** | Pushing from wrong directory could overwrite repo state |
| O6 | **Multiple `.env` backup files** scattered in non-secured paths | Security and maintenance overhead |
| O7 | **`/home/azim/fazle-agent-dev/` vs `/home/azim/system-agent/`** — dev and deployed versions of same agent | Sync and confusion risk |
| O8 | **`api.iamazim.com` and `fazle.iamazim.com`** both proxy to same backend (port 8200) | Double exposure, one can be removed |

---

## 14. Working Workflows

| Workflow | Path | Status |
|---|---|---|
| WhatsApp → Bridge → HTTP | Bridge HTTP endpoints responding (both 200) | ✅ Working |
| Admin command (APPROVE/REJECT via WhatsApp) | When bridge send works | ✅ Partially (send OK, receive broken) |
| AI model inference | Ollama ↔ Open WebUI | ✅ Working |
| Private AI chat | chat.iamazim.com → Ollama | ✅ Working |
| Company website | iamazim.com → static files | ✅ Working |
| LocationWhere GPS tracking | Mobile → API → PostgreSQL | ✅ Working |
| LocationWhere frontend | locationwhere.iamazim.com | ✅ Working |
| Dev IDE access | vscode.iamazim.com → code-server | ✅ Working |
| Monitoring | Grafana → Prometheus + Loki | ✅ Working |
| Media processing | Voice/Image/PDF → media-processor | ✅ Working |
| Database backup | Automated (cron via `backup` module) | ✅ Working |
| SSL certificates | Let's Encrypt (all active domains) | ✅ Working |

---

## 15. Broken Workflows

| Workflow | Break Point | Cause |
|---|---|---|
| **Inbound WhatsApp message processing** | bridge_poller cannot open `messages.db` | SQLite store path mismatch (C1, C2) |
| **Auto-reply generation** | `AUTO_REPLY_ENABLED=false` | Safe mode / disabled flag (C3) |
| **Outbound message queue** | `OUTBOUND_ENABLED=false` | Disabled |
| **Fazle Payroll Engine gap scan** | Cannot read bridge SQLite | Same as C1/C2 |
| **Fazle AI Call Platform** | 13 services never deployed | No deployment executed |
| **LocationWhere SMS alerts** | No SMS gateway configured | Missing credentials (G6) |
| **LocationWhere file uploads** | No S3 bucket configured | Missing credentials (G5) |
| **LocationWhere on VPS reboot** | No systemd service | Manual start only (G1) |
| **Bridge2 store access** | `/home/azim/whatsapp2/` missing | Directory not created or renamed (C2) |

---

## 16. VPS Resource Summary

| Resource | Total | Used | Free | Status |
|---|---|---|---|---|
| Disk | 194 GB | 112 GB (58%) | 83 GB | ✅ Healthy |
| RAM | 23 GB | 8.4 GB | 14 GB available | ✅ Healthy |
| Swap | 3 GB | 802 MB | 2.2 GB | ✅ Healthy |
| CPU Load (1m/5m/15m) | — | 2.52 / 1.85 / 1.87 | — | ⚠️ Moderate |
| Uptime | 11 days | — | — | ✅ Stable |

---

## 17. Production Capability Summary — Per App

| App | Production % | Blocker |
|---|---|---|
| **iamazim.com Website** | **90%** | No CI/CD; manual deploy |
| **Open WebUI (chat)** | **95%** | None significant |
| **VS Code Server** | **95%** | None significant |
| **Grafana Monitoring Stack** | **85%** | Alert delivery may be unconfigured |
| **Media Processor** | **85%** | Works but no validation of STT accuracy |
| **LocationWhere Backend** | **65%** | No systemd, no S3, no SMS, wrong git remote |
| **LocationWhere Frontend** | **80%** | APK download present; S3/SMS features incomplete |
| **Fazle Payroll Engine** | **65%** | SQLite bridge errors break gap scan & hsync |
| **Fazle Core (WhatsApp AI)** | **55%** | Bridge SQLite broken — no inbound message processing |
| **WhatsApp Bridges (1 & 2)** | **60%** | Running and can send, but inbound reading broken |
| **Fazle Agent** | **50%** | 4,863 open incidents; dry_run flag inconsistency |
| **Fazle Social Auto-Reply** | **40%** | Auto-reply disabled; legacy pipeline |
| **Fazle Diagnostic Agent** | **20%** | No active service; manual only |
| **AI Call Platform / Fazle AI** | **15%** | Infrastructure only; 13/14 services not running |

---

## 18. Recommended Immediate Actions

### Priority 1 — Fix Broken Core Workflow
1. **Identify correct bridge SQLite paths.** The bridge processes write their stores to directory structures, not a single `messages.db`. Locate the actual DB file (likely `whatsapp.db` inside the store dir, or a combined db written by the Go binary) and update `BRIDGE1_DB_PATH`/`BRIDGE2_DB_PATH` in `/home/azim/core/.env`.
2. **Recreate `/home/azim/whatsapp2/` or update `whatsapp-bridge2.service`** to use the correct working directory (likely `/home/azim/bridges/bridge2/`).
3. **Resolve `AUTO_REPLY_ENABLED`** — if auto-reply should be live, set it to `true` in `/home/azim/core/.env` and restart `fazle-core.service`.

### Priority 2 — Fix Gaps
4. **Create systemd service for `locationwhere-backend`** so it survives reboots.
5. **Fix locationwhere-backend git remote** to point to the correct repo.

### Priority 3 — Clean Up
6. **Archive `/home/azim/modules/`** (stale 30-module copy) to avoid confusion.
7. **Remove stale `.env.bak` files** from non-secured locations.
8. **Remove/archive 10 stale nginx `.bak` configs** in `sites-available/`.
9. **Clarify canonical GitHub account** — pick `arshiyaazim` or `MuradulAzim` as primary; sync forks.

---

*Audit completed 2026-06-07 — read-only, no changes made.*
