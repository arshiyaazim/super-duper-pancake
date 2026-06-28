# Fazle Core Production Consolidation Plan

**Plan date:** 2026-06-13
**Scope:** Fazle Core, Bridge 1, Bridge 2, Meta WhatsApp, PostgreSQL, payroll,
escort operations, media processing, AI replies, recovery, and deployment
**Priority:** Bridge 2 and Bridge 1 data must never be silently lost
**Method:** Incremental, reversible, additive migrations with verified rollback

---

## 1. Executive Decision

Fazle Core will remain one deployable application for now. The solution is not
to create more repositories, services, databases, or overlapping modules. The
solution is to establish:

1. one durable inbound event path;
2. one durable outbound queue;
3. one canonical service for each business domain;
4. read-only projections for frontends;
5. additive database migrations;
6. continuous Bridge 1/2 backup and replay capability;
7. tested recovery procedures.

Bridge 1 and Bridge 2 SQLite stores are the most important raw records. They
must remain available, must be backed up independently, and must never be
modified by Fazle Core. Bridge 2 receives the highest recovery priority because
it contains operational, escort, accountant, and payment-related history.

Meta data loss is acceptable as a business decision, but Meta connection loss
is not. Meta configuration, webhook reachability, subscription health, and
delivery diagnostics must still be monitored and recoverable.

### Desired end state

```text
Bridge 1 / Bridge 2 / Meta
          |
authenticated channel adapter
          |
durable raw inbound event journal
          |
one identity + intent decision
          |
domain command service
          |
transactional canonical write
          |
read-only projections + review tasks
          |
durable outbound queue
          |
channel adapter + delivery receipt
```

No domain module should independently archive, deduplicate, mutate business
state, create drafts, and send messages. Each responsibility must have one
owner.

---

## 2. Non-Negotiable Production Guarantees

“Never lose data” is treated as an engineering objective supported by multiple
independent copies, durable acknowledgement, replay, and continuous
verification. No single disk or process can provide an absolute guarantee.

### 2.1 Service objectives

| Area | Objective |
|---|---|
| Bridge 2 acknowledged message RPO | 0: archive before processing/acknowledgement |
| Bridge 1 acknowledged message RPO | 0: archive before processing/acknowledgement |
| Bridge SQLite host-loss RPO | Maximum 5 minutes after snapshot automation |
| PostgreSQL host-loss RPO | Maximum 5 minutes with WAL/PITR; target below 1 minute |
| Bridge 1/2 recovery time | Under 60 minutes for app recovery; full media may take longer |
| Meta connection recovery time | Under 30 minutes |
| Business write recovery | No automatic duplicate payment, assignment, or reply |
| Deployment data loss | Zero; every deploy must support rollback without schema rollback |

### 2.2 Mandatory rules

1. Fazle Core always opens Bridge SQLite databases read-only.
2. A Bridge 1/2 event is archived durably before it is marked processed.
3. A cursor advances only after all earlier events are durably archived.
4. A processing failure never deletes or hides the raw event.
5. Every event and outbound request has a stable idempotency key.
6. No production table is dropped, renamed, or destructively rewritten during
   consolidation.
7. Historical financial or outbound actions are never replayed automatically.
8. Every migration has pre-check, post-check, rollback, and reconciliation steps.
9. Projection tables may be rebuilt; canonical records may not be overwritten.
10. Bridge 1/2 backup failure is a P0 alert.

---

## 3. Verified Current State

### 3.1 Live channel stores

| Source | Raw SQLite history | Store size | Earliest raw message |
|---|---:|---:|---|
| Bridge 1 | 8,528 messages: 5,444 inbound, 3,084 outbound | About 128 MiB including media | 2025-06-15 |
| Bridge 2 | 16,670 messages: 9,779 inbound, 6,891 outbound | About 52 MiB including media | 2024-12-01 |

Both `messages.db` files pass `PRAGMA integrity_check` and currently use SQLite
`journal_mode=delete`.

Live paths:

```text
/home/azim/whatsapp1/store/messages.db
/home/azim/whatsapp1/store/whatsapp.db
/home/azim/whatsapp2/store/messages.db
/home/azim/whatsapp2/store/whatsapp.db
```

Both bridge systemd services and `fazle-core` are active.

### 3.2 PostgreSQL message coverage

| Platform | PostgreSQL messages | Rows with source reference |
|---|---:|---:|
| Bridge 1 | 2,863 | 697 |
| Bridge 2 | 5,775 | 3,753 |

These totals cannot be directly compared with raw SQLite totals because the
poller intentionally excludes groups, newsletters, status broadcasts, empty
content, and some unsupported media. However, the coverage difference and low
Bridge 1 source-reference count require a full reconciliation audit.

### 3.3 Existing protections

- Bridge SQLite is opened read-only by the poller.
- PostgreSQL stores bridge cursors and processed message IDs.
- Message archive supports canonical phones, source references, source
  timestamps, source contexts, and content hashes.
- Recovery utilities exist for message and critical-number recovery.
- Daily PostgreSQL dumps currently exist from June 7 through June 13.
- Meta, Bridge 1, and Bridge 2 heartbeats are healthy.

### 3.4 Confirmed protection gaps

1. No regular Bridge 1/2 SQLite or full-store backups were found.
2. Existing recovery scripts use stale `/home/azim/bridges/...` paths instead of
   current `/home/azim/whatsapp1/...` and `/home/azim/whatsapp2/...` paths.
3. The SQLite poller marks many messages processed before archival succeeds.
4. The poller message-save helper catches errors instead of failing the event.
5. The poller advances a timestamp-only cursor after a batch, including after
   individual archive failures.
6. First-run cursors start near the current time and therefore do not recover
   historical gaps automatically.
7. Direct bridge sends do not have a durable retry/DLQ path.
8. PostgreSQL dumps are stored on the same VPS and do not provide host-loss
   protection.
9. Backup tests currently fail and restore tests are not automated.

---

## 4. Problems This Plan Must Resolve

### P0: Bridge 1/2 silent inbound loss risk

The current poller can insert into `processed_bridge_messages` before
`wbom_whatsapp_messages` is saved. Because `_save_message()` swallows database
errors and the cursor later advances, a transient failure can create a gap that
will not be retried.

### P0: No Bridge 1/2 disaster copy

The current SQLite stores and media are on the same VPS as the services. Disk
loss, accidental deletion, corruption, or an unsafe cleanup could remove the
only complete Bridge history.

### P0: Outbound message loss and duplicate risk

Direct sends have no durable retry record. Historical recovery must also avoid
resending messages or repeating financial/escort actions.

### P0: Release and payment inconsistency

Release workflow contains invalid schema queries and performs business writes
in multiple non-atomic steps.

### P1: Overlapping data ownership

- `wbom_employees` and `fpe_employees` overlap.
- `wbom_cash_transactions` and `fpe_cash_transactions` overlap.
- `wbom_escort_programs` and `escort_roster_entries` are both writable.
- reply and payment drafts are created from multiple paths.
- channel safety and send behavior differ.

### P1: Existing historical gaps may be unknown

Bridge 1/2 raw SQLite contains more eligible history than PostgreSQL. Existing
recovery tools are useful but have stale paths and do not yet produce a
complete gap manifest.

### P1: Regression and recovery tests are unreliable

The suite does not currently provide a trustworthy release gate. Recovery
procedures are documented but not continuously executed against isolated data.

### P2: Operational and security debt

- `app/main.py` remains a large composition/route monolith.
- Meta and bridge webhook authentication/signature policy is inconsistent.
- dashboard authentication is weak.
- Qdrant and MinIO are deployed without clear production ownership.
- `location-where` repeated `401` traffic creates high CPU and log noise.

---

## 5. Simple Target Architecture

The target is a modular monolith, not microservices.

```text
app/
  api/                 HTTP routes only
  channels/            bridge1, bridge2, meta adapters
  ingestion/           event journal, replay, cursor, dedup
  domain/
    employees/         canonical employee commands and queries
    recruitment/       restricted-source reply service
    escort/            order, assignment, lifecycle, settlement
    accounting/        immutable transactions and payroll
    messaging/         drafts, outbound queue, delivery receipts
  projections/         roster, payroll, dashboard read models
  integrations/        media processor, AI providers, storage
  operations/          scheduler, backup, health, reconciliation
```

### One-owner rule

| Responsibility | Single owner |
|---|---|
| Raw inbound event | `ingestion.event_journal` |
| Bridge cursor and replay | `ingestion.bridge_reader` |
| Phone normalization | `identity.phone` using one shared implementation |
| Identity classification | `identity.service` |
| Intent decision | `routing.intent_service` |
| Employee write | `domain.employees.service` |
| Escort write | `domain.escort.service` |
| Financial write | `domain.accounting.service` |
| Reply draft | `domain.messaging.drafts` |
| Outbound delivery | `domain.messaging.outbound` |
| Roster/payroll display | Read-only projection services |

Existing modules remain in place while adapters are introduced. No large
rewrite or “big bang” cutover is allowed.

---

## 6. Bridge 1/2 Zero-Loss Design

### 6.1 Preserve the raw sources

The live Bridge 1/2 store directories remain authoritative recovery sources.
Fazle Core must never write, vacuum, migrate, or clean these stores.

Protect:

```text
messages.db       raw message history
whatsapp.db       WhatsApp session/LID mapping and bridge state
media files       images, audio, PDFs, and documents under store directories
systemd units     executable path, working directory, ports, store paths
```

### 6.2 Add a durable inbound event journal

Create one additive table:

```text
fazle_inbound_events
  id
  source
  source_message_id
  source_timestamp
  sender_raw
  sender_canonical
  direction
  event_type
  text
  media_type
  media_path
  raw_payload_json
  payload_hash
  archive_status
  processing_status
  attempts
  last_error
  archived_at
  processed_at
```

Required constraints:

```text
UNIQUE(source, source_message_id)
INDEX(source, source_timestamp, source_message_id)
INDEX(processing_status, archived_at)
```

Raw event storage must be append-only. Business processing state may change,
but raw payload fields must not be rewritten.

### 6.3 Correct processing order

Required event lifecycle:

```text
read Bridge SQLite row
  -> normalize only enough to identify source/message ID
  -> INSERT raw inbound event ON CONFLICT DO NOTHING
  -> COMMIT archive transaction
  -> enqueue/process domain event
  -> save canonical message/business result
  -> mark event processed
  -> advance durable cursor after contiguous archived events
```

Never:

```text
mark processed -> try archive -> swallow error -> advance cursor
```

### 6.4 Cursor design

Replace timestamp-only cursor semantics with a composite checkpoint:

```text
source
last_source_timestamp
last_source_message_id
last_archived_event_id
updated_at
```

Every poll must reread an overlap window, initially 10 minutes. Unique source
message IDs make overlap safe and allow recovery from clock/timestamp edge cases.

Cursor advancement rules:

1. advance only after raw archive commit;
2. never advance past a failed archive event;
3. record blocked message ID and error;
4. alert after three failed attempts;
5. allow operator replay from any timestamp without deleting dedup records.

### 6.5 Bridge backup policy

#### Every 5 minutes: SQLite snapshots

Use SQLite online backup API or `sqlite3 .backup` separately for:

```text
bridge1/messages.db
bridge1/whatsapp.db
bridge2/messages.db
bridge2/whatsapp.db
```

Each snapshot must:

- be written to a temporary filename;
- pass `PRAGMA integrity_check`;
- be atomically renamed after validation;
- include SHA-256, size, source path, and timestamp in a manifest;
- be retained locally for at least 48 hours;
- be copied offsite.

#### Daily: complete store archive

Create a complete archive of each store directory, including media, with a
manifest and checksums. Use a snapshot/copy method that does not modify the live
store. Retain:

- 14 daily copies;
- 8 weekly copies;
- 12 monthly copies.

#### Offsite copies

At least one independent encrypted destination is mandatory, such as Backblaze
B2, S3-compatible storage, or another VPS. MinIO on the same VPS is not offsite.

Bridge 2 snapshots receive P0 alerting and are uploaded before Bridge 1 when
bandwidth/storage is constrained.

### 6.6 Bridge connection continuity

Protect the WhatsApp session databases and service definitions. Before any
bridge binary upgrade:

1. capture validated `messages.db` and `whatsapp.db` snapshots;
2. archive the current binary and systemd unit;
3. verify current connection/health;
4. upgrade only one bridge at a time;
5. start with Bridge 1 unless the change specifically targets Bridge 2;
6. confirm heartbeat, inbound test, outbound test, LID resolution, and cursor;
7. keep the other bridge untouched as operational fallback;
8. rollback binary/config immediately if session or message flow changes.

Never restart both bridges simultaneously during a planned change.

---

## 7. Historical Data Recovery Plan

Historical recovery is a separate process from live processing. Live traffic
must remain available while history is audited and imported.

### 7.1 First fix recovery tooling

Update recovery utilities to use configuration-backed current paths instead of
hardcoded stale paths:

```text
/home/azim/whatsapp1/store/
/home/azim/whatsapp2/store/
```

The recovery tool must support:

- `--dry-run` by default;
- source selection;
- timestamp range;
- phone range/list;
- inbound/outbound filtering;
- media metadata;
- manifest output;
- idempotent import using `(source, source_message_id)`;
- no business action execution;
- no outbound send;
- no AI reply;
- no cursor mutation.

### 7.2 Build a gap manifest

For every eligible Bridge 1/2 direct message:

1. resolve canonical phone using `whatsapp.db`;
2. calculate stable source key and payload hash;
3. check raw event journal and canonical message archive;
4. classify:
   - archived;
   - missing;
   - unresolved LID;
   - empty/unsupported;
   - group/status/newsletter excluded;
   - conflicting duplicate;
   - missing media file.

Produce a signed/checksummed report before any import.

### 7.3 Recovery priority

1. Bridge 2 inbound direct messages.
2. Bridge 2 outbound direct messages.
3. Bridge 2 media metadata and files.
4. Bridge 1 inbound direct messages.
5. Bridge 1 outbound direct messages.
6. Bridge 1 media metadata and files.
7. Meta recovery only if operationally useful.

### 7.4 Safe import behavior

Recovered messages are archived with:

```text
source_context = historical_recovery
source_message_ref = original Bridge message ID
processing_status = review_required
```

Recovered events must not automatically:

- send a reply;
- create or approve payment;
- close an escort duty;
- assign an employee;
- update a current conversation session;
- trigger recruitment auto-reply;
- advance live bridge cursors.

### 7.5 Recovering lost business actions

Historical messages may show that an action should have happened but did not.
Create review queues instead of executing actions:

| Historical evidence | Recovery action |
|---|---|
| Missing escort order | Create `historical_order_review` task |
| Missing assignment completion | Create `assignment_reconciliation` task |
| Missing release message | Create `release_reconciliation` task |
| Missing advance/payment instruction | Create `financial_reconciliation` task |
| Missing recruitment lead | Create `recruitment_followup_review` task |
| Missing media | Record missing-media incident; do not fabricate content |

An admin must approve every recovered business action.

### 7.6 Recovery validation

After each import batch:

1. verify imported count equals manifest-approved count;
2. verify zero outbound messages were sent;
3. verify no financial tables changed unexpectedly;
4. verify live cursor and heartbeat did not change;
5. rerun the gap scan;
6. retain before/after reports and SQL counts;
7. stop immediately on any mismatch.

---

## 8. Meta Connection Protection

Meta message history is lower priority, but connection continuity remains
required.

### Required controls

1. Preserve masked configuration metadata, WABA/phone IDs, webhook URL, Graph
   API version, and subscription fields in an encrypted operations record.
2. Keep actual tokens/secrets only in restricted secret storage.
3. Enforce Meta POST signature verification.
4. Archive raw Meta webhook events before processing.
5. Monitor:
   - public webhook verification;
   - last inbound heartbeat;
   - subscription status;
   - Graph API token validity;
   - webhook 4xx/5xx rate;
   - outbound delivery failures.
6. Provide a controlled Meta connection test that never sends a customer reply.
7. Rotate tokens without changing App ID/secret unless necessary.
8. Keep Meta auto-reply disabled until it uses the same durable journal,
   safety, and outbound queue as Bridge 1/2.

Meta recovery does not block Bridge 1/2 restoration.

---

## 9. PostgreSQL No-Loss and Migration Strategy

### 9.1 Backup layers

| Layer | Frequency | Purpose |
|---|---|---|
| Transaction/WAL archive | Continuous or at most every 5 minutes | Point-in-time recovery |
| PostgreSQL custom dump | Daily | Logical recovery and table extraction |
| Schema-only dump | Every deployment | Migration comparison |
| Critical-table export | Before every migration | Fast targeted rollback/reconciliation |
| Offsite encrypted copy | Every successful backup | VPS-loss recovery |
| Restore drill | Monthly | Prove backups are usable |

Daily dumps on the same VPS are not sufficient. WAL/PITR and offsite copies are
required before destructive cleanup or canonical-table cutover.

### 9.2 Expand-migrate-contract rule

Every schema change follows:

1. **Expand:** add nullable columns/tables/indexes; do not remove old fields.
2. **Dual-read/dual-observe:** compare old and new paths.
3. **Backfill:** write idempotently in controlled batches.
4. **Validate:** counts, checksums, referential integrity, business totals.
5. **Cut over:** change one writer at a time.
6. **Observe:** keep old path available but read-only.
7. **Contract:** archive/remove only after at least 30 days and a restore test.

No table merge is performed by deleting one side. Existing overlapping tables
remain until canonical ownership and reconciliation are proven.

### 9.3 Migration safety requirements

Before each migration:

- successful Bridge 1/2 snapshots;
- successful PostgreSQL backup and offsite confirmation;
- schema and row-count manifest;
- migration dry run on restored test DB;
- estimated lock time;
- rollback procedure;
- approved maintenance window if lock risk exists.

After each migration:

- health and heartbeat checks;
- canonical/projection count checks;
- no unexpected row decrease;
- no duplicate financial transactions;
- live Bridge message test;
- rollback if any gate fails.

---

## 10. Canonical Domain Consolidation

### 10.1 Employee identity

**Canonical owner:** Employee domain service
**Initial canonical record:** `wbom_employees`
**Secondary accounting identity:** `fpe_employees`

Plan:

1. Add/validate normalized unique phone and canonical employee linkage.
2. Build a reconciliation queue for unresolved FPE employees.
3. New operational employee creation writes WBOM first, then links FPE.
4. FPE auto-created identities remain `pending_review` until linked/approved.
5. All employee updates go through one service.
6. FPE and roster consumers read through canonical employee mapping.
7. No employee row is deleted during consolidation; use inactive/merged status.

### 10.2 Financial transactions

**Canonical owner:** Accounting domain service
**Canonical money record:** immutable FPE transaction ledger

Plan:

1. Preserve all WBOM and FPE cash records.
2. Add stable cross-reference and reconciliation status.
3. Detect possible duplicates by employee, amount, date, source, and message ID.
4. Never auto-delete or auto-merge possible duplicates.
5. Every new approved advance, salary, food, conveyance, or settlement posts one
   immutable canonical transaction.
6. Corrections use reversal/adjustment transactions, never row edits.
7. WBOM cash becomes a compatibility/read projection after validation.

### 10.3 Escort lifecycle

**Canonical owner:** Escort domain service
**Canonical record:** `wbom_escort_programs` during migration

Plan:

1. Fix invalid `shift_type` and `contact_id` queries.
2. Require canonical employee linkage for confirmed assignments.
3. Use one transaction for:

```text
validate assignment
  -> close program
  -> save release fields
  -> calculate structured settlement
  -> backfill attendance
  -> create payment review
  -> update roster projection
  -> enqueue notifications
```

4. Make `escort_roster_entries` a rebuildable read projection.
5. Frontend edit actions call Escort service, never update roster projection
   directly.
6. Replace hardcoded food/conveyance values with versioned configuration.

### 10.4 Drafts and review tasks

Create one draft/review service. Keep reply drafts, payment reviews, identity
reviews, and recovered-action reviews as distinct typed workflows.

Every draft requires:

- source event ID;
- workflow type;
- subject/reference ID;
- status;
- reviewer;
- expiry policy;
- audit history;
- idempotency key.

No module may directly insert a draft after cutover.

### 10.5 Projections

Roster, payroll summaries, dashboards, and reports are projections. They may be
recalculated or rebuilt from canonical sources and must not become independent
sources of truth.

---

## 11. Durable Outbound Messaging

All customer replies, admin notifications, accountant notifications, and
approved operational messages must use one persistent outbound queue.

### Required outbound record

```text
id
idempotency_key
source_event_id
channel
recipient
message_type
body/media reference
business_reference
status
attempts
next_attempt_at
last_error
provider_message_id
created_at/sent_at/delivered_at
```

### Delivery rules

1. Business transaction and outbound enqueue occur in one PostgreSQL transaction.
2. Channel worker sends only committed queue rows.
3. Retry uses exponential backoff.
4. Permanent failures enter DLQ and alert operations.
5. Idempotency key prevents duplicate sends.
6. Delivery status is separate from business/payment status.
7. Internal operation notifications use
   `INTERNAL_OPERATION_NOTIFICATIONS_ENABLED`, independent of customer
   auto-reply.
8. Direct `bridge.send()` from business modules is removed after shadow/cutover.

This resolves both silent send loss and safe-mode suppression of internal
operations.

---

## 12. AI, Media, and Learning Boundaries

### AI may

- classify free-form intent;
- extract structured fields with confidence;
- generate recruitment replies from the approved source;
- draft general replies from approved context;
- suggest employee/escort matches;
- summarize for human review.

### AI may not

- approve or post money;
- create an authoritative employee identity;
- close a duty;
- assign an escort;
- change a canonical rate;
- send an unreviewed high-risk reply;
- learn financial facts automatically.

### Media processor may

- identify document/media type;
- OCR/transcribe;
- extract candidate fields;
- preserve media reference and confidence.

It may not mutate escort, employee, payment, or payroll state.

### Recruitment remains isolated

Recruitment replies continue to use only:

```text
resources/ops/recruitment_source_of_truth.txt
```

Recruitment source/version changes require review and tests. General KB/RAG,
memory, and unrelated files remain excluded from recruitment replies.

---

## 13. Testing and Release Gates

### 13.1 Build a reliable test environment first

1. Create isolated PostgreSQL and Redis test services.
2. Use copied/synthetic Bridge SQLite fixtures, never live stores.
3. Separate pure unit, DB, integration, workflow, recovery, and E2E tests.
4. Stop collecting operational scripts as normal tests.
5. Register pytest markers and fixture loop scope.
6. Fix current backup, context-memory, write-router, parser, and OCR test drift.

### 13.2 Mandatory zero-loss tests

- archive fails: message remains retryable and cursor does not advance;
- process fails after archive: raw event remains and can replay;
- duplicate source event: only one raw event/business action;
- same timestamp, different message IDs: both archived;
- restart during batch: no gap and no duplicate action;
- PostgreSQL unavailable: Bridge stores remain untouched and backlog recovers;
- Bridge unavailable: health alert and no cursor mutation;
- outbound send timeout: retry/DLQ without duplicate business action;
- recovery import: no outbound send and no live cursor mutation;
- Bridge 2 full restore from offsite copy;
- PostgreSQL point-in-time restore and reconciliation.

### 13.3 Deployment gates

A production deploy is allowed only when:

- required tests pass;
- Bridge 1/2 and PostgreSQL backups are current and validated;
- migration dry run passes;
- no unresolved P0 alert exists;
- rollback artifact is available;
- one operator is watching logs/metrics;
- post-deploy Bridge 1/2 test messages archive correctly.

---

## 14. Phased Delivery Plan

Each phase is independently reversible. Do not start the next phase until the
exit gate is met.

### Phase 0: Freeze and baseline

**Goal:** Prevent avoidable loss while preparing changes.

Actions:

1. Keep global general auto-reply and generic outbound disabled.
2. Do not change Bridge binary/session/store paths.
3. Record live row counts, cursors, heartbeats, service units, file checksums,
   and current configuration flags.
4. Create immediate validated Bridge 1/2 SQLite snapshots and full-store copies.
5. Create PostgreSQL backup and verify restore in an isolated database.
6. Copy backups offsite.

Exit gate:

- all four Bridge databases have validated local and offsite copies;
- PostgreSQL restore succeeds;
- baseline manifest is stored.

Rollback: no runtime change.

### Phase 1: Repair backup and recovery foundation

**Goal:** Make recovery possible before changing ingestion.

Actions:

1. Implement scheduled SQLite snapshot and full-store backup.
2. Add manifests, checksums, integrity checks, retention, and alerts.
3. Fix recovery tool paths and make them configuration-backed.
4. Add dry-run gap manifest generation.
5. Add monthly Bridge and PostgreSQL restore drills.

Exit gate:

- 24 hours of successful Bridge snapshots;
- one successful offsite restore of Bridge 2;
- recovery dry-run completes without writes.

Rollback: disable new backup jobs; existing stores unchanged.

### Phase 2: Add raw inbound event journal

**Goal:** Archive every event before business processing.

Actions:

1. Add event-journal and composite-checkpoint tables.
2. Shadow-write raw events while current pipeline still operates.
3. Compare shadow archive with SQLite rows continuously.
4. Add alert for any eligible source row absent from the journal.

Exit gate:

- seven days with zero unexplained Bridge 1/2 shadow gaps;
- no current pipeline regression;
- journal import/replay tests pass.

Rollback: stop shadow writer; additive tables remain.

### Phase 3: Cut over archive-before-process

**Goal:** Remove silent inbound loss risk.

Actions:

1. Process only from committed raw events.
2. Advance composite cursor only after contiguous archive commit.
3. Keep overlap replay enabled.
4. Remove/archive pre-mark behavior from the live path.
5. Fail loudly instead of swallowing archive errors.

Cutover order:

1. Bridge 1
2. observe for 48 hours
3. Bridge 2

Exit gate:

- restart/failure tests prove no gap;
- live reconciliation shows zero unexplained missing events;
- Bridge 2 runs 72 hours without archive gap.

Rollback: restore previous reader while journal continues to collect.

### Phase 4: Historical reconciliation and recovery

**Goal:** Identify and safely recover existing gaps.

Actions:

1. Generate complete Bridge 2 gap manifest.
2. Admin reviews classifications and approved import ranges.
3. Import missing Bridge 2 raw/canonical messages without actions.
4. Create business reconciliation tasks.
5. Repeat for Bridge 1.
6. Record unresolved LID and missing-media cases.

Exit gate:

- every eligible raw Bridge 1/2 message is archived, excluded with reason, or
  listed as unresolved;
- zero historical sends or automatic financial writes occurred.

Rollback: imported messages remain immutable; incorrect classifications are
corrected by metadata/reversal, not deletion.

### Phase 5: Durable outbound and internal notifications

**Goal:** Prevent silent send loss and restore operations notifications.

Actions:

1. Introduce channel-neutral outbound queue.
2. Route internal notifications through queue under separate flag.
3. Shadow direct sends and compare results.
4. Cut over admin notifications first.
5. Cut over approved drafts.
6. Cut over recruitment auto-replies.
7. Keep Meta auto-reply disabled.

Exit gate:

- all sends have durable queue records and delivery outcomes;
- retry/DLQ tests pass;
- no duplicate sends during restart/failure tests.

Rollback: pause workers; queued messages remain durable for review.

### Phase 6: Repair escort release and settlement

**Goal:** Make the highest-risk business workflow transactional.

Actions:

1. Fix schema mismatches.
2. Create structured settlement model.
3. Version food/conveyance/rate configuration.
4. Implement one transactional release command.
5. Route payment through canonical accounting.
6. Rebuild roster projection after commit.
7. Add release/settlement review frontend.

Exit gate:

- contract/workflow tests pass;
- test release produces one program close, one settlement, one payment review,
  one roster update, and no duplicate money event.

Rollback: disable new command handler; no schema rollback.

### Phase 7: Employee and accounting reconciliation

**Goal:** Remove identity and money ambiguity without data loss.

Actions:

1. Reconcile FPE employees to WBOM employees.
2. Review ambiguous identities manually.
3. Add canonical cross-references.
4. Reconcile WBOM/FPE transactions and mark possible duplicates.
5. Move new writes through canonical services.
6. Keep old tables read-only/compatible during observation.

Exit gate:

- all active employees linked or explicitly unresolved;
- all new financial actions create one canonical immutable transaction;
- no automatic row deletion/merge.

Rollback: switch writers back; additive links/statuses remain.

### Phase 8: Projection and frontend simplification

**Goal:** Make UI behavior reflect canonical services.

Actions:

1. Make roster and payroll views read projections.
2. Replace direct projection edits with domain actions.
3. Add reconciliation/review pages.
4. Add program timeline and audit views.
5. Rebuild projections and compare totals.

Exit gate:

- projections can be deleted in test and rebuilt exactly;
- UI no longer creates independent business records.

Rollback: keep old UI routes available read-only.

### Phase 9: Security, Meta, and operational hardening

Actions:

1. Enforce webhook authentication/signatures.
2. Rotate exposed credentials.
3. Improve dashboard sessions and 2FA.
4. Add Meta connection test/alerts.
5. Diagnose and fix `location-where` 401 retry loop/high CPU.
6. Add media processor memory trend and restart policy.
7. Add offsite backup dashboard and restore evidence.

Exit gate:

- no exposed/expired critical credential;
- Meta connection monitoring passes;
- location-where unauthorized retry storm is resolved.

### Phase 10: Controlled cleanup

Only after at least 30 days of stable canonical operation:

1. classify old modules/tables as compatibility, archived, or removable;
2. verify zero production callers and successful restore;
3. archive code before removal;
4. export and archive data before any table drop;
5. remove one item per deployment;
6. retain audit evidence.

No cleanup is part of earlier phases.

---

## 15. Monitoring and Alerts

### P0 alerts

- Bridge 2 SQLite snapshot older than 10 minutes.
- Bridge 1 SQLite snapshot older than 10 minutes.
- Bridge database integrity check failure.
- Bridge heartbeat stale more than 2 minutes.
- Raw event archive failure or cursor blocked.
- Eligible Bridge 1/2 source event missing from journal.
- PostgreSQL WAL/offsite backup failure.
- Outbound DLQ contains financial/admin operational message.
- Duplicate financial transaction/idempotency violation.

### P1 alerts

- Meta heartbeat stale more than 6 hours.
- Meta verification/subscription/token check fails.
- roster projection differs from canonical escort programs.
- unresolved employee/transaction reconciliation count increases.
- expired payment review count increases.
- media processor RSS grows beyond approved threshold.
- test/restore drill fails.

### Required dashboards

1. Bridge source vs journal vs canonical archive coverage.
2. Cursor lag and blocked event.
3. Backup age, integrity, offsite copy, and restore status.
4. Outbound pending/retry/DLQ/delivery status.
5. Employee and financial reconciliation.
6. Escort lifecycle and projection drift.
7. Meta connection health.

---

## 16. Recovery Runbooks

### 16.1 Fazle Core failure, bridges healthy

1. Do not restart bridges.
2. Verify Bridge SQLite snapshots continue.
3. Restore/start Fazle Core.
4. Confirm journal resumes from composite checkpoint with overlap.
5. Verify no raw event gap.
6. Resume outbound worker only after queue review.

### 16.2 PostgreSQL failure

1. Pause business processing and outbound workers.
2. Keep bridges running and snapshotting.
3. Restore PostgreSQL using PITR/dump.
4. Reconcile Bridge raw events from last known checkpoint.
5. Import missing raw events without actions.
6. Review pending outbound queue before sending.

### 16.3 Bridge 2 failure

1. Declare P0; do not modify/delete its store.
2. Capture current files/checksums if readable.
3. Restore service/binary/config without replacing session DB unnecessarily.
4. If store is corrupt, restore latest validated snapshot to a separate path.
5. Validate integrity and compare manifest before cutover.
6. Reconnect, verify heartbeat and controlled inbound/outbound test.
7. Replay missing interval into raw journal.
8. Review any operational/financial actions manually.

### 16.4 Bridge 1 failure

Use the same procedure as Bridge 2. Bridge 1 remains zero-loss despite lower
business criticality.

### 16.5 Meta failure

1. Keep Bridge 1/2 unaffected.
2. Verify public webhook, subscription, token, and logs.
3. Restore/rotate Meta configuration.
4. Run controlled verification and inbound test.
5. Do not enable Meta auto-reply during recovery.

### 16.6 Accidental historical gap discovered

1. Freeze cleanup and related migrations.
2. Generate dry-run gap manifest.
3. Preserve all current sources and backups.
4. Import raw/canonical messages only.
5. Create review tasks for missing business actions.
6. Never replay outbound or financial actions automatically.

---

## 17. Risk Matrix

| Change | Risk | Mitigation | Rollback |
|---|---|---|---|
| Bridge snapshot automation | Low | Online backup API, integrity check, temp+rename | Disable job |
| Raw event shadow journal | Low | Additive table, no behavior change | Stop shadow writer |
| Archive-before-process cutover | Medium | Bridge 1 first, overlap replay, tests | Restore old reader |
| Historical import | Medium | Dry-run, idempotent, no actions/sends | Correct metadata; no deletes |
| Durable outbound cutover | Medium | Shadow, idempotency, queue pause | Pause worker/use old path |
| Release transaction rewrite | High | Isolated tests, review-only launch | Disable new handler |
| Employee/accounting reconciliation | High | Additive links, manual ambiguity review | Switch writer/read path |
| Projection rebuild | Medium | Canonical comparison | Restore/rebuild old projection |
| Table/module deletion | Very high | Delay 30+ days, archive/export/restore test | Restore archive |

---

## 18. Actions Explicitly Prohibited

1. Do not delete or overwrite Bridge 1/2 store directories.
2. Do not run SQLite write operations, vacuum, or schema migrations on live
   bridge databases from Fazle Core.
3. Do not restart both bridges together for planned changes.
4. Do not reset bridge cursors to current time to hide backlog.
5. Do not delete dedup rows to force uncontrolled replay.
6. Do not auto-replay historical replies, payments, releases, or assignments.
7. Do not merge/delete WBOM/FPE records automatically.
8. Do not make roster/payroll projections independent canonical writers.
9. Do not enable Meta/general auto-reply before durable journal/outbound cutover.
10. Do not perform destructive schema cleanup without offsite backup and restore
    proof.
11. Do not treat a healthy `/health` response as proof of data consistency.

---

## 19. Final Acceptance Criteria

The consolidation is complete only when all conditions are true:

### Bridge safety

- Every eligible Bridge 1/2 direct message has an archived raw event or an
  explicit exclusion reason.
- Bridge 1/2 cursors cannot advance past archive failures.
- Validated Bridge database snapshots exist every five minutes locally and
  offsite.
- Full Bridge stores, including media/session data, have tested restore copies.
- Restart, outage, and PostgreSQL failure tests show no unexplained gaps.

### Messaging

- Every outbound message is queued durably with idempotency and delivery state.
- No business module directly sends a message.
- Internal notifications operate independently from customer auto-reply.
- Meta connection is monitored and recoverable.

### Business data

- Employee identities are linked or explicitly unresolved.
- New financial writes create one immutable canonical transaction.
- Escort release/settlement is atomic and tested.
- Roster and payroll displays are rebuildable projections.
- Historical recovery never caused duplicate payment, reply, release, or
  assignment.

### Operations

- PostgreSQL PITR/offsite backups and monthly restore drills pass.
- Bridge restore drills pass.
- Required tests provide a reliable deployment gate.
- No P0 alerts remain unresolved.
- Every production change has stored pre/post manifests and rollback evidence.

---

## 20. Recommended Delivery Order

```text
1. Freeze and baseline
2. Bridge 1/2 backup + offsite restore proof
3. Recovery-tool path fix + dry-run manifest
4. Raw event shadow journal
5. Archive-before-process cutover: Bridge 1 then Bridge 2
6. Historical Bridge 2 recovery, then Bridge 1
7. Durable outbound and internal notifications
8. Escort release/settlement repair
9. Employee/accounting reconciliation
10. Projection/frontend simplification
11. Meta/security/operations hardening
12. Delayed controlled cleanup
```

The first production implementation should be Bridge 1/2 backup and recovery
foundation. No consolidation change is safe until the raw message stores and
PostgreSQL can be independently restored.

---

*This plan supersedes earlier consolidation instructions that described the
live Bridge paths as missing or recommended enabling general auto-reply before
durable ingestion and delivery were established.*
