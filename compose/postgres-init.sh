#!/bin/sh
# Creates Kiln's three roles and its database on the first start of the Postgres volume:
# kiln_migrator owns the schema and runs migrations, kiln_app is the runtime role (rows only,
# never row-level security bypass), kiln_ops reads for operations.
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    -v migrator_password="$KILN_MIGRATOR_PASSWORD" \
    -v app_password="$KILN_APP_PASSWORD" \
    -v ops_password="$KILN_OPS_PASSWORD" <<'SQL'
CREATE ROLE kiln_migrator LOGIN PASSWORD :'migrator_password';
CREATE ROLE kiln_app LOGIN PASSWORD :'app_password' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
CREATE ROLE kiln_ops LOGIN PASSWORD :'ops_password' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
CREATE DATABASE kiln OWNER kiln_migrator;
SQL
