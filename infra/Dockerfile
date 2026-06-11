FROM postgis/postgis:16-3.4

# pgvector: 빌드 도구 없이 PGDG apt 패키지로 설치 (이미지 크기 ~2GB 절감)
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-16-pgvector \
    && rm -rf /var/lib/apt/lists/*

COPY init.sql /docker-entrypoint-initdb.d/20_init.sql