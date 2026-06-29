# Super Duper Pancake - VPS Platform

Unified monorepo for Al-Aqsa Security Service & Trading Centre infrastructure.

## 🏗️ Repository Structure

```
super-duper-pancake/
├── apps/                          # Application source code
│   ├── core/                     # Fazle Core - WhatsApp AI Backend (Python/FastAPI)
│   ├── locationwhere/            # Employee GPS Tracking (Node.js/Prisma)
│   ├── agent/                    # Fazle Super Agent - System Monitoring (Python)
│   ├── bridges/                  # WhatsApp Web Bridges (Node.js/Baileys)
│   ├── smsgateway/               # SMS Gateway Service (Java/Android)
│   ├── iamazim-web/              # Company Website (Static/HTML)
│   └── whatsapp-mcp/             # WhatsApp MCP Bridge Binaries
├── infra/                         # Infrastructure configuration
│   ├── docker/                   # Docker Compose files
│   ├── nginx/                    # Nginx site configurations
│   ├── systemd/                  # Systemd service files
│   └── scripts/                  # Deployment and maintenance scripts
├── docs/                          # Documentation
├── scripts/                       # Utility scripts
├── .env.example                   # Environment template
├── .gitignore                     # Git ignore rules
└── README.md                      # This file
```

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Node.js 20+
- Python 3.10+
- Java 17 (for smsgateway)

### Setup
1. Copy `.env.example` to `.env` and fill in secrets
2. Start infrastructure: `docker-compose -f infra/docker/docker-compose.yml up -d`
3. Install app dependencies (see individual app READMEs)

## 📊 Applications

| App | Port | Language | Description |
|-----|------|----------|-------------|
| core | 8200 | Python/FastAPI | WhatsApp AI Backend |
| locationwhere | 8310 | Node.js/Prisma | Employee GPS Tracking |
| agent | 8300 | Python | System Monitoring Agent |
| bridges | 8081-8083 | Node.js | WhatsApp Web Bridges |
| smsgateway | - | Java/Android | SMS Gateway Service |
| iamazim-web | 80 | HTML/JS | Company Website |
| whatsapp-mcp | - | Binary | MCP Bridge |

## ⚠️ Important Notes

- **DO NOT** commit `.env` files with real credentials
- Original application directories in `/home/azim/` are backed up at `~/final-backup-YYYYMMDD/`
- All services should be updated to point to `~/super-duper-pancake/apps/` paths
- Oracle JDK excluded from repo (140MB+ binary) - install separately

## 🔒 Security

- Sensitive data in `.env` files (not committed)
- SSL certificates in `/etc/letsencrypt/` (not in repo)
- Database data in Docker volumes (not in repo)
- Firebase/Twilio credentials removed from commit history


## 🏦 Financial Architecture (C1B — Canonical Transaction Engine)

As of 2026-06-29, the system uses a **single canonical financial transaction table**:

```
WhatsApp → Parser → Employee Resolve → create_transaction() → fpe_cash_transactions → Ledger → Audit → Dashboard / Payroll / Reports
```

### Canonical Tables

| Table | Role |
|-------|------|
| `fpe_cash_transactions` | **Only canonical cash transaction store** — all new writes go here |
| `wbom_cash_transactions` | **Legacy read-only archive** — no new writes permitted |
| `fpe_employee_ledger\ | Employee balance ledger — updated only by `create_transaction()` |
| `fpe_accounting_audit_logs` | Immutable audit trail for all financial operations |
| `fpe_employees` | Canonical employee registry (WBOM employees linked via `wbom_employee_id`) |

### Key Rules (Financial Architecture Freeze v2)

1. `create_transaction()` in `apps/core/modules/fazle_payroll_engine/accounting.py` is the **only canonical financial writer**
2. No developer may add a write path to `wbom_cash_transactions`
3. All new financial features must use the FPE pipeline
4. The employee ledger is updated **only** by `create_transaction()` via `_upsert_ledger()`
5. All financial reads must query `fpe_cash_transactions`

### C1B Certification Documents

- [`apps/core/audits/C1B-FINAL-OWNER-REPORT.md`](apps/core/audits/C1B-FINAL-OWNER-REPORT.md) — Phase-by-phase implementation report
- [`apps/core/audits/C1B-RELEASE-GATE-CERTIFICATION.md`](apps/core/audits/C1B-RELEASE-GATE-CERTIFICATION.md) — 5-gate production certification

## 🔄 Migration

This repository was created on 2026-06-28 by consolidating 7 separate application repositories into a unified monorepo structure.

### Original Repositories Merged:
- `/home/azim/core` → `apps/core`
- `/home/azim/location_where` → `apps/locationwhere`
- `/home/azim/agent` → `apps/agent`
- `/home/azim/bridges` → `apps/bridges`
- `/home/azim/smsgateway` → `apps/smsgateway`
- `/home/azim/iamazim-web` → `apps/iamazim-web`
- `/home/azim/whatsapp-mcp` → `apps/whatsapp-mcp`

## 📅 Migration Date

Completed: 2026-06-28

## 🔗 Links

- Production: https://fazle.iamazim.com
- Repository: https://github.com/arshiyaazim/super-duper-pancake
