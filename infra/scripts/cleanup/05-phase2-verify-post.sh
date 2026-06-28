#!/usr/bin/env bash
# ==============================================================================
# 05-phase2-verify-post.sh — Post-cleanup health verification
# Runs the same checks as 00-verify-state.sh but writes to cleanup-post-state.json
# and diffs key metrics against the pre-state snapshot.
# ==============================================================================
set -euo pipefail

# Re-use the verify script with the "post" label
bash /home/azim/scripts/cleanup/00-verify-state.sh post

# ── Diff pre vs post key metrics ───────────────────────────────────────────────
echo ""
echo "================================================================"
echo "  PRE vs POST COMPARISON"
echo "================================================================"

PRE="/home/azim/cleanup-pre-state.json"
POST="/home/azim/cleanup-post-state.json"

if [[ ! -f "$PRE" || ! -f "$POST" ]]; then
    echo "  Cannot compare: one or both state files missing."
    exit 0
fi

python3 - << 'PYEOF'
import json, sys

with open("/home/azim/cleanup-pre-state.json") as f:
    pre = json.load(f)
with open("/home/azim/cleanup-post-state.json") as f:
    post = json.load(f)

print(f"  Generated pre:  {pre['generated_at']}")
print(f"  Generated post: {post['generated_at']}")
print()

# Disk comparison
pre_disk = pre.get("disk", {})
post_disk = post.get("disk", {})
pre_used  = pre_disk.get("used_gb",  0)
post_used = post_disk.get("used_gb", 0)
recovered = pre_used - post_used

print(f"  DISK:")
print(f"    Before: {pre_used} GB used  ({pre_disk.get('used_pct',0)}%)")
print(f"    After:  {post_used} GB used  ({post_disk.get('used_pct',0)}%)")
print(f"    Recovered: {recovered} GB")
print()

# Git pack sizes
pre_tp1  = pre_disk.get("tmp_pack_v3r9Fa_bytes", 0)
post_tp1 = post_disk.get("tmp_pack_v3r9Fa_bytes", 0)
print(f"  GIT tmp_pack_v3r9Fa: {pre_tp1:,} bytes → {post_tp1:,} bytes")

pre_tp2  = pre_disk.get("tmp_pack_DKU8rF_bytes", 0)
post_tp2 = post_disk.get("tmp_pack_DKU8rF_bytes", 0)
print(f"  GIT tmp_pack_DKU8rF: {pre_tp2:,} bytes → {post_tp2:,} bytes")

pre_b3   = pre_disk.get("bridge3_log_bytes", 0)
post_b3  = post_disk.get("bridge3_log_bytes", 0)
print(f"  bridge3.log:         {pre_b3:,} bytes → {post_b3:,} bytes")
print()

# Service health comparison
def check_services(state, label):
    svcs = state.get("systemd_services", {})
    issues = [k for k, v in svcs.items() if v != "active"]
    if issues:
        print(f"  SERVICES {label}: ISSUES with {issues}")
    else:
        print(f"  SERVICES {label}: all active")

check_services(pre,  "pre:")
check_services(post, "post:")
print()

# HTTP status comparison
def check_http(state, label):
    local = state.get("local_http", {})
    https = state.get("https_endpoints", {})
    all_endpoints = {**local, **https}
    issues = {k: v for k, v in all_endpoints.items() if v not in ("200", "301", "302")}
    if issues:
        print(f"  HTTP {label}: ISSUES → {issues}")
    else:
        print(f"  HTTP {label}: all OK")

check_http(pre,  "pre:")
check_http(post, "post:")
print()

# Docker health
def check_docker(state, label):
    containers = state.get("docker_containers", {})
    issues = {k: v for k, v in containers.items() if v.get("health") not in ("healthy", "no-healthcheck") and k != "fazle-brain"}
    if issues:
        print(f"  DOCKER {label}: ISSUES → {issues}")
    else:
        print(f"  DOCKER {label}: all healthy (excluding fazle-brain which was already exited)")

check_docker(pre,  "pre:")
check_docker(post, "post:")
print()

# Overall verdict
overall_pre  = pre.get("overall_healthy", False)
overall_post = post.get("overall_healthy", False)
print(f"  OVERALL: pre={overall_pre}  post={overall_post}")

if overall_post:
    print()
    print("  ✓ All checks passed. Phase 2 cleanup verified successful.")
else:
    print()
    print("  ⚠  Some post-cleanup checks failed. Review the comparison above.")
    sys.exit(1)
PYEOF
