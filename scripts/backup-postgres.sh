#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"

backup_dir="${1:-./backups}"
mkdir -p -- "$backup_dir"
chmod 700 "$backup_dir"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_file="$backup_dir/vaultstream-$timestamp.dump"
partial_file="$backup_file.partial"
trap 'rm -f -- "$partial_file"' EXIT

docker compose --env-file .env -f docker-compose.prod.yml exec -T vaultstream-db \
  sh -ec 'pg_dump --format=custom --username="$POSTGRES_USER" "$POSTGRES_DB"' \
  > "$partial_file"

mv -- "$partial_file" "$backup_file"
trap - EXIT
printf 'Created PostgreSQL backup: %s\n' "$backup_file"
