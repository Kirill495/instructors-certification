#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     -c "DO \$\$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$DB_APP_USER') THEN CREATE USER $DB_APP_USER WITH PASSWORD '$DB_APP_PASSWORD'; END IF; END\$\$;"

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     -v app_user="$DB_APP_USER" \
     -v db_name="$POSTGRES_DB" \
     -f /docker-entrypoint-initdb.d/sql/02_create_schema.sql