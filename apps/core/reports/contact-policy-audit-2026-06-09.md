# Contact Policy Audit Report

**Generated:** 2026-06-09 12:04 UTC  
**Host:** VPS iamazim (5.189.131.48) — PostgreSQL ai-postgres container  
**Purpose:** Auto-reply block/allow decisions — SELECT-only audit, no DB modifications  
**Total contacts audited:** 576

---

## 1. Executive Summary

| Bucket | Count | Recommended Action |
|--------|------:|-------------------|
| `protected_admin` | 3 | preserve_and_allow_manual_only |
| `keyword_block_no_autoreply` | 6 | preserve_and_block_autoreply |
| `env_prefix_match` | 467 | preserve_and_needs_review |
| `active_escort` | 6 | preserve_and_allow_manual_only |
| `has_payment` | 67 | preserve_and_allow_manual_only |
| `identity_role` | 7 | preserve_and_allow_manual_only |
| `unclear_relation_needs_review` | 12 | preserve_and_needs_review |
| `low_evidence_unknown` | 8 | safe_delete_candidate |

---

## 2. Human vs Bot Evidence Detection

### Heuristic applied

| Source | Human signal | Bot/auto signal |
|--------|-------------|-----------------|
| `fazle_draft_replies` | `approved_at IS NOT NULL` OR `sent_at IS NOT NULL` | `reviewed=false AND draft_only=true AND status IN (draft, pending)` |
| `wbom_whatsapp_messages outbound` | ⚠ **Cannot confidently separate** | No `source='admin'` column; `template_used_id` NULL for all outbound; no manual-send flag |
| `fazle_outbound_queue` | None reliably | All purposes are system-generated (daily-digest, backup-stale, payroll-daily, health-summary) |

> **Assumption:** Only `fazle_draft_replies` rows with `approved_at` or `sent_at` are treated as human-reviewed evidence (123 approved, 12 sent, 180 unique recipients). All other outbound message counts are treated as **ambiguous** and used only as a soft signal for the `unclear_relation_needs_review` bucket.

---

## 3. Schema Findings

### Message tables relevant to audit

| Table | Key columns | Used for |
|-------|-------------|----------|
| `wbom_whatsapp_messages` | `contact_id`, `direction` (inbound/outbound), `status`, `received_at`, `canonical_phone` | Message volume per contact |
| `fazle_draft_replies` | `recipient`, `status`, `reviewed`, `approved_at`, `sent_at`, `edited_at` | Human evidence |
| `fpe_wa_messages` | `source_number`, `is_from_me`, `timestamp_wa` | FPE (payroll engine) messages — no direct contact link |
| `llm_conversation_log` | `user_id`, `ts` | LLM calls log — no direct contact link |
| `fazle_outbound_queue` | `recipient`, `purpose`, `status`, `sent_at` | System-generated outbound — all bot |

### Phone normalization

- `wbom_contacts.whatsapp_number`: international format `880XXXXXXXXX` (13 digits)
- `fpe_employees.primary_phone`, `fpe_cash_transactions.payout_phone`, `fazle_contact_roles.phone`: local format `01XXXXXXXXX` (11 digits)
- Normalization: `'880' || substring(local_phone FROM 2)` — applied consistently across all joins

---

## 4. DRAFT_NAME_PREFIXES

From `/home/azim/core/.env`:

```
DRAFT_NAME_PREFIXES=client,escort,office
```

Contacts matching env prefix OR `keep_reason='name_prefix'`: **467**

---

## 5. Keyword-Flagged Contacts

> ⚠️ **Important caveat:** `Al-Aqsa` is the **business name itself** (Al-Aqsa Security Service & Trading Centre).
> The 4 matching contacts are internal branch/staff contacts, not external blocked entities.
> `Operation` in display_name appears to be a **staff role designation** (e.g. "Operation Jasim" = supervisor).
> Consider reviewing whether `al-aqsa` and `operation` keywords should be excluded from the hard-block list for internal business contacts.

| contact_id | whatsapp_number | display_name | company | keep_tier | keyword hit | recommended |
|-----------|----------------|-------------|---------|-----------|-------------|-------------|
| 368671 | 8801787635690 | Operation R E D Y M A X | - | tier1 | `operation` | preserve_and_block_autoreply |
| 2672 | 8801849283561 | al-aqsasecuritygroup 🙅‍♂️ | - | tier1 | `al-aqsa` | preserve_and_block_autoreply |
| 639087 | 8801849258074 | Saiful Operation | - | tier1 | `operation` | preserve_and_block_autoreply |
| 2115 | 8801958122302 | Al-Aqsa Nimtola | - | tier1 | `al-aqsa` | preserve_and_block_autoreply |
| 1194 | 8801958122301 | Al-Aqsa AK Khan | - | tier1 | `al-aqsa` | preserve_and_block_autoreply |
| 2880 | 8801958122303 | Operation Jasim | - | tier1 | `operation` | preserve_and_block_autoreply |

---

## 6. Top 25 Unclear — needs_review Contacts

| # | whatsapp_number | display_name | interaction_count | outbound_n | last_human_evidence | evidence_note |
|---|----------------|-------------|-------------------|-----------|---------------------|---------------|
| 1 | 8801814504737 | মায়া সংঘ ❤️ | 8 | 0 | - | interaction_count=8 |
| 2 | 8801322805911 | Anik Grameenphone Corporate | 34 | 0 | 2026-05-09 | interaction_count=34; human draft evidence at 2026-05-09 |
| 3 | 8801750107303 | Roŋƴ Fʌɩsʌɭ | 10 | 0 | 2026-04-26 | interaction_count=10; human draft evidence at 2026-04-26 |
| 4 | 8801404446103 | Banglalink Area Manager | 21 | 0 | - | interaction_count=21 |
| 5 | 8801779415282 | somir sutradhar | 12 | 0 | - | interaction_count=12 |
| 6 | 8801818516678 | Nilufar Sultana | 8 | 0 | - | interaction_count=8 |
| 7 | 8801846299708 | Soniya | 5 | 0 | 2026-05-09 | human draft evidence at 2026-05-09 |
| 8 | 8801911224342 | mdhero67120 | 19 | 0 | 2026-05-09 | interaction_count=19; human draft evidence at 2026-05-09 |
| 9 | 8801607105023 | Rakim | 14 | 3 | 2026-05-09 | interaction_count=14; human draft evidence at 2026-05-09 |
| 10 | 8801805675218 | Shoydul | 25 | 9 | - | interaction_count=25 |
| 11 | 8801857940519 | hasan | 16 | 1 | - | interaction_count=16 |
| 12 | 8801994935070 | komolshil5070 | 61 | 22 | - | interaction_count=61; outbound_n=22 |

---

## 7. Bucket Definitions & Confidence

| Bucket | Logic | Confidence | Recommended Action |
|--------|-------|-----------|-------------------|
| `protected_admin` | `is_protected=true` in wbom_contacts | high | preserve_and_allow_manual_only |
| `keyword_block_no_autoreply` | display_name/company_name/notes matches hard-block keyword | high | preserve_and_block_autoreply |
| `env_prefix_match` | display_name starts with client/escort/office OR `keep_reason=name_prefix` | high | preserve_and_needs_review |
| `active_escort` | EXISTS in wbom_escort_programs WHERE status != 'Completed' | high | preserve_and_allow_manual_only |
| `has_payment` | phone (normalized) found in fpe_employees OR fpe_cash_transactions | high | preserve_and_allow_manual_only |
| `identity_role` | phone (normalized) found in fazle_contact_roles WHERE is_active=true | high | preserve_and_allow_manual_only |
| `unclear_relation_needs_review` | interaction_count>5 OR human draft evidence OR outbound_n>10 | medium | preserve_and_needs_review |
| `low_evidence_unknown` | no match in any category above | low | safe_delete_candidate |

---

## 8. Full Contact List (summary)

Full data in CSV: `contact-policy-review-{TODAY}.csv`

