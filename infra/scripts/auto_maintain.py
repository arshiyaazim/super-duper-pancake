#!/usr/bin/env python3
"""
auto_maintain.py — nightly contact tier classification + draft expiry

Features:
  1. wbom_contacts tier reclassification
       Priority: admin > active_escort > has_payment > identity_role
                 > preserve existing name_prefix > none/delete_candidate
       - is_protected contacts: never touched
       - existing tier1/name_prefix: preserved unless promoted to a stronger category
  2. fazle_draft_replies: pending > 48h → expired
  3. Summary with row counts + timestamp

Usage:
  auto_maintain.py           — live run (for cron)
  auto_maintain.py --dry-run — counts only, no DB changes
"""
import argparse
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

try:
    import psycopg2
    import psycopg2.extras
except ImportError:
    print("ERROR: psycopg2 not available. Install: sudo apt install python3-psycopg2", file=sys.stderr)
    sys.exit(1)


def get_db_url() -> str:
    env_file = Path('/home/azim/core/.env')
    if env_file.exists():
        for line in env_file.read_text().splitlines():
            line = line.strip()
            if line.startswith('DATABASE_URL=') and not line.startswith('DATABASE_URL_TEMPLATE='):
                return line.split('=', 1)[1].strip().strip('"\'')
            if line.startswith('DATABASE_URL_TEMPLATE='):
                val = line.split('=', 1)[1].strip().strip('"\'')
                return val.replace('__HOST__', '172.20.0.3')
    return os.environ.get(
        'DATABASE_URL',
        'postgresql://postgres:3UTioVfpNwVgcZ2VtlEr9XDR5C8PSOb@172.20.0.3:5432/postgres',
    )


def get_admin_phones() -> list:
    phones = set()
    env_file = Path('/home/azim/agent/.env')
    if env_file.exists():
        for line in env_file.read_text().splitlines():
            if line.startswith('OWNER_PHONE='):
                p = line.split('=', 1)[1].strip()
                if p:
                    phones.add(p)
            elif line.startswith('ADMIN_PHONES='):
                for p in line.split('=', 1)[1].split(','):
                    p = p.strip()
                    if p:
                        phones.add(p)
    phones.update(['8801880446111', '8801958122300'])
    return list(phones)


# CTE assigns a new_reason to each non-protected contact.
# new_reason IS NULL  →  none/delete_candidate
# Priority: admin > active_escort > has_payment > identity_role
#           > preserve existing name_prefix > NULL (none)
TIER_CTE = """
WITH tier_assign AS (
    SELECT
        wc.contact_id,
        wc.keep_tier            AS old_tier,
        wc.keep_reason          AS old_reason,
        wc.marked_for_delete_at AS old_mark,
        CASE
            WHEN wc.whatsapp_number = ANY(%(admin_phones)s)
                THEN 'admin'
            WHEN EXISTS (
                SELECT 1 FROM wbom_escort_programs ep
                WHERE ep.contact_id = wc.contact_id
                  AND ep.status != 'Completed'
            ) THEN 'active_escort'
            WHEN (
                EXISTS (
                    SELECT 1 FROM fpe_employees fe
                    WHERE fe.primary_phone IS NOT NULL
                      AND fe.primary_phone ~ '^0[0-9]+'
                      AND wc.whatsapp_number = '880' || substring(fe.primary_phone FROM 2)
                )
                OR EXISTS (
                    SELECT 1 FROM fpe_cash_transactions ft
                    WHERE ft.payout_phone IS NOT NULL
                      AND ft.payout_phone ~ '^0[0-9]+'
                      AND wc.whatsapp_number = '880' || substring(ft.payout_phone FROM 2)
                )
            ) THEN 'has_payment'
            WHEN EXISTS (
                SELECT 1 FROM fazle_contact_roles fcr
                WHERE (
                    fcr.phone = wc.whatsapp_number
                    OR (fcr.phone ~ '^0[0-9]+' AND wc.whatsapp_number = '880' || substring(fcr.phone FROM 2))
                )
                  AND fcr.is_active = true
            ) THEN 'identity_role'
            WHEN wc.keep_tier = 'tier1' AND wc.keep_reason = 'name_prefix'
                THEN 'name_prefix'
            ELSE NULL
        END AS new_reason
    FROM wbom_contacts wc
    WHERE wc.is_protected IS NOT TRUE
)
"""


def run(dry_run: bool) -> None:
    ts = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    mode = 'DRY RUN' if dry_run else 'LIVE RUN'
    print(f"\n{'=' * 50}")
    print(f"  auto_maintain.py  [{mode}]  {ts}")
    print(f"{'=' * 50}\n")

    db_url = get_db_url()
    admin_phones = get_admin_phones()
    params = {'admin_phones': admin_phones}

    conn = psycopg2.connect(db_url)
    conn.autocommit = False
    cur = conn.cursor()

    try:
        # ── Current state snapshot ──────────────────────────────────────────
        cur.execute("""
            SELECT keep_tier, keep_reason, COUNT(*)
            FROM wbom_contacts
            GROUP BY keep_tier, keep_reason
            ORDER BY COUNT(*) DESC
        """)
        current_rows = cur.fetchall()

        # ── Dry-run classification breakdown ────────────────────────────────
        cur.execute(
            TIER_CTE + """
            SELECT
                CASE WHEN new_reason IS NOT NULL THEN 'tier1' ELSE 'none' END AS new_tier,
                COALESCE(new_reason, 'delete_candidate') AS new_reason,
                COUNT(*) AS n
            FROM tier_assign
            GROUP BY 1, 2
            ORDER BY n DESC
            """,
            params,
        )
        new_breakdown = cur.fetchall()

        # ── name_prefix: how many preserved vs promoted ─────────────────────
        cur.execute(
            TIER_CTE + """
            SELECT new_reason, COUNT(*) FROM tier_assign
            WHERE old_tier = 'tier1' AND old_reason = 'name_prefix'
            GROUP BY new_reason
            """,
            params,
        )
        name_prefix_rows = cur.fetchall()
        # new_reason='name_prefix' → preserved; anything else → promoted to stronger category
        cnt_name_prefix_preserved = sum(n for r, n in name_prefix_rows if r == 'name_prefix')
        cnt_name_prefix_promoted  = sum(n for r, n in name_prefix_rows if r != 'name_prefix')

        # ── Soft-delete candidates (already none + marked >30d) ─────────────
        cur.execute("""
            SELECT COUNT(*) FROM wbom_contacts
            WHERE keep_tier = 'none'
              AND marked_for_delete_at IS NOT NULL
              AND marked_for_delete_at < NOW() - INTERVAL '30 days'
              AND is_active = true
              AND is_protected IS NOT TRUE
        """)
        cnt_soft_delete = cur.fetchone()[0]

        # ── Draft expiry count ───────────────────────────────────────────────
        cur.execute("""
            SELECT COUNT(*) FROM fazle_draft_replies
            WHERE status = 'pending'
              AND created_at < NOW() - INTERVAL '48 hours'
        """)
        cnt_expired_drafts = cur.fetchone()[0]

        # ── Print results ────────────────────────────────────────────────────
        print("Feature 1 — Contact tier reclassification")
        print("-" * 42)
        print("Current state (before run):")
        for tier, reason, n in current_rows:
            print(f"  {str(tier):10}  {str(reason):22}  {n:4d}")

        print("\nNew classification (this run will produce):")
        for new_tier, new_reason, n in new_breakdown:
            print(f"  {new_tier:10}  {new_reason:22}  {n:4d}")

        print(f"\n  name_prefix contacts ({cnt_name_prefix_preserved + cnt_name_prefix_promoted} total):")
        print(f"    preserved as tier1/name_prefix : {cnt_name_prefix_preserved}")
        if cnt_name_prefix_promoted:
            for reason, n in name_prefix_rows:
                if reason != 'name_prefix':
                    print(f"    promoted → tier1/{reason:18} : {n}")

        print(f"\n  Soft-delete ready (none + marked >30 days): {cnt_soft_delete} → is_active=false")

        print(f"\nFeature 2 — fazle_draft_replies expiry")
        print("-" * 42)
        print(f"  pending > 48h → expired: {cnt_expired_drafts}")

        if dry_run:
            print(f"\n[dry-run] No changes made. Run without --dry-run to apply.")
            conn.rollback()
            return

        # ════════════════════════════════════════════════════════════════════
        # LIVE: Apply changes
        # ════════════════════════════════════════════════════════════════════

        # 1a. Reclassify all non-protected contacts in one pass
        cur.execute(
            TIER_CTE + """
            UPDATE wbom_contacts wc
            SET
                keep_tier   = CASE WHEN ta.new_reason IS NOT NULL THEN 'tier1' ELSE 'none' END,
                keep_reason = COALESCE(ta.new_reason, 'delete_candidate'),
                marked_for_delete_at = CASE
                    WHEN ta.new_reason IS NOT NULL THEN NULL
                    WHEN ta.old_mark IS NULL       THEN NOW()
                    ELSE ta.old_mark
                END,
                updated_at  = NOW()
            FROM tier_assign ta
            WHERE ta.contact_id = wc.contact_id
              AND (
                  wc.keep_tier    IS DISTINCT FROM CASE WHEN ta.new_reason IS NOT NULL THEN 'tier1' ELSE 'none' END
                  OR wc.keep_reason IS DISTINCT FROM COALESCE(ta.new_reason, 'delete_candidate')
              )
            """,
            params,
        )
        upd_tier = cur.rowcount

        # 1b. Soft-delete: none + marked > 30 days
        cur.execute("""
            UPDATE wbom_contacts
            SET is_active = false, updated_at = NOW()
            WHERE keep_tier = 'none'
              AND marked_for_delete_at IS NOT NULL
              AND marked_for_delete_at < NOW() - INTERVAL '30 days'
              AND is_active = true
              AND is_protected IS NOT TRUE
        """)
        upd_soft_delete = cur.rowcount

        # 2. Expire stale drafts
        cur.execute("""
            UPDATE fazle_draft_replies
            SET status = 'expired', edited_at = NOW()
            WHERE status = 'pending'
              AND created_at < NOW() - INTERVAL '48 hours'
        """)
        upd_drafts = cur.rowcount

        conn.commit()

        print(f"\n[live] Changes committed:")
        print(f"  tier reclassified  : {upd_tier:4d} rows")
        print(f"  soft-deleted       : {upd_soft_delete:4d} rows  (is_active → false)")
        print(f"  drafts expired     : {upd_drafts:4d} rows  (status → expired)")

    except Exception as exc:
        conn.rollback()
        print(f"\n[ERROR] {exc}", file=sys.stderr)
        raise
    finally:
        cur.close()
        conn.close()

    print(f"\n[done] {datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')}")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Nightly contact maintenance')
    parser.add_argument('--dry-run', action='store_true', help='Count only — no DB changes')
    args = parser.parse_args()
    run(dry_run=args.dry_run)
