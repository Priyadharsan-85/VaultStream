#!/bin/sh
set -eu

: "${DB_MIGRATION_USERNAME:?DB_MIGRATION_USERNAME is required}"
: "${DB_USERNAME:?DB_USERNAME is required}"

if [ "$DB_MIGRATION_USERNAME" = "$DB_USERNAME" ] || \
   [ "$DB_MIGRATION_USERNAME" = "$POSTGRES_USER" ] || \
   [ "$DB_USERNAME" = "$POSTGRES_USER" ]; then
  echo "Bootstrap, migration, and runtime database usernames must all be different" >&2
  exit 1
fi

DB_MIGRATION_PASSWORD="$(cat /run/secrets/db_migration_password)"
DB_RUNTIME_PASSWORD="$(cat /run/secrets/db_password)"
export DB_MIGRATION_PASSWORD DB_RUNTIME_PASSWORD

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=ON_ERROR_STOP=1 \
  --set=migration_user="$DB_MIGRATION_USERNAME" \
  --set=runtime_user="$DB_USERNAME" <<'SQL'
\getenv migration_password DB_MIGRATION_PASSWORD
\getenv runtime_password DB_RUNTIME_PASSWORD

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'migration_user', :'migration_password') \gexec
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'runtime_user', :'runtime_password') \gexec
SELECT format('ALTER DATABASE %I OWNER TO %I', current_database(), :'migration_user') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'runtime_user') \gexec
GRANT USAGE ON SCHEMA public TO :"runtime_user";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migration_user" IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"runtime_user";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migration_user" IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO :"runtime_user";

\unset migration_password
\unset runtime_password
SQL

unset DB_MIGRATION_PASSWORD DB_RUNTIME_PASSWORD
