# RBAC Matrix (Payroll Hardening)

Verification target: local hardened instance `http://127.0.0.1:8211` on 2026-06-27.

## Effective Access Matrix (Observed)

`ALLOW`/`DENY` is derived from actual endpoint status codes plus `/api/admin/session` capability flags.

| Role | Dashboard | Overview | Employees | Income | Cash | Transactions | Reports | Payroll | Admin | Add User | Edit User | Delete User | Reset Password | Create Transaction | Edit Transaction | Delete Transaction | View Transaction |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Owner | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| SuperAdmin | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| Admin | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | DENY | DENY | DENY | DENY | ALLOW | ALLOW | ALLOW | ALLOW |
| HR | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | DENY | DENY | DENY | DENY | DENY | ALLOW | ALLOW | ALLOW | ALLOW |
| Accountant | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | DENY | DENY | DENY | DENY | DENY | ALLOW | ALLOW | ALLOW | ALLOW |
| OfficeAssistant01 | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | DENY | DENY | DENY | DENY | DENY | DENY | DENY | DENY | ALLOW |
| OfficeAssistant02 | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | DENY | DENY | DENY | DENY | DENY | DENY | DENY | DENY | ALLOW |
| Viewer | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | DENY | DENY | DENY | DENY | DENY | DENY | DENY | DENY | ALLOW |

## Policy Conformance (Expected vs Observed)

`PASS` means observed behavior matches the intended policy for that role.

| Role | Admin | Add User | Edit User | Delete User | Reset Password | Create Transaction | Edit Transaction | Delete Transaction |
|---|---|---|---|---|---|---|---|---|
| Owner | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| SuperAdmin | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Admin | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| HR | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Accountant | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| OfficeAssistant01 | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| OfficeAssistant02 | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Viewer | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |

## Raw Verification Details
```json
{
  "Owner": {
    "session_flags": {
      "can_manage_admin": true,
      "can_edit_delete_transactions": true,
      "permissions": {
        "can_view_admin_users": true,
        "can_add_user": true,
        "can_change_role": true,
        "can_deactivate_user": true,
        "can_reset_login_key": true,
        "can_view_admin_audit": true,
        "can_view_accounting_audit": true
      }
    },
    "codes": {
      "add": 400,
      "role": 400,
      "delete_user": 400,
      "reset": 400,
      "create": 200,
      "edit": 200,
      "delete": 200,
      "view": 200
    }
  },
  "OfficeAssistant01": {
    "session_flags": {
      "can_manage_admin": false,
      "can_edit_delete_transactions": false,
      "permissions": {
        "can_view_admin_users": false,
        "can_add_user": false,
        "can_change_role": false,
        "can_deactivate_user": false,
        "can_reset_login_key": false,
        "can_view_admin_audit": false,
        "can_view_accounting_audit": false
      }
    },
    "codes": {
      "add": 403,
      "role": 403,
      "delete_user": 403,
      "reset": 403,
      "create": 403,
      "edit": 403,
      "delete": 403,
      "view": 200
    }
  },
  "OfficeAssistant02": {
    "session_flags": {
      "can_manage_admin": false,
      "can_edit_delete_transactions": false,
      "permissions": {
        "can_view_admin_users": false,
        "can_add_user": false,
        "can_change_role": false,
        "can_deactivate_user": false,
        "can_reset_login_key": false,
        "can_view_admin_audit": false,
        "can_view_accounting_audit": false
      }
    },
    "codes": {
      "add": 403,
      "role": 403,
      "delete_user": 403,
      "reset": 403,
      "create": 403,
      "edit": 403,
      "delete": 403,
      "view": 200
    }
  },
  "SuperAdmin": {
    "session_flags": {
      "can_manage_admin": true,
      "can_edit_delete_transactions": true,
      "permissions": {
        "can_view_admin_users": true,
        "can_add_user": true,
        "can_change_role": true,
        "can_deactivate_user": true,
        "can_reset_login_key": true,
        "can_view_admin_audit": true,
        "can_view_accounting_audit": true
      }
    },
    "codes": {
      "add": 400,
      "role": 400,
      "delete_user": 400,
      "reset": 400,
      "create": 200,
      "edit": 409,
      "delete": 409,
      "view": 200
    }
  },
  "Admin": {
    "session_flags": {
      "can_manage_admin": true,
      "can_edit_delete_transactions": true,
      "permissions": {
        "can_view_admin_users": true,
        "can_add_user": false,
        "can_change_role": false,
        "can_deactivate_user": false,
        "can_reset_login_key": false,
        "can_view_admin_audit": true,
        "can_view_accounting_audit": true
      }
    },
    "codes": {
      "add": 403,
      "role": 403,
      "delete_user": 403,
      "reset": 403,
      "create": 200,
      "edit": 409,
      "delete": 409,
      "view": 200
    }
  },
  "HR": {
    "session_flags": {
      "can_manage_admin": false,
      "can_edit_delete_transactions": true,
      "permissions": {
        "can_view_admin_users": false,
        "can_add_user": false,
        "can_change_role": false,
        "can_deactivate_user": false,
        "can_reset_login_key": false,
        "can_view_admin_audit": false,
        "can_view_accounting_audit": false
      }
    },
    "codes": {
      "add": 403,
      "role": 403,
      "delete_user": 403,
      "reset": 403,
      "create": 200,
      "edit": 409,
      "delete": 409,
      "view": 200
    }
  },
  "Accountant": {
    "session_flags": {
      "can_manage_admin": false,
      "can_edit_delete_transactions": true,
      "permissions": {
        "can_view_admin_users": false,
        "can_add_user": false,
        "can_change_role": false,
        "can_deactivate_user": false,
        "can_reset_login_key": false,
        "can_view_admin_audit": false,
        "can_view_accounting_audit": false
      }
    },
    "codes": {
      "add": 403,
      "role": 403,
      "delete_user": 403,
      "reset": 403,
      "create": 200,
      "edit": 409,
      "delete": 409,
      "view": 200
    }
  },
  "Viewer": {
    "session_flags": {
      "can_manage_admin": false,
      "can_edit_delete_transactions": false,
      "permissions": {
        "can_view_admin_users": false,
        "can_add_user": false,
        "can_change_role": false,
        "can_deactivate_user": false,
        "can_reset_login_key": false,
        "can_view_admin_audit": false,
        "can_view_accounting_audit": false
      }
    },
    "codes": {
      "add": 403,
      "role": 403,
      "delete_user": 403,
      "reset": 403,
      "create": 403,
      "edit": 403,
      "delete": 403,
      "view": 200
    }
  }
}
```