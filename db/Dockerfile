FROM postgres:17

COPY initdb/01_instructors.sh /docker-entrypoint-initdb.d/01_instructors.sh
COPY initdb/02_publication.sh /docker-entrypoint-initdb.d/02_publication.sh
COPY sql /docker-entrypoint-initdb.d/sql

RUN chmod +x /docker-entrypoint-initdb.d/01_instructors.sh
RUN chmod +x /docker-entrypoint-initdb.d/02_publication.sh