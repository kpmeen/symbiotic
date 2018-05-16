FROM postgres:10.3
RUN mkdir -p /docker-entrypoint-initdb.d/
COPY ./*.sql /docker-entrypoint-initdb.d/
