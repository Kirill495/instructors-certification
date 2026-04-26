FROM postgres:17

COPY init.sh /docker-entrypoint-initdb.d/init.sh
COPY sql /docker-entrypoint-initdb.d/sql

RUN chmod +x /docker-entrypoint-initdb.d/init.sh