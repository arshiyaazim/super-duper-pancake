# 📜 Repository Constitution

## Fazle Payroll Engine — Supreme Governance Document

> **এই Document এই Repository-এর সর্বোচ্চ নীতিমালা।**
> ভবিষ্যতে Copilot, Claude Code, GPT, Roo বা অন্য কোনো Developer/AI Agent যখনই এই Repository-তে কাজ করবে, **তাকে প্রথমে এই Constitution পড়তে হবে।**
> এতে Business Rule, Protected WhatsApp Flow, Canonical Transaction Principle এবং Owner Authority সবসময় অক্ষুণ্ণ থাকবে।

---

**Created:** 2026-06-28  
**Owner:** Azim (Business Owner)  
**Authority Level:** Supreme — এই Document-এর নিয়ম সব Code, Architecture, এবং Development Decision-এর উপরে।  
**Amendment:** শুধুমাত্র Business Owner পরিবর্তন করতে পারেন।  
**Supersedes:** সব পূর্ববর্তী architecture decision, convention, এবং informal rule।

---

## Section 1 — Architecture Freeze

আজকের Production Architecture (2026-06-28) Reference Architecture হিসেবে Freeze করা হলো।

### Frozen Reference Architecture

```
WhatsApp Bridge (bridge1/bridge2)
  ↓
FPE Ingestion Layer (ingestion.py)
  ↓
FPE Message Processing State (pending → parsing → parsed → accounting → done)
  ↓
FPE Parser Worker (parser.py + ai_enhancer.py)
  ↓
FPE Accounting Worker (workers.py)
  ↓
Employee Identity Resolution (employee.py — fpe_employees + canonical_employee_id)
  ↓
Canonical Transaction Service (accounting.py:create_transaction())
  ↓
fpe_cash_transactions (Single Canonical Transaction Table)
  ↓
fpe_employee_ledger (Single Canonical Ledger)
  ↓
fpe_accounting_audit_logs (Single Audit Trail)
  ↓
FPE Frontend (payroll.html — GET /api/fpe/transactions, /api/fpe/employees)
```

### Frozen Components

| Component | File | Status |
|-----------|------|--------|
| WhatsApp Ingestion | `fazle_payroll_engine/ingestion.py` | FROZEN |
| Parser Worker | `fazle_payroll_engine/workers.py:message_processor_worker()` | FROZEN |
| Accounting Worker | `fazle_payroll_engine/workers.py:accounting_worker()` | FROZEN |
| Parser Engine | `fazle_payroll_engine/parser.py:parse_message()` | FROZEN |
| AI Enhancer | `fazle_payroll_engine/ai_enhancer.py:ai_enhance_parse()` | FROZEN |
| Validation | `fazle_payroll_engine/validation.py:validate_for_accounting()` | FROZEN |
| Employee Match | `fazle_payroll_engine/employee.py:match_or_create_employee()` | FROZEN |
| Canonical Identity | `fazle_payroll_engine/employee.py:_resolve_canonical()` | FROZEN |
| Transaction Service | `fazle_payroll_engine/accounting.py:create_transaction()` | FROZEN |
| Ledger Service | `fazle_payroll_engine/accounting.py:_upsert_ledger()` | FROZEN |
| Reversal Service | `fazle_payroll_engine/accounting.py:reverse_transaction()` | FROZEN |
| Transaction List API | `fazle_payroll_engine/routes.py:list_transactions()` | FROZEN |
| Employee List API | `fazle_payroll_engine/routes.py:list_employees()` | FROZEN |

### Architecture Freeze Rules

এই Phase শেষ না হওয়া পর্যন্ত **নিষিদ্ধ:**

- ❌ নতুন App
- ❌ নতুন Microservice
- ❌ নতুন Database
- ❌ নতুন Transaction Table
- ❌ নতুন Ledger System
- ❌ নতুন Payroll Engine
- ❌ নতুন Approval Engine
- ❌ নতুন WhatsApp Parsing Engine
- ❌ Parallel Transaction Pipeline
- ❌ Duplicate Business Logic

**শুধুমাত্র Approved Refactoring Specification অনুযায়ী Additive Refactoring করা যাবে।**

কোনো নতুন Architecture Design করা যাবে না।

---

## Section 2 — Business Constitution Lock

Business Constitution সর্বোচ্চ Authority।

**Code কখনো Business Rule পরিবর্তন করবে না।**

**Business Rule পরিবর্তিত হলে Code পরিবর্তিত হবে।**

### Mandatory Business Rules

| # | Rule | Evidence | Enforcement |
|---|------|----------|-------------|
| 1 | Employee Request কখনো Final Transaction নয় | Employee request → draft → admin approve → transaction | Code must enforce approval gate |
| 2 | Draft Approval ছাড়া Employee Request Ledger-এ যাবে না | `fpe_unmatched_messages` review_status='pending' → promote → `create_transaction()` | Pending items must NOT call `_upsert_ledger()` |
| 3 | Admin → Accountant Payment Instruction-ই Final Transaction | WhatsApp `is_from_me=TRUE` → `create_transaction()` | Only `is_from_me` messages create transactions |
| 4 | বর্তমান WhatsApp Cash Ledger Flow Protected | Runtime evidence: 2,307 transactions by `fpe_engine`, 9,110 messages | See Section 4 |
| 5 | Employee Identity একটিই থাকবে | `fpe_employees` + `canonical_employee_id` soft-merge | No parallel employee table |
| 6 | Canonical Transaction Service একটিই থাকবে | `accounting.create_transaction()` | No parallel transaction creation |
| 7 | Canonical Ledger একটিই থাকবে | `fpe_employee_ledger` | No parallel ledger |
| 8 | Audit Log বাধ্যতামূলক | `fpe_accounting_audit_logs` — 1,382 entries | Every transaction must have audit entry |
| 9 | Business Decision Code-এর উপরে | Owner decides Edit/Delete/Approve policy | Code implements, never overrides |

এই Rules ভবিষ্যতের সব Module-এর জন্য বাধ্যতামূলক।

---

## Section 3 — Canonical Function Lock

### Canonical Transaction Function

```
Canonical Transaction Function = accounting.create_transaction()
```

**File:** `super-duper-pancake/apps/core/modules/fazle_payroll_engine/accounting.py:30`

### Canonical Ledger Function

```
Canonical Ledger Function = accounting._upsert_ledger()
```

**File:** `super-duper-pancake/apps/core/modules/fazle_payroll_engine/accounting.py:190`

### Canonical Reversal Function

```
Canonical Reversal Function = accounting.reverse_transaction()
```

**File:** `super-duper-pancake/apps/core/modules/fazle_payroll_engine/accounting.py:106`

### Canonical Employee Match Function

```
Canonical Employee Match Function = employee.match_or_create_employee()
```

**File:** `super-duper-pancake/apps/core/modules/fazle_payroll_engine/employee.py:94`

### Canonical Parser Function

```
Canonical Parser Function = parser.parse_message()
```

**File:** `super-duper-pancake/apps/core/modules/fazle_payroll_engine/parser.py:199`

### Lock Rules

| Rule | Description |
|------|-------------|
| Signature Lock | Core Function-এর Signature পরিবর্তন করা যাবে না |
| Behavior Lock | Existing Behavior পরিবর্তন করা যাবে না |
| Direct Modification | নিষিদ্ধ |
| Extension | শুধুমাত্র Wrapper / Adapter / Decorator Pattern-এর মাধ্যমে |
| Regression Gate | Regression Test Pass না করলে Core Function স্পর্শ করা যাবে না |
| Additive Only | নতুন parameter হলে default value সহ additive |
| No Override | Existing function override নিষিদ্ধ; নতুন function তৈরি করুন |

### Approved Extension Patterns

| Pattern | When to Use | Example |
|---------|-------------|---------|
| **Wrapper** | নতুন pre/post logic দরকার | `create_transaction_v2()` calls `create_transaction()` inside |
| **Adapter** | নতুন input format থেকে canonical format-এ convert | `add_admin_transaction_canonical()` builds `TransactionCreateRequest` then calls `create_transaction()` |
| **Decorator** | Cross-cutting concern (logging, metrics) | Feature flag check before calling canonical function |
| **Feature Flag** | নতুন behavior ON/OFF | `if settings.feature_flag: canonical_path() else: old_path()` |

---

## Section 4 — Protected Components Lock

### Never Modify Directly

| # | Component | File:Function | Protected Because | Business Risk | Regression Risk |
|---|-----------|-------------|-------------------|---------------|-----------------|
| 1 | `create_transaction()` | [`accounting.py:30`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/accounting.py:30) | Canonical transaction creation — সব WhatsApp payment এখান দিয়ে যায় | Transaction creation বন্ধ হলে সব payment বন্ধ | সম্পূর্ণ pipeline shutdown |
| 2 | `_upsert_ledger()` | [`accounting.py:190`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/accounting.py:190) | Canonical ledger update — employee balance নির্ভর করে | Balance ভুল হলে overpayment/underpayment | Employee totals ভুল হবে |
| 3 | `reverse_transaction()` | [`accounting.py:106`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/accounting.py:106) | Reversal service — correction এখান দিয়ে | Correction বন্ধ হলে ভুল সংশোধন অসম্ভব | Reversal logic ভাঙলে audit trail corrupted |
| 4 | `match_or_create_employee()` | [`employee.py:94`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/employee.py:94) | Employee identity resolution — সব transaction নির্ভর করে | Employee not found → transaction ব্যর্থ | Duplicate employee creation |
| 5 | `_resolve_canonical()` | [`employee.py:281`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/employee.py:281) | Canonical soft-link resolution | Identity merge ভাঙলে duplicate totals | Employee totals split |
| 6 | `parse_message()` | [`parser.py:199`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/parser.py:199) | Core parser — payment detection | Payment মিস হলে ledger incomplete | সব payment মিস হবে |
| 7 | `accounting_worker()` | [`workers.py:261`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/workers.py:261) | Accounting worker loop | Worker বন্ধ হলে কোনো transaction তৈরি হবে না | সম্পূর্ণ pipeline বন্ধ |
| 8 | `message_processor_worker()` | [`workers.py:103`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/workers.py:103) | Parser worker loop | Parser বন্ধ হলে কোনো message parse হবে না | সব message pending থাকবে |
| 9 | `_process_parsed_batch()` | [`workers.py:305`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/workers.py:305) | Parsed → transaction + ledger | Transaction creation বন্ধ | Pipeline বন্ধ |
| 10 | `_process_pending_batch()` | [`workers.py:119`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/workers.py:119) | Pending → parsed | Parser বন্ধ | সব message pending |
| 11 | `ingest_message()` | [`ingestion.py:25`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/ingestion.py:25) | WhatsApp message entry point | Message ingest বন্ধ | কোনো message আসবে না |
| 12 | `mark_processing_status()` | [`ingestion.py:84`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/ingestion.py:84) | FSM state transitions | FSM ভাঙলে message stuck | Message stuck in wrong state |
| 13 | `store_parser_result()` | [`ingestion.py:103`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/ingestion.py:103) | Parser result persistence | Result না থাকলে accounting worker কাজ করবে না | Pipeline বন্ধ |
| 14 | `store_unmatched()` | [`ingestion.py:129`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/ingestion.py:129) | Review queue insert | Review queue ভাঙলে unmatched message হারাবে | Unmatched message lost |
| 15 | `validate_for_accounting()` | [`validation.py:151`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/validation.py:151) | Per-type validation gate | Validation ভাঙলে invalid data transaction হবে | Bad data in ledger |
| 16 | `list_transactions()` | [`routes.py:272`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/routes.py:272) | Frontend transaction visibility | Frontend ভাঙলে Admin দেখতে পাবে না | Dashboard blank |
| 17 | `list_employees()` | [`routes.py:484`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/routes.py:484) | Frontend employee totals | Employee totals ভাঙলে ভুল balance | Wrong payment decisions |
| 18 | `ai_enhance_parse()` | [`ai_enhancer.py:46`](super-duper-pancake/apps/core/modules/fazle_payroll_engine/ai_enhancer.py:46) | AI fallback for low confidence | AI বন্ধ হলে low-confidence parse মিস | Some payments missed |

### Protected Tables (No Schema Change Without Owner Approval)

| Table | Purpose | Protected Because |
|-------|---------|-------------------|
| `fpe_wa_messages` | WhatsApp message storage | Ingestion depends on schema |
| `fpe_message_processing_state` | Processing FSM | Worker depends on schema |
| `fpe_parser_results` | Parsed data | Accounting worker depends on schema |
| `fpe_unmatched_messages` | Review queue | Review promotion depends on schema |
| `fpe_cash_transactions` | **Canonical transaction table** | সব transaction এখানে |
| `fpe_employees` | **Canonical employee table** | Identity resolution |
| `fpe_employee_aliases` | Employee identity aliases | Match logic depends on schema |
| `fpe_employee_ledger` | **Canonical ledger** | Balance calculation |
| `fpe_accounting_audit_logs` | **Canonical audit trail** | Compliance |

---

## Section 5 — Existing Structure First Policy

নতুন Table, Queue, Module, বা API তৈরি করার আগে বাধ্যতামূলকভাবে এই checklist পূরণ করতে হবে:

### Pre-Creation Checklist

```
[ ] বর্তমান System-এ একই কাজের জন্য কোনো Existing Structure আছে কি?
    → যদি থাকে, কোন file/table/function-এ?
[ ] সেটি Reuse করা যাবে কি?
    → যদি হ্যাঁ, কীভাবে?
[ ] যদি Reuse সম্ভব হয়, নতুন কিছু তৈরি করা যাবে না।
[ ] যদি Reuse সম্ভব না হয়, Evidence দিতে হবে:
    → কেন existing structure কাজ করছে না?
    → কোন limitation আছে?
    → কোন evidence (file, function, SQL)?
[ ] Owner Approval লাগবে।
```

### Existing Structure Inventory

| Need | Existing Structure | File/Table | Reuse? |
|------|-------------------|------------|--------|
| Transaction storage | `fpe_cash_transactions` | DB table | ✅ Yes — canonical |
| Employee identity | `fpe_employees` + `canonical_employee_id` | DB table | ✅ Yes — canonical |
| Ledger | `fpe_employee_ledger` | DB table | ✅ Yes — canonical |
| Audit log | `fpe_accounting_audit_logs` | DB table | ✅ Yes — canonical |
| Review queue | `fpe_unmatched_messages` | DB table | ✅ Yes — reuse for operator pending |
| Approval queue | `fazle_payment_drafts` | DB table | ✅ Yes — existing draft system |
| Transaction creation | `create_transaction()` | `accounting.py:30` | ✅ Yes — canonical service |
| Employee match | `match_or_create_employee()` | `employee.py:94` | ✅ Yes — canonical service |
| Parser | `parse_message()` | `parser.py:199` | ✅ Yes — canonical parser |
| Frontend | `payroll.html` | Static HTML | ✅ Yes — single SPA |
| API | `/api/fpe/*` | FastAPI router | ✅ Yes — canonical API |

---

## Section 6 — Database Change Policy

Database হবে **শেষ বিকল্প**।

### Priority Hierarchy

```
১. বর্তমান Structure Reuse
   ↓
২. Wrapper (নতুন function existing function call করে)
   ↓
৩. Adapter (নতুন input format → canonical format convert)
   ↓
৪. Feature Flag (নতুন behavior ON/OFF)
   ↓
৫. New API (নতুন route, existing table)
   ↓
৬. New Table (শুধুমাত্র যদি কোনো existing table reuse সম্ভব না হয়)
   ↓
৭. Migration (শুধুমাত্র যদি schema change অনিবার্য)
```

### Migration Conditions (All Must Be True)

| Condition | Requirement | Evidence |
|-----------|-------------|---------|
| Owner Explicit Approval | Written approval from Business Owner | Approval record |
| Backup Complete | Full DB backup taken and verified | Backup file path + checksum |
| Rollback Tested | Rollback procedure tested in staging | Rollback test result |
| Staging Verified | Migration tested in staging environment | Staging test report |
| Regression Passed | All regression tests pass | Test results |

### Forbidden DB Operations (Without Owner Approval)

- ❌ Migration
- ❌ Schema change (column add/drop/modify)
- ❌ Table merge
- ❌ Data update/delete
- ❌ Ledger repair
- ❌ Backfill
- ❌ Sync execution
- ❌ Production transaction create
- ❌ Service restart
- ❌ Truncate
- ❌ Forced sync
- ❌ Production data correction

---

## Section 7 — Definition of Done

কোনো Phase Complete ঘোষণা করা যাবে না যতক্ষণ না নিচের সবগুলো পূরণ হয়:

### Done Checklist

```
[ ] Business Rule Match — প্রতিটি business rule compliance verified
[ ] Acceptance Test Pass — সব acceptance test pass
[ ] Regression Test Pass — সব regression test pass (বিশেষ করে WhatsApp flow)
[ ] WhatsApp Flow Pass — Protected WhatsApp flow unchanged and working
[ ] Health Check OK — GET /api/fpe/health returns status=ok
[ ] Log Clean — কোনো new ERROR entry নেই
[ ] Ledger Correct — Transaction sum = ledger total (validated)
[ ] Employee Total Correct — GET /api/fpe/employees shows correct totals
[ ] Audit Log Verified — প্রতিটি transaction-এর audit entry আছে
[ ] Owner Approval Complete — Written owner approval আছে
```

**সবগুলো পূরণ হবে — একটিও বাদ দেওয়া যাবে না।**

---

## Section 8 — Evidence Standard Lock

**কোনো Statement Evidence ছাড়া লেখা যাবে না।**

### Acceptable Evidence Types

| Evidence Type | Example |
|---------------|---------|
| Source File | `accounting.py:30` |
| Function | `create_transaction()` |
| Route | `POST /api/fpe/transactions/manual` |
| SQL | `SELECT * FROM fpe_cash_transactions WHERE id = 2497` |
| API Response | `GET /api/fpe/health → {"status":"ok"}` |
| Runtime Log | `fazle-core.log: [fpe.acct] created txn id=2497` |
| Browser Network | DevTools Network tab screenshot |
| Database Row | `SELECT * FROM fpe_cash_transactions WHERE id = 2497` |
| Frontend Rendering | Screenshot of payroll.html transaction table |

### Evidence Rules

1. প্রতিটি Recommendation-এ Evidence reference থাকতে হবে
2. "Likely", "Probably", "Maybe" — নিষিদ্ধ
3. যদি Evidence না থাকে, "No Evidence Found" লিখতে হবে
4. Evidence সবসময় file:line format-এ থাকবে
5. Runtime evidence সবসময় timestamp সহ থাকবে

---

## Section 9 — Refactoring Philosophy

Refactoring-এর উদ্দেশ্য **নতুন Feature যোগ করা নয়**।

### Refactoring Objectives

| # | Objective | Description |
|---|-----------|-------------|
| 1 | Business Alignment | Code business rule-এর সাথে মিলবে |
| 2 | Canonical Consistency | সব channel একই transaction service ব্যবহার করবে |
| 3 | Identity Consistency | একই employee একই ID পাবে |
| 4 | Ledger Consistency | Transaction sum = ledger total |
| 5 | Audit Consistency | প্রতিটি transaction-এ audit entry থাকবে |
| 6 | UI Consistency | সব frontend page একই API contract ব্যবহার করবে |

### Out of Scope

- ❌ নতুন Feature যোগ করা
- ❌ নতুন Architecture Design
- ❌ নতুন Transaction Pipeline
- ❌ নতুন Employee Identity System
- ❌ নতুন Ledger System
- ❌ নতুন Audit System

---

## Section 10 — Owner Approval Gates

প্রতিটি Phase-এর আগে Owner Approval Required কিনা তা স্পষ্টভাবে লিখতে হবে।

### Approval Gate Matrix

| Phase | Owner Approval Required | Decision Needed |
|-------|------------------------|-----------------|
| Phase 1: Ledger Validation | NO (read-only) | Repair execution: YES (later) |
| Phase 2: Admin Console Add | YES | Feature flag ON |
| Phase 3: UI Alignment | YES | Feature flag ON + Office Expense decision |
| Phase 4: Operator Flow | YES | Operator role + approval design + DB change (if new table) |
| Phase 5: Activity Log | NO | Always beneficial |
| Phase 6: Edit/Delete | YES | Controlled Edit + Soft Delete + Restore policy |
| Phase 7: NL Advance | YES | Feature flag ON + employee mapping |
| Phase 8: Escort Finalize | YES | Feature flag ON + employee mapping |
| Phase 9: Payroll Read | YES | Feature flag ON + WBOM data decision |
| Phase 10: Frontend | YES | Per feature flag |
| Phase 11: Regression Tests | YES | Test execution approval |
| Phase 12: Rollback Plan | NO | Plan only |

### Approval Rule

```
IF Owner Approval Required = YES
    THEN Coding শুরু করা যাবে না
    WAIT for Owner written approval
ELSE
    Proceed with caution
    Still require: backup + staging test + health check
```

---

## Section 11 — AI Governance Rules

এই Repository-তে কাজ করা ভবিষ্যতের সব AI Agent-এর জন্য Rule:

| # | Rule | Violation Consequence |
|---|------|----------------------|
| 1 | অনুমান করবে না — Evidence ছাড়া কিছু লিখবে না | Statement rejected |
| 2 | Business Rule Override করবে না | Change rejected |
| 3 | Production DB Write করবে না (owner approval ছাড়া) | Operation blocked |
| 4 | Protected Function Modify করবে না | Change rejected |
| 5 | Parallel Transaction Logic লিখবে না | Code rejected |
| 6 | Duplicate Employee Identity তৈরি করবে না | Code rejected |
| 7 | Existing Working WhatsApp Flow ভাঙবে না | Change rejected + rollback |
| 8 | Specification ছাড়া Coding করবে না | Code rejected |
| 9 | Feature Flag ছাড়া নতুন behavior deploy করবে না | Deploy rejected |
| 10 | Staging test ছাড়া production deploy করবে না | Deploy blocked |
| 11 | Backup ছাড়া DB change করবে না | Operation blocked |
| 12 | Regression test ছাড়া Phase complete ঘোষণা করবে না | Phase not complete |

### AI Agent Pre-Work Checklist

```
[ ] এই Constitution পড়েছি
[ ] Approved Refactoring Specification পড়েছি
[ ] Protected Components list দেখেছি
[ ] Owner Approval status যাচাই করেছি
[ ] Feature flag default OFF নিশ্চিত করেছি
[ ] Staging environment আছে কিনা চেক করেছি
[ ] Backup নেওয়া হয়েছে কিনা চেক করেছি
[ ] Regression test suite আছে কিনা চেক করেছি
```

---

## Section 12 — Final Governance Declaration

এই Repository-এর সর্বোচ্চ Priority (Descending Order):

| Priority | Item | Description |
|----------|------|-------------|
| ১ | **Business Constitution** | Owner-এর Business Rule সবচেয়ে উপরে |
| ২ | **Protected WhatsApp Cash Ledger Flow** | Admin ↔ Accountant WhatsApp payment flow অক্ষত |
| ৩ | **Single Employee Identity** | `fpe_employees` + `canonical_employee_id` — একটিই |
| ৪ | **Single Canonical Transaction Service** | `accounting.create_transaction()` — একটিই |
| ৫ | **Single Ledger** | `fpe_employee_ledger` — একটিই |
| ৬ | **Auditability** | প্রতিটি transaction-এ audit log বাধ্যতামূলক |
| ৭ | **Backward Compatibility** | বর্তমান working behavior ভাঙা যাবে না |
| ৮ | **Safe Refactoring** | Additive-only, feature-flagged, staging-tested |
| ৯ | **Feature Flags** | নতুন behavior default OFF, owner approval পরে ON |
| ১০ | **Minimal Risk Deployment** | সবসময় সর্বনিম্ন risk path বেছে নিতে হবে |

---

## Section 13 — Repository Scope Lock

এই Repository-এর Scope শুধুমাত্র নিম্নোক্ত Domain-এর মধ্যে সীমাবদ্ধ:

### Approved Domains

| # | Domain | Description |
|---|--------|-------------|
| ১ | Employee Management | Employee CRUD, identity resolution, canonical merge |
| ২ | Payroll | Monthly payroll compute, state machine, approval log |
| ৩ | Cash Ledger | Transaction creation, ledger upsert, reversal, audit |
| ৪ | Attendance | Attendance tracking, draft-based attendance recording |
| ৫ | Escort Operations | Escort programs, escort payment drafts, finalize |
| ৬ | Recruitment | External recruitment agent, candidate pipeline |
| ৭ | WhatsApp AI | Message ingestion, parser, AI enhancer, worker pipeline |
| ৮ | Knowledge Base | Knowledge articles, user memory, configuration |
| ৯ | Administration | RBAC, user management, API keys, audit, backup |
| ১০ | Reporting | Daily/cash/payroll/reconciliation/escort reports |

### Forbidden Domains (Out of Scope)

- ❌ নতুন Business Domain যোগ করা
- ❌ Financial Investment Module
- ❌ Inventory Module
- ❌ CRM (Customer Relationship Management)
- ❌ POS (Point of Sale)
- ❌ ERP Expansion
- ❌ Microservice Split
- ❌ Multi-company Support
- ❌ SaaS Conversion

### Scope Change Protocol

যদি Owner নতুন Business Domain যোগ করতে চান, তাহলে:

```
Step 1: Constitution Update (Owner approval)
Step 2: Architecture Review (impact analysis)
Step 3: Implementation Plan (specification)
Step 4: Owner Approval (per phase)
Step 5: Staging Test
Step 6: Production Deploy
```

**Scope Change কখনো Coding-এর মাধ্যমে শুরু করা যাবে না।**

---

## Section 14 — Compatibility Contract

Refactoring-এর পরে নিচের Behavior অপরিবর্তিত থাকতে হবে:

### Compatibility Guarantees

| # | Behavior | Current Evidence | Must Remain |
|---|----------|-----------------|-------------|
| 1 | Admin ↔ Accountant WhatsApp Payment Format | `parse_message()` detects bKash/Nagad/Cash SMS format | Unchanged |
| 2 | Payment Parser | `parser.py:parse_message()` — regex patterns for payment detection | Unchanged |
| 3 | Employee Lookup Rules | `employee.py:match_or_create_employee()` — rules 1-6 (phone, name, alias, auto-create) | Unchanged |
| 4 | Ledger Calculation | `accounting.py:_upsert_ledger()` — INSERT ON CONFLICT DO UPDATE, closing_balance formula | Unchanged |
| 5 | Transaction Reference Pattern | `fpe-{sha256(wa_message_id, employee_id, amount, period, method)}` | Unchanged |
| 6 | API Contract (non-breaking) | `GET /api/fpe/transactions`, `GET /api/fpe/employees`, etc. | Unchanged (additive only) |
| 7 | Frontend URL | `payroll.html` served at existing path | Unchanged |
| 8 | Role Mapping | `fazle_users` (admin/member), `ops_users` (admin) | Unchanged (additive only) |
| 9 | WhatsApp Bridge Integration | `bridge1`/`bridge2` to `POST /api/fpe/ingest` | Unchanged |

### Breaking Change Protocol

Breaking Change প্রয়োজন হলে নিচের ৪টি step বাধ্যতামূলক:

```
Step 1: Owner Approval
  - Written approval from Business Owner
  - Document: কেন breaking change প্রয়োজন?
  - Document: কোন behavior পরিবর্তিত হবে?
  - Document: কোন user affected হবে?

Step 2: Compatibility Report
  - Before/after behavior comparison
  - Affected components list
  - Affected users list
  - Risk assessment

Step 3: Migration Strategy
  - Phased rollout plan
  - Feature flag approach
  - Fallback mechanism
  - Timeline

Step 4: Rollback Strategy
  - Rollback procedure
  - Data rollback procedure
  - Rollback trigger criteria
  - Rollback testing
```

**এই চারটি ছাড়া কোনো Breaking Change গ্রহণযোগ্য নয়।**

### Non-Breaking Change Rules

Non-breaking change হলে (additive, feature-flagged, backward-compatible):
- Owner approval প্রয়োজন (feature flag ON করার জন্য)
- Compatibility report প্রয়োজন নয়
- Migration strategy প্রয়োজন নয়
- Rollback: feature flag OFF

---

## Appendix A — Production Runtime Evidence (2026-06-28)

### Service Status

| Service | Status | Uptime | PID |
|---------|--------|--------|-----|
| `fazle-core.service` | Active (running) | 5h 54min | 2575543 |
| `whatsapp-bridge.service` | Active (running) | 5h 54min | 2576101 |

### Database Row Counts

| Table | Rows |
|-------|------|
| `fpe_wa_messages` | 9,110 |
| `fpe_message_processing_state` | 9,110 (done=2248, skipped=6856, failed=6) |
| `fpe_cash_transactions` | 2,485 |
| `wbom_cash_transactions` | 1,428 |
| `fpe_employee_ledger` | 363 |
| `fpe_employees` | 375 |
| `wbom_employees` | 177 |
| `fpe_income_transactions` | 1 |
| `fazle_payment_drafts` | 47 (all expired) |
| `fpe_unmatched_messages` | 24 |
| `fpe_accounting_audit_logs` | 1,382 |

### Transaction `created_by` Distribution

| Created By | Count | Channel |
|------------|-------|---------|
| `fpe_engine` | 2,307 | WhatsApp Parsed Payment (PROTECTED) |
| `repair_tool` | 142 | Unknown repair tool |
| `admin_manual` | 36 | Admin Console Add |

### Audit Log Action Distribution

| Action | Count |
|--------|-------|
| `create` | 1,301 |
| `admin_soft_delete` | 33 |
| `admin_create` | 33 |
| `admin_edit` | 15 |

### Known Issues (From Audit)

| # | Issue | Severity | Evidence |
|---|-------|----------|---------|
| 1 | Ledger inconsistency | CRITICAL | Employee 14: TXN sum ৳33,400 vs Ledger ৳21,050 |
| 2 | Dual transaction table | CRITICAL | `fpe_cash_transactions` (2,485) vs `wbom_cash_transactions` (1,428) |
| 3 | All drafts expired | HIGH | 47 drafts, all `status=expired` |
| 4 | 142 repair_tool transactions | MEDIUM | Ledger update status unknown |
| 5 | 6 failed messages in DLQ | LOW | `status=failed` in processing state |
| 6 | 33 soft-deleted transactions | LOW | `deleted_at IS NOT NULL` |

---

## Appendix B — Protected WhatsApp Flow Trace (Evidence-Based)

### Live Flow (2026-06-28 15:21 UTC)

```
Step 1: WhatsApp message received
  Evidence: fpe_wa_messages = 9,110 rows

Step 2: Ingest → fpe_message_processing_state (pending)
  Evidence: 9,110 rows, 0 pending (all processed)

Step 3: Parser worker → parse_message()
  Evidence: done=2248, skipped=6856, failed=6

Step 4: Parser result → fpe_parser_results
  Evidence: Accessed via JOIN in accounting worker

Step 5: Accounting worker → validate_for_accounting()
  Evidence: 6 failed (DLQ)

Step 6: Employee match → match_or_create_employee()
  Evidence: 375 employees, 150 active

Step 7: Transaction create → create_transaction()
  Evidence: 2,307 by fpe_engine
  Live log: [fpe.acct] created txn id=2497 ref=fpe-614cc0de emp=375 amount=750.0

Step 8: Ledger update → _upsert_ledger()
  Evidence: 363 ledger rows

Step 9: Audit log → fpe_accounting_audit_logs
  Evidence: 1,301 create actions

Step 10: Frontend → GET /api/fpe/transactions
  Evidence: 200 OK, 2367 rows

Step 11: Employee totals → GET /api/fpe/employees
  Evidence: 200 OK, 150 employees
```

### Protected Flow Files (NEVER MODIFY)

```
ingestion.py:ingest_message()           — WhatsApp message entry
ingestion.py:mark_processing_status()   — FSM state transitions
ingestion.py:store_parser_result()       — Parser result persistence
ingestion.py:store_unmatched()           — Review queue insert
workers.py:message_processor_worker()    — Parser worker loop
workers.py:_process_pending_batch()      — Pending → parsed
workers.py:accounting_worker()           — Accounting worker loop
workers.py:_process_parsed_batch()       — Parsed → done
parser.py:parse_message()                — Core parser
ai_enhancer.py:ai_enhance_parse()        — AI fallback
validation.py:validate_for_accounting()  — Validation gate
employee.py:match_or_create_employee()   — Employee identity
employee.py:_resolve_canonical()         — Canonical soft-link
accounting.py:create_transaction()       — Canonical transaction service
accounting.py:_upsert_ledger()           — Canonical ledger upsert
accounting.py:reverse_transaction()      — Reversal service
routes.py:list_transactions()            — Frontend transaction visibility
routes.py:list_employees()               — Frontend employee totals
```

---

## Appendix C — Canonical Service Inventory

| Service | Function | File | Purpose |
|---------|----------|------|---------|
| Transaction Create | `create_transaction()` | `accounting.py:30` | Immutable transaction insert + idempotency + audit |
| Ledger Upsert | `_upsert_ledger()` | `accounting.py:190` | Atomic ledger update (INSERT ON CONFLICT DO UPDATE) |
| Reversal | `reverse_transaction()` | `accounting.py:106` | Reversal row (negative amount, is_reversal=TRUE) |
| Income Create | `create_income_transaction()` | `accounting.py:280` | Income transaction (separate table, no ledger) |
| Employee Match | `match_or_create_employee()` | `employee.py:94` | Employee identity resolution (rules 1-6) |
| Canonical Resolve | `_resolve_canonical()` | `employee.py:281` | Canonical soft-link follow |
| Parser | `parse_message()` | `parser.py:199` | Payment/escort/cash/income detection |
| AI Enhancer | `ai_enhance_parse()` | `ai_enhancer.py:46` | Ollama fallback for low confidence |
| Validation | `validate_for_accounting()` | `validation.py:151` | Per-type validation gate |
| Ingest | `ingest_message()` | `ingestion.py:25` | WhatsApp message idempotent insert |
| FSM Update | `mark_processing_status()` | `ingestion.py:84` | Processing state machine |
| Parser Result | `store_parser_result()` | `ingestion.py:103` | Parsed data persistence |
| Review Queue | `store_unmatched()` | `ingestion.py:129` | Unmatched message storage |

---

## Appendix D — Refactoring Specification Reference

এই Constitution `Approved Refactoring Specification` (12 Phases) এর সাথে পড়তে হবে।

| Phase | Title | Owner Approval |
|-------|-------|---------------|
| 1 | Ledger Validation | NO (read-only) |
| 2 | Admin Console Add Canonical Alignment | YES |
| 3 | Manual Entry / Add Payment UI Alignment | YES |
| 4 | Operator Submission Approval Model | YES |
| 5 | Activity Log Specification | NO |
| 6 | Controlled Edit / Soft Delete Specification | YES |
| 7 | NL Advance Canonical Alignment | YES |
| 8 | Escort Draft Finalize Canonical Alignment | YES |
| 9 | Payroll Read Path Decision | YES |
| 10 | Frontend Consistency Specification | YES |
| 11 | Regression Test Suite Specification | YES |
| 12 | Rollback and Deployment Plan | NO |

---

## Final Declaration

> **এই Document এই Repository-এর সর্বোচ্চ নীতিমালা।**
>
> ভবিষ্যতে যেকোনো AI Agent (Copilot, Claude Code, GPT, Roo) বা Developer যখন এই Repository-তে কাজ করবে:
>
> ১. **প্রথমে এই Constitution পড়তে হবে।**
> ২. **Approved Refactoring Specification পড়তে হবে।**
> ৩. **Protected Components list মেনে চলতে হবে।**
> ৪. **Owner Approval ছাড়া কোনো Coding/Refactoring/Migration/Production Change করা যাবে না।**
> ৫. **WhatsApp Cash Ledger Flow কোনোভাবেই ভাঙা যাবে না।**
> ৬. **Business Rule সবসময় Code-এর উপরে।**
>
> **Owner Authority সবসময় অক্ষুণ্ণ থাকবে।**

---

---

## Section 15 — Financial Architecture Freeze v2

**Effective:** 2026-06-29  
**Authority:** Owner Final Directive (C1B Implementation)  
**Supersedes:** All prior financial architecture decisions

### Canonical Financial Transaction Store

1. `fpe_cash_transactions` is the **only canonical financial transaction store**.
2. `wbom_cash_transactions` is a **legacy read-only archive** — no new writes permitted.
3. No developer may add a new write path to `wbom_cash_transactions`.
4. All new financial features **must** use the FPE pipeline:
   - `payment_event_from_*()` → `payment_event_to_request()` → `create_transaction()`
5. `create_transaction()` in `apps/core/modules/fazle_payroll_engine/accounting.py` is the **only canonical financial writer**.
6. The employee ledger (`fpe_employee_ledger`) is updated **only** by `create_transaction()` via `_upsert_ledger()`.
7. All financial reads must query `fpe_cash_transactions` — not `wbom_cash_transactions`.
8. The only exception is `wbom_fpe_sync.py` which reads WBOM for historical migration.
9. Any violation of this freeze requires explicit owner approval.

### Enforcement

- **Code Review**: Any PR touching `wbom_cash_transactions` with INSERT/UPDATE must be rejected.
- **Monitoring**: Health API metrics will alert if `wbom_new_writes_today > 0`.
- **Test Coverage**: Unit tests verify no WBOM writes in all payment flows.

### Certification

- Live WhatsApp Certification: **PASS** (2026-06-29)
- Read Path Certification: **PASS** (2026-06-29)
- 154 C1B-related unit tests: **ALL PASS**

---

**Document Status:** Approved  
**Owner:** Azim (Business Owner)  
**Date:** 2026-06-29  
**Version:** 2.0 (with Financial Architecture Freeze v2)

---

*End of Repository Constitution*