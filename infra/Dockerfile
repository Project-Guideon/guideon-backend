# 1. 베이스 이미지 (PostGIS 포함 버전)
FROM postgis/postgis:16-3.4

# 2. 필수 빌드 도구 설치 (여기에 clang-13, llvm-13 추가함!)
RUN apt-get update && apt-get install -y \
    git \
    make \
    build-essential \
    postgresql-server-dev-16 \
    clang-13 \
    llvm-13 \
    && rm -rf /var/lib/apt/lists/*

# 3. pgvector 소스 다운로드 및 빌드/설치
RUN cd /tmp && \
    git clone --branch v0.7.0 https://github.com/pgvector/pgvector.git && \
    cd pgvector && \
    make && \
    make install