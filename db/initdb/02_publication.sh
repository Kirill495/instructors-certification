#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     -c "DO \$\$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$PUB_APP_USER') THEN CREATE USER $PUB_APP_USER WITH PASSWORD '$PUB_APP_PASSWORD'; END IF; END\$\$;"

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     -c "CREATE DATABASE \"$PUB_DB_NAME\" OWNER \"$PUB_APP_USER\""

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$PUB_DB_NAME" \
     -v app_user="$PUB_APP_USER" \
     -v db_name="$PUB_DB_NAME" \
     -f /docker-entrypoint-initdb.d/sql/publication/01_create_schema.sql
