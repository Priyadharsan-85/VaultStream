#!/usr/bin/env bash
set -euo pipefail
umask 077

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"

backup_dir="${1:?Usage: VAULTSTREAM_BACKUP_AGE_RECIPIENT=age1... $0 /path/to/backup-storage}"
age_recipient="${VAULTSTREAM_BACKUP_AGE_RECIPIENT:?Set VAULTSTREAM_BACKUP_AGE_RECIPIENT to the age public recipient}"

if ! command -v age >/dev/null 2>&1; then
  echo "age is required to encrypt production backups" >&2
  exit 1
fi

mkdir -p -- "$backup_dir"
chmod 700 "$backup_dir"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_file="$backup_dir/vaultstream-$timestamp.dump.age"
partial_file="$backup_file.partial"
trap 'rm -f -- "$partial_file"' EXIT

docker compose --env-file .env -f docker-compose.prod.yml exec -T vaultstream-db \
  sh -ec 'pg_dump --format=custom --username="$POSTGRES_USER" "$POSTGRES_DB"' \
  | age --encrypt --recipient "$age_recipient" > "$partial_file"

mv -- "$partial_file" "$backup_file"
trap - EXIT
printf 'Created encrypted PostgreSQL backup: %s\n' "$backup_file"
