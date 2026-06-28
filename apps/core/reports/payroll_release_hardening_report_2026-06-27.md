# Payroll Release Hardening Report (Production)

Date: 2026-06-27
Validation target: local hardened instance on 127.0.0.1:8211
Production status: pending deploy/restart on 127.0.0.1:8200

## Scope

This report closes the Payroll Release Hardening phase with minimal, production-safe changes.
No redesign and no large rewrites were introduced.

## Part 1: RBAC Matrix

Status: Completed

Primary artifact:
- reports/payroll_rbac_matrix_2026-06-27.md

Result summary:
- Effective ALLOW/DENY matrix is now explicit (not all PASS by default).
- Role-policy conformance is PASS for Owner, SuperAdmin, Admin, HR, Accountant, OfficeAssistant01, OfficeAssistant02, Viewer.
- Office assistant mutation lock is enforced (create/edit/delete transactions DENY, read ALLOW).

## Part 2: Frontend/Backend Synchronization

Status: Completed

Verified behavior:
- Frontend resolves user capabilities from /api/admin/session before app init (persisted login path included).
- Admin tabs and admin action controls are rendered from the same permission source used by backend checks.
- Transaction mutation controls are hidden/disabled client-side when capability is not granted.

## Part 3: Audit Coverage

Status: Partial (important gaps documented)

Operation checks executed on 8211:
- Employee add: HTTP 200 (success)
- Employee edit: HTTP 200 (success)
- Income add: HTTP 200 (success)
- Admin cash transaction create: HTTP 200 (success)
- Payroll transaction edit: HTTP 200 (success)
- Payroll transaction delete: HTTP 200 (success)
- User add/role/reset: HTTP 200 (success)

Observed audit evidence:
- /api/admin/accounting-audit currently returns entity_type = transaction only.
- Transaction actions present: admin_create, admin_edit, admin_soft_delete (plus legacy create entries).
- Accounting audit rows include created_at, action, entity_type, entity_id, performed_by, reason, before_state/after_state where applicable.
- /admin/audit contains user management audit rows (user_add, user_role, user_apikey) with actor metadata and arguments.

Coverage gap:
- Employee add/edit and income add are not currently represented in /api/admin/accounting-audit.
- If strict cross-module audit parity is required, add accounting audit writes for employee and income mutations.

## Part 4: Navigation Audit

Status: Completed

Results:
- Route collision fixed by replacing catch-all payroll tab route with explicit tab routes.
- /payroll/runs now returns API data (validated HTTP 200), no longer shadowed by SPA deep-link handler.
- Mobile sidebar behavior remains in-page and logout remains the last menu action.

## Part 5: Performance Audit

Status: Completed (code-level), Pending (DB migration apply)

Completed:
- Added index migration script for high-frequency lookup paths:
  - migrations/003_add_fpe_indexes.sql

Pending:
- Migration application in production environment and confirm index existence in live DB.

## Part 6: Security Audit

Status: Partial (hardening complete, secret hygiene requires immediate ops action)

Completed hardening:
- Backend command-level RBAC enforced on /admin user-management endpoints.
- Transaction mutations in /api/admin/transactions now require role-based permission and office-assistant restriction.
- Admin session endpoint exposes explicit permission map consumed by frontend and backend.

High-priority finding:
- Plaintext credentials are present in local environment/config backups.
- Immediate action recommended: rotate exposed keys and move secret source to secure secret management (not repo-tracked plaintext files).

## Part 7: UI Polish (No Redesign)

Status: Completed

Minimal polish/fixes only:
- Fixed null access risk in header status updater.
- Fixed cash/staging amount formatter runtime error (fmt -> fmtNum).
- Added /favicon.ico 204 handler to remove repeated browser 404 noise.

## Part 8: Code Cleanup

Status: Completed

Cleanup outcomes:
- Reduced route ambiguity and improved handler explicitness.
- Unified permission checks around a shared session capability model.
- Added targeted helper routines for permission-aware admin actions.

## Part 9: Release Decision and Production Checklist

Release decision:
- Ready for production deployment after service restart and quick smoke validation.

Blocker:
- This session could not restart the production systemd unit on 8200 due sudo credential requirement.

Required production actions:
1. Deploy current code to production working tree.
2. Restart fazle-core service on 8200.
3. Run quick post-deploy checks:
   - GET /payroll/runs?period=YYYY-MM returns JSON (not SPA HTML)
   - Owner: admin + transaction mutations ALLOW
   - OfficeAssistant01/02: transaction mutation DENY with 403
   - /api/admin/session permissions match UI visibility
   - No repeated favicon 404 in browser network panel

Final state:
- Hardening changes verified on local hardened runtime (8211).
- Production verification remains pending restart/redeploy on 8200.
