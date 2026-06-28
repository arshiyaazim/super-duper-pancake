#!/usr/bin/env bash
# Daily backup: bridge SQLite + PostgreSQL dump
# Runs via cron: 30 3 * * *
set -euo pipefail

DEST="${BACKUP_DEST:-/home/azim/secure-env-backup/auto}"
DAILY_DIR="${DEST}/daily"
WEEKLY_DIR="${DEST}/weekly"
TS="$(date +%Y%m%d_%H%M%S)"
LOG="${DEST}/backup.log"
LOCK_FILE="${DEST}/.backup.lock"
KEEP_DAILY="${BACKUP_KEEP_DAILY:-14}"
KEEP_WEEKLY="${BACKUP_KEEP_WEEKLY:-8}"

mkdir -p "$DAILY_DIR" "$WEEKLY_DIR"

if [[ -e "$LOCK_FILE" ]]; then
	echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] backup skipped (lock exists): $LOCK_FILE" >> "$LOG"
	exit 0
fi

trap 'rm -f "$LOCK_FILE"' EXIT
touch "$LOCK_FILE"

run_backup_into() {
	local target_root="$1"
	local dir="${target_root}/${TS}"
	mkdir -p "$dir"

	# Bridge SQLite stores
	tar -czf "${dir}/bridge1_sqlite.tgz" -C /home/azim/whatsapp1/store . 2>/dev/null
	tar -czf "${dir}/bridge2_sqlite.tgz" -C /home/azim/whatsapp2/store . 2>/dev/null

	# PostgreSQL dump (postgres DB — waerp dropped 2026-06-09)
	docker exec ai-postgres pg_dump -U postgres -Fc postgres > "${dir}/pg_postgres.dump"

	printf "%s" "$dir"
}

rotate_keep() {
	local base="$1"
	local keep="$2"
	mapfile -t dirs < <(find "$base" -maxdepth 1 -mindepth 1 -type d | sort)
	local count="${#dirs[@]}"
	if (( count <= keep )); then
		return 0
	fi
	local prune_count=$((count - keep))
	for ((i=0; i<prune_count; i++)); do
		rm -rf -- "${dirs[$i]}"
	done
}

daily_path="$(run_backup_into "$DAILY_DIR")"

# Every Sunday (ISO day 7), also retain a weekly checkpoint.
if [[ "$(date +%u)" == "7" ]]; then
	weekly_path="$(run_backup_into "$WEEKLY_DIR")"
else
	weekly_path="-"
fi

rotate_keep "$DAILY_DIR" "$KEEP_DAILY"
rotate_keep "$WEEKLY_DIR" "$KEEP_WEEKLY"

echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] backup ok daily=${daily_path} weekly=${weekly_path} keep_daily=${KEEP_DAILY} keep_weekly=${KEEP_WEEKLY}" >> "$LOG"
