FROM postgis/postgis:16-3.4

RUN apt-get update && apt-get install -y \
    git \
    make \
    build-essential \
    postgresql-server-dev-16 \
    clang-13 \
    llvm-13 \
    && rm -rf /var/lib/apt/lists/*

RUN cd /tmp && \
    git clone --branch v0.7.0 https://github.com/pgvector/pgvector.git && \
    cd pgvector && \
    make && \
    make install

COPY init.sql /docker-entrypoint-initdb.d/20_init.sql