# Phase 0 Baseline Report

Generated: 2026-06-24T19:33:11.932721+00:00
Repository: `/home/azim/core`

## Module Inventory
Modules discovered: **57**

| Module | Python files | Router | DB use | README |
|---|---:|---|---|---|
| `accountant_summary` | 1 | False | False |  |
| `admin_chat_lab` | 1 | False | False |  |
| `admin_commands` | 8 | False | True |  |
| `admin_employees` | 1 | True | True |  |
| `admin_transactions` | 1 | True | True |  |
| `ai_readonly_tools` | 9 | False | True |  |
| `attendance` | 1 | False | True |  |
| `attendance_parser` | 1 | False | True |  |
| `backup` | 1 | False | True |  |
| `bridge_poller` | 1 | False | True |  |
| `contact_roles` | 2 | True | True |  |
| `contact_sync` | 1 | False | True |  |
| `conversation_layer` | 4 | False | False |  |
| `draft_quality` | 1 | False | False |  |
| `drafts` | 2 | True | True |  |
| `employee_verification` | 1 | False | True |  |
| `escort` | 1 | False | True | modules/escort/README.md |
| `escort_lifecycle` | 1 | False | True | modules/escort_lifecycle/README.md |
| `escort_roster` | 6 | True | True |  |
| `escort_slip_extractor` | 1 | False | True |  |
| `fazle_payroll_engine` | 18 | True | True | modules/fazle_payroll_engine/README.md |
| `identity_brain` | 1 | False | True |  |
| `image_hash` | 1 | False | True |  |
| `intent` | 1 | False | False |  |
| `kb_upload` | 3 | True | True |  |
| `knowledge_base` | 1 | False | True |  |
| `media_normalization` | 1 | False | False |  |
| `memory_extractor` | 1 | False | True |  |
| `message_archive` | 1 | False | True |  |
| `message_router` | 1 | False | True |  |
| `number_identity` | 1 | False | False |  |
| `observability` | 1 | False | False |  |
| `ocr_processor` | 1 | False | False |  |
| `ollama_memory` | 4 | True | True |  |
| `operations_health` | 1 | False | False |  |
| `outbound` | 1 | False | True |  |
| `payment` | 1 | False | False |  |
| `payment_correction` | 1 | False | True |  |
| `payment_ingest` | 2 | False | True |  |
| `payment_workflow` | 1 | False | True |  |
| `payroll` | 1 | False | True |  |
| `payroll_logic` | 1 | False | True |  |
| `phone_normalizer` | 1 | False | False |  |
| `rag` | 1 | False | True |  |
| `rbac` | 1 | False | True |  |
| `recruitment_ai` | 1 | False | False |  |
| `recruitment_assistant` | 1 | False | False |  |
| `recruitment_flow` | 1 | False | True |  |
| `reply_templates` | 1 | False | False |  |
| `reports` | 1 | False | True |  |
| `reviewed_reply_memory` | 1 | False | True |  |
| `role_classifier` | 1 | False | True |  |
| `scheduler` | 1 | False | True |  |
| `social_auto_reply` | 21 | True | True |  |
| `user_role` | 1 | False | True |  |
| `voice_processor` | 1 | False | False |  |
| `wa_chat_frontend` | 1 | True | True |  |

## Service Inventory
| Service | Referenced files |
|---|---:|
| Ollama | 21 |
| Groq | 4 |
| GitHub Models | 5 |
| PostgreSQL | 18 |
| Redis | 3 |
| WhatsApp Bridge | 10 |
| Meta API | 3 |
| Media Processor | 5 |
| LocationWhere | 2 |
| SMS Gateway | 4 |

## Route Inventory
Routes discovered: **209**

| Method | Path | File |
|---|---|---|
| GET | `` | `modules/escort_roster/routes.py` |
| POST | `` | `modules/escort_roster/routes.py` |
| GET | `/` | `app/main.py` |
| GET | `/` | `modules/contact_roles/routes.py` |
| POST | `/` | `modules/contact_roles/routes.py` |
| GET | `/active` | `modules/escort_roster/routes.py` |
| GET | `/admin/approvals` | `app/main.py` |
| GET | `/admin/audit` | `app/main.py` |
| GET | `/admin/audit` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/admin/dlq` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/admin/dlq/{fpe_wa_message_id}/requeue` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/admin/drafts` | `app/main.py` |
| GET | `/admin/gap-scan/runs` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/admin/gap-scan/trigger` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/admin/kb/delete` | `modules/kb_upload/routes.py` |
| GET | `/admin/kb/list` | `modules/kb_upload/routes.py` |
| GET | `/admin/kb/stats` | `modules/kb_upload/routes.py` |
| POST | `/admin/kb/upload` | `modules/kb_upload/routes.py` |
| GET | `/admin/memory/reviewed-replies` | `app/main.py` |
| DELETE | `/admin/memory/reviewed-replies/{entry_id}` | `app/main.py` |
| PATCH | `/admin/memory/reviewed-replies/{entry_id}/toggle` | `app/main.py` |
| GET | `/admin/needs-review` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/admin/needs-review/{unmatched_id}/dismiss` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/admin/needs-review/{unmatched_id}/promote` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/admin/overview` | `app/main.py` |
| GET | `/admin/payment-drafts` | `app/main.py` |
| GET | `/admin/reconcile` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/admin/recruitment` | `app/main.py` |
| GET | `/admin/review-summary` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/admin/safe-mode` | `app/main.py` |
| GET | `/admin/users` | `app/main.py` |
| POST | `/admin/users` | `app/main.py` |
| POST | `/admin/users/{phone}/apikey` | `app/main.py` |
| POST | `/admin/users/{phone}/disable` | `app/main.py` |
| POST | `/admin/users/{phone}/role` | `app/main.py` |
| DELETE | `/admin/users/{phone}/role/{role}` | `app/main.py` |
| GET | `/api/bridges/diagnostics` | `app/main.py` |
| POST | `/api/bridges/probe` | `app/main.py` |
| GET | `/api/drafts` | `modules/drafts/routes.py` |
| GET | `/api/drafts/stats` | `modules/drafts/routes.py` |
| POST | `/api/drafts/{draft_id}/approve` | `modules/drafts/routes.py` |
| POST | `/api/drafts/{draft_id}/block` | `modules/drafts/routes.py` |
| POST | `/api/drafts/{draft_id}/delete` | `modules/drafts/routes.py` |
| POST | `/api/drafts/{draft_id}/edit` | `modules/drafts/routes.py` |
| POST | `/api/frontend/heartbeat` | `app/main.py` |
| GET | `/api/frontend/sync-stats` | `app/main.py` |
| GET | `/api/memory/pending` | `app/main.py` |
| POST | `/api/memory/{memory_id}/dismiss` | `app/main.py` |
| POST | `/api/memory/{memory_id}/promote` | `app/main.py` |
| GET | `/api/queue/arbiter-metrics` | `app/main.py` |
| GET | `/api/queue/dead-letters` | `app/main.py` |
| POST | `/api/rag/rebuild` | `app/main.py` |
| GET | `/api/rag/recent-searches` | `app/main.py` |
| GET | `/api/rag/stats` | `app/main.py` |
| GET | `/api/runtime/nodes` | `app/main.py` |
| POST | `/api/self-heal/check` | `app/main.py` |
| GET | `/api/self-heal/diagnostics` | `app/main.py` |
| GET | `/api/state-version` | `app/main.py` |
| GET | `/api/stats/llm` | `app/main.py` |
| GET | `/api/users/search` | `app/main.py` |
| GET | `/api/users/{phone}` | `app/main.py` |
| PUT | `/api/users/{phone}` | `app/main.py` |
| POST | `/api/users/{phone}/memory` | `app/main.py` |
| POST | `/api/wa/broadcast` | `modules/wa_chat_frontend/__init__.py` |
| GET | `/api/wa/contacts` | `modules/wa_chat_frontend/__init__.py` |
| POST | `/api/wa/contacts/sync` | `modules/wa_chat_frontend/__init__.py` |
| GET | `/api/wa/contacts/sync-status` | `modules/wa_chat_frontend/__init__.py` |
| DELETE | `/api/wa/contacts/{phone}` | `modules/wa_chat_frontend/__init__.py` |
| GET | `/api/wa/contacts/{phone}` | `modules/wa_chat_frontend/__init__.py` |
| PATCH | `/api/wa/contacts/{phone}` | `modules/wa_chat_frontend/__init__.py` |
| POST | `/api/wa/contacts/{phone}/block` | `modules/wa_chat_frontend/__init__.py` |
| POST | `/api/wa/contacts/{phone}/unblock` | `modules/wa_chat_frontend/__init__.py` |
| GET | `/api/wa/drafts` | `modules/wa_chat_frontend/__init__.py` |
| POST | `/api/wa/drafts/{draft_id}/approve` | `modules/wa_chat_frontend/__init__.py` |
| POST | `/api/wa/drafts/{draft_id}/edit` | `modules/wa_chat_frontend/__init__.py` |
| POST | `/api/wa/drafts/{draft_id}/reject` | `modules/wa_chat_frontend/__init__.py` |
| GET | `/api/wa/groups` | `modules/wa_chat_frontend/__init__.py` |
| POST | `/api/wa/groups` | `modules/wa_chat_frontend/__init__.py` |
| DELETE | `/api/wa/groups/{group_id}` | `modules/wa_chat_frontend/__init__.py` |
| PATCH | `/api/wa/groups/{group_id}` | `modules/wa_chat_frontend/__init__.py` |
| POST | `/api/wa/groups/{group_id}/send` | `modules/wa_chat_frontend/__init__.py` |
| GET | `/api/wa/messages/{phone}` | `modules/wa_chat_frontend/__init__.py` |
| POST | `/api/wa/send` | `modules/wa_chat_frontend/__init__.py` |
| GET | `/api/wa/settings` | `modules/wa_chat_frontend/__init__.py` |
| PATCH | `/api/wa/settings` | `modules/wa_chat_frontend/__init__.py` |
| GET | `/api/wa/stream` | `modules/wa_chat_frontend/__init__.py` |
| POST | `/backfill-files` | `modules/escort_roster/routes.py` |
| POST | `/backfill-sqlite` | `modules/escort_roster/routes.py` |
| GET | `/backup/list` | `app/main.py` |
| POST | `/backup/rotate` | `app/main.py` |
| POST | `/backup/run` | `app/main.py` |
| GET | `/backup/status` | `app/main.py` |
| GET | `/cash` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/cash/{txn_id}` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/chat-lab` | `app/main.py` |
| GET | `/chat/memory/history` | `app/main.py` |
| GET | `/chat/memory/stats` | `app/main.py` |
| POST | `/chat/message` | `app/main.py` |
| GET | `/chat/models` | `app/main.py` |
| POST | `/cleanup-drafts` | `modules/escort_roster/routes.py` |
| POST | `/cleanup-empty-drafts` | `modules/escort_roster/routes.py` |
| POST | `/cleanup-junk-drafts` | `modules/escort_roster/routes.py` |
| GET | `/config` | `modules/escort_roster/routes.py` |
| POST | `/config` | `modules/escort_roster/routes.py` |
| GET | `/dashboard` | `app/main.py` |
| GET | `/dashboard/legacy` | `app/main.py` |
| GET | `/dashboard/wa-chat` | `app/main.py` |
| GET | `/debug/auth-check` | `modules/admin_employees/__init__.py` |
| GET | `/drafts` | `app/main.py` |
| GET | `/drafts` | `modules/escort_roster/routes.py` |
| GET | `/employees` | `modules/admin_employees/__init__.py` |
| GET | `/employees` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/employees` | `modules/admin_employees/__init__.py` |
| GET | `/employees/by-phone/{phone}` | `modules/admin_employees/__init__.py` |
| GET | `/employees/search` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/employees/suggest` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/employees/{emp_id}` | `modules/admin_employees/__init__.py` |
| GET | `/employees/{emp_id}` | `modules/fazle_payroll_engine/routes.py` |
| PATCH | `/employees/{emp_id}` | `modules/fazle_payroll_engine/routes.py` |
| PUT | `/employees/{emp_id}` | `modules/admin_employees/__init__.py` |
| PATCH | `/employees/{emp_id}/deactivate` | `modules/admin_employees/__init__.py` |
| GET | `/employees/{emp_id}/transactions` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/escort-roster` | `app/main.py` |
| POST | `/escort-slip/extract` | `app/main.py` |
| GET | `/escort-slip/extractions` | `app/main.py` |
| POST | `/escort-slip/test-report` | `app/main.py` |
| POST | `/escort/release` | `app/main.py` |
| GET | `/export` | `modules/escort_roster/routes.py` |
| GET | `/facts/{subject_type}/{subject_key}` | `modules/ollama_memory/memory_api.py` |
| GET | `/flagged` | `modules/social_auto_reply/routes.py` |
| GET | `/health` | `app/main.py` |
| GET | `/health` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/health/deep` | `app/main.py` |
| GET | `/income` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/income/{income_id}` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/ingest` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/kb` | `app/main.py` |
| GET | `/ledger/{emp_id}` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/match-slip` | `modules/escort_roster/routes.py` |
| GET | `/metrics` | `app/main.py` |
| GET | `/metrics/json` | `app/main.py` |
| POST | `/normalization/employees/{employee_id}/aliases` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/normalization/employees/{employee_id}/canonical` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/normalization/employees/{employee_id}/inactivate` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/normalization/employees/{employee_id}/link-canonical` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/normalization/review` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/normalization/review/{review_id}/resolve` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/normalization/summary` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/observability/errors` | `app/main.py` |
| GET | `/observability/summary` | `app/main.py` |
| GET | `/open-chat` | `app/main.py` |
| POST | `/pause` | `modules/social_auto_reply/routes.py` |
| POST | `/payment/advance-draft` | `app/main.py` |
| POST | `/payment/escort-draft` | `app/main.py` |
| POST | `/payment/ingest` | `app/main.py` |
| GET | `/payroll` | `app/main.py` |
| POST | `/payroll/compute` | `app/main.py` |
| POST | `/payroll/run/{run_id}/transition` | `app/main.py` |
| GET | `/payroll/runs` | `app/main.py` |
| GET | `/payroll/runs/{run_id}` | `app/main.py` |
| GET | `/payroll/{tab}` | `app/main.py` |
| GET | `/questions` | `modules/ollama_memory/memory_api.py` |
| GET | `/queue` | `modules/social_auto_reply/routes.py` |
| GET | `/rag/answer` | `app/main.py` |
| POST | `/rag/reindex` | `app/main.py` |
| GET | `/rag/search` | `app/main.py` |
| GET | `/rag/stats` | `app/main.py` |
| POST | `/rebuild-history` | `modules/escort_roster/routes.py` |
| POST | `/reconcile` | `modules/escort_roster/routes.py` |
| GET | `/reports` | `app/main.py` |
| GET | `/reports/{name}` | `app/main.py` |
| POST | `/resume` | `modules/social_auto_reply/routes.py` |
| POST | `/retry` | `modules/social_auto_reply/routes.py` |
| POST | `/scheduler/run/{job_name}` | `app/main.py` |
| GET | `/scheduler/status` | `app/main.py` |
| POST | `/send/mcp1` | `app/main.py` |
| POST | `/send/mcp2` | `app/main.py` |
| POST | `/send/meta` | `app/main.py` |
| GET | `/staging-payments` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/stats` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/stats` | `modules/ollama_memory/memory_api.py` |
| GET | `/status` | `modules/social_auto_reply/routes.py` |
| GET | `/summary` | `modules/escort_roster/routes.py` |
| GET | `/sync-all` | `modules/escort_roster/routes.py` |
| POST | `/sync-all` | `modules/escort_roster/routes.py` |
| POST | `/sync-history` | `modules/escort_roster/routes.py` |
| GET | `/sync/status` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/sync/trigger` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/transactions` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/transactions` | `modules/admin_transactions/__init__.py` |
| POST | `/transactions/manual` | `modules/fazle_payroll_engine/routes.py` |
| DELETE | `/transactions/{txn_id}` | `modules/admin_transactions/__init__.py` |
| GET | `/transactions/{txn_id}` | `modules/fazle_payroll_engine/routes.py` |
| PUT | `/transactions/{txn_id}` | `modules/admin_transactions/__init__.py` |
| POST | `/transactions/{txn_id}/reverse` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/unmatched` | `modules/fazle_payroll_engine/routes.py` |
| POST | `/unmatched/{unmatched_id}/mark-reviewed` | `modules/fazle_payroll_engine/routes.py` |
| GET | `/wa-chat` | `app/main.py` |
| POST | `/webhook/mcp1` | `app/main.py` |
| POST | `/webhook/mcp2` | `app/main.py` |
| GET | `/webhook/meta` | `app/main.py` |
| POST | `/webhook/meta` | `app/main.py` |
| DELETE | `/{phone}` | `modules/contact_roles/routes.py` |
| PUT | `/{phone}` | `modules/contact_roles/routes.py` |
| DELETE | `/{program_id}` | `modules/escort_roster/routes.py` |
| GET | `/{program_id}` | `modules/escort_roster/routes.py` |
| PATCH | `/{program_id}` | `modules/escort_roster/routes.py` |
| POST | `/{program_id}/recalculate` | `modules/escort_roster/routes.py` |
| POST | `/{program_id}/sync` | `modules/escort_roster/routes.py` |

## Database Inventory
SQL files: **26**
Declared tables: **31**
Declared views: **10**

Declared tables/views are from static SQL inspection only; no production database was queried.

## Knowledge Base Inventory
Markdown files: **203**

| Folder | Markdown files |
|---|---:|
| `00_governance` | 15 |
| `01_employee_knowledge` | 8 |
| `02_admin_knowledge` | 5 |
| `02_admin_system` | 6 |
| `03_ai_identity` | 10 |
| `03_developer_system` | 7 |
| `04_business_rules` | 9 |
| `05_workflows` | 8 |
| `06_developer_system` | 37 |
| `07_archived` | 84 |
| `KBTI_v1_PreExecution_Deliverables_20260621.md` | 1 |
| `PKM_KE_KBG_Program_v2_Analysis_2026-06-21.md` | 1 |
| `PKM_KE_KBG_Program_v2_Analysis_2026-06-21_BOARD_READY.md` | 1 |
| `PKM_KE_KBG_Program_v2_Analysis_2026-06-21_DETAILED.md` | 1 |
| `PKVC_Program_v1_Analysis_2026-06-21.md` | 1 |
| `README.md` | 1 |
| `ai_access_matrix.md` | 1 |
| `conflict_resolution_record.md` | 1 |
| `duplicate_report.md` | 1 |
| `enrichment_report.md` | 1 |
| `gap_report.md` | 1 |
| `knowledge_inventory.md` | 1 |
| `missing_report.md` | 1 |
| `production_knowledge_report.md` | 1 |

## Frontend Inventory
Frontend/static files: **7**

## Integration Inventory
Integration references are captured in `baseline_inventory.json` under `services` and `database.code_references`.

## Phase 0 Result
Read-only baseline audit completed. No production logic, database schema, bridge store, or service configuration was modified.
