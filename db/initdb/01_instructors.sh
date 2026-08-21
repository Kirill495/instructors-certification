#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     -c "DO \$\$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$INSTR_APP_USER') THEN CREATE USER $INSTR_APP_USER WITH PASSWORD '$INSTR_APP_PASSWORD'; END IF; END\$\$;"

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     -c "CREATE DATABASE \"$INSTR_DB_NAME\" OWNER \"$INSTR_APP_USER\""

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$INSTR_DB_NAME" \
     -v app_user="$INSTR_APP_USER" \
     -v db_name="$INSTR_DB_NAME" \
     -f /docker-entrypoint-initdb.d/sql/instructors/02_create_schema.sql