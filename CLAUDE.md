# GUIDEON(가이온) 개발 마스터 문서 (Claude용 .MD)
> 이 문서는 GUIDEON 프로젝트의 “단일 진실(SSOT)”입니다.  
> Claude는 아래 내용을 **규칙/제약으로 우선 적용**하고, 문서에 없는 부분은 “추정”이 아니라 “선택지/가정”으로 명시합니다.

---

## 0. 프로젝트 한 줄 요약
“관리자가 자료만 넣으면 끝! 어느 관광지에서든 바로 쓸 수 있는 3D 캐릭터 AI 가이드 키오스크”

---

## 1. 목표/핵심 가치
### 1.1 관광객 UX
- 버튼식 언어 선택 없이, **말한 언어 그대로** 응답
- 친근한 **3D 캐릭터(표정/제스처/립싱크)** + 지도/경로 자동 표시
- **멀티턴 대화**로 “거기” 같은 지시어 문맥 유지

### 1.2 운영(관리자) UX
- 개발자 호출 없이 운영 변경 가능
    - POI(장소) 등록: 엑셀처럼 입력 / 지도 클릭으로 좌표 등록
    - PDF 업로드: 파일만 올리면 RAG 학습
- 키오스크가 여러 대여도 **관리자 웹에서 기기 위치를 관리** → “가장 가까운 화장실”이 기기 위치 기준으로 정확히 달라짐

### 1.3 기술 목표(성능)
- 사용자 질문 → 응답(텍스트+TTS+지도 액션)까지 **10초 이내**를 목표
- 트래픽/CPU-heavy 작업은 분리:
    - Spring Boot: 인증/정책/조립/공간 질의(또는 DB에서)
    - FastAPI: STT/TTS/RAG/LLM/LangGraph

---

## 2. 시스템 아키텍처
### 2.1 전체 구성
- **Client (Unity 키오스크)** ↔ **Main Server (Spring Boot)** ↔ **AI Server (FastAPI)**
- **Admin Web (React)** ↔ **Spring Boot**

### 2.2 역할 분담(원칙)
- Unity는 **FastAPI를 직접 호출하지 않는다.**
- Spring이 **정책/지도URL 생성/보안/tts 합성(또는 audio_url 토큰화)**까지 해서 Unity가 “그대로 소비 가능한 최종 패킷”을 만든다.
- FastAPI는 내부 전용이며 외부 노출 금지.

---

## 3. 스택/버전(기준)
### 3.1 Client (Unity)
- Unity 2022.3.x LTS / C# 9.0
- UniVRM v0.115.0 (VRM 로드)
- uLipSync v3.0.0 (립싱크)
- UniTask v2.5.0 (비동기)
- Native WebSocket (실시간)
- WebView: UnityWebBrowser(무료) 또는 UniWebView(유료) 검토
    - 지도는 URL 기반 WebView 로드

### 3.2 Admin Web
- React 18.x / Vite / JavaScript
- (후보) MUI v5
- Axios
- Context API (Auth 정도)
- Kakao Maps API + react-kakao-maps-sdk

### 3.3 Main Server
- Spring Boot 3.2.x / Java 17 / Gradle
- Spring WebSocket(STOMP)

### 3.4 AI Server
- FastAPI 0.109.x / Python 3.11
- LangChain / LangGraph
- Vector DB는 Postgres(pgvector) 기반을 우선(현재 DB 스키마 기준)

### 3.5 DevOps
- Docker / Docker Compose
- 단일 인스턴스에서 컨테이너로 올리는 구조(초기)

---

## 4. 핵심 로직(필수 규칙)
### 4.1 멀티턴(문맥 유지)
- 모든 대화 요청은 `session_id`를 포함
- FastAPI(LangGraph)가 `chat_history`를 `max_history_turns` 만큼 유지
- 비용 최소화를 위해 최근 N턴만 유지(기본 10)

### 4.2 다국어(언어 자동 감지 + 게이트)
- STT(Whisper)에서 `detected_lang` 산출
- **지원 언어 게이트**(site 정책)에 통과해야 실제 응답 언어가 된다
    - `reply_lang = detected_lang` if supported
    - 아니면 `reply_lang = fallback_lang`
    - 지원 안 하는 언어인 경우 안내 문구를 1회 포함(정책)

### 4.3 키오스크 위치 동기화
- Unity `config.json`에 `device_id` 저장
- 부팅 시 Spring `bootstrap`으로 기기 좌표/zone/policy/마스코트 설정을 받아 메모리에 상주
- 이후 모든 질의에 기기 위치가 포함되어야 “가장 가까운 장소”가 정확해짐

### 4.4 공간 기반 검색(우선순위)
- 기본은 “같은 zone 우선” + 그 다음 거리 기반
- OUTER는 `zone_id = NULL`로 표현(폴리곤 밖)
- DB 타입 원칙:
    - Zone: `geometry(Polygon, 4326)`
    - Place/Device: `geography(Point, 4326)` (미터 단위 거리 정확)

---

## 5. 데이터 파이프라인
### 5.1 관리자 데이터 주입
#### A-1 장소(POI)/기기 등록
- Admin Web에서 지도 클릭 → lat/lng 추출 → Spring 저장
- Spring은 zone 자동 계산(ST_Contains) 후 `zone_id` 캐싱 저장(AUTO)

#### A-2 문서(PDF) 학습
- React → Spring: Multipart 업로드
- Spring은 파일을 직접 파싱하지 않고 FastAPI로 전달(부하 분리)
- FastAPI: Chunking → Embedding → `tb_doc_chunk` 저장
- 처리 완료/실패는 콜백/상태 변경로 관리(`tb_document.status`)

### 5.2 사용자 실시간 상호작용
- Unity(WebSocket)로 오디오/텍스트 전송
- Spring이 인증/정책 적용 후 FastAPI 호출(STT/Chat/TTS)
- 최종 응답은 Spring이 “Unity 고정 포맷”으로 조립하여 반환/Publish

---

## 6. API 공통 규약(임시)
### 6.1 Base URL
- Spring REST(외부): `https://{spring-host}/api/v1`
- Spring WebSocket(외부): `wss://{spring-host}/ws/v1`
- FastAPI Internal(내부): `http://{fastapi-host}/internal/v1`
- Map WebView: `https://{spring-host}/kiosk-map?t={token}`

### 6.2 인증/권한
- Admin Web → Spring: `Authorization: Bearer {admin_jwt}`
    - 역할: `PLATFORM_ADMIN`, `SITE_ADMIN`
    - SITE_ADMIN은 본인 스코프(site_ids) 밖 접근 시 403
- Unity(Device) → Spring:
    - `X-Device-Id: {device_id}`
    - `X-Device-Token: {plain_token}` (서버엔 hash만 저장)
- Spring ↔ FastAPI(Internal):
    - 내부망 + 리버스프록시 제한 + (권장) `X-Internal-Key`

### 6.3 Response Envelope(고정)
성공:
```json
{
  "success": true,
  "data": {},
  "error": null,
  "trace_id": "uuid"
}
6.4 공통 에러 코드(요약)

400 VALIDATION_ERROR

401 AUTH_REQUIRED / AUTH_INVALID

403 ADMIN_SITE_FORBIDDEN / SITE_INACTIVE

404 NOT_FOUND

409 CONFLICT

422 DOMAIN_RULE_VIOLATION

429 RATE_LIMITED

500 INTERNAL_ERROR

503 UPSTREAM_TIMEOUT

7. WebSocket (반 실시간 STT/TTS)
7.1 연결

URL: wss://{spring-host}/ws/v1

STOMP over WebSocket

CONNECT Headers: X-Device-Id, X-Device-Token

7.2 토픽

Publish: /pub/chat/audio, /pub/chat/text

Subscribe:

/sub/chat/{session_id}

/sub/chat/{session_id}/status(선택)

7.3 오디오 프로토콜(START/CHUNK/END)

AUDIO_START: format/sample_rate/channels/codec 포함

AUDIO_CHUNK: base64 오디오, seq

AUDIO_END: final_seq

서버는 END 수신 후 STT → 의도분류 → POI/문서검색 → 필요시 지도 URL 생성 → TTS → CHAT_RESPONSE publish

7.4 서버 응답 CHAT_RESPONSE(요약)

reply_lang, reply_text, emotion, action(SHOW_MAP), audio, references, trace_id

8. Internal API (Spring ↔ FastAPI)
8.1 STT

POST /internal/v1/stt/transcribe

권장: base64 대신 multipart 또는 presigned URL(운영 시)

8.2 Chat Execute

POST /internal/v1/chat/execute

입력: site_id/session_id/device 위치 + text/detected_lang

출력: intent/reply_text/reply_lang/emotion/action/references

8.3 TTS

POST /internal/v1/tts/synthesize

출력: audio_base64 or (권장) audio_url 토큰 방식으로 확장

9. “Unity 최종 응답 조립” 규칙(가장 중요 / 고정)
9.1 Device Chat (REST) 최종 패킷

POST /device/chat

Spring은 아래 순서로 조립:

Device 인증/사이트 활성 검사

FastAPI chat/execute 호출

action이 SHOW_MAP이면 Spring이 maps/route로 map_url 생성(토큰화)

want_tts=true이면 TTS 생성(권장: audio_url 토큰)

Unity가 그대로 소비 가능한 고정 JSON 반환

Unity는 이 응답을 그대로 파싱해 애니메이션/립싱크/웹뷰 로드를 수행한다.

9.2 Device 세션 종료

DELETE /device/sessions/{session_id}

idle timeout 시 대화 이력/락/임시 리소스 정리

10. 운영/감사로그(명세로 고정)
10.1 감사로그 조회

GET /sites/{site_id}/audit-logs

10.2 반드시 기록할 이벤트

Admin CRUD: site/zone/place/device/document/daily_info/place_detail/mascot

System 작업: zone recalc 실행 결과, document process 완료/실패

Device 보안 이벤트: 기기 인증 실패, rate limit

changes_json 권장 포맷:

{
  "before": { "is_active": true, "zone_id": 3 },
  "after":  { "is_active": false, "zone_id": null },
  "meta":   { "reason": "ADMIN_TOGGLE", "job_id": "job_..." }
}

11. DB 스키마(요약 / 최종 기준)
11.1 엔진/확장

PostgreSQL 15+

PostGIS, pgvector, pg_trgm

11.2 핵심 원칙

모든 쿼리는 site_id 필터가 필수

zone: geometry / place&device: geography

zone_id 캐싱 + zone_source(AUTO/MANUAL)

updated_at은 트리거로 자동 갱신(서비스에서 직접 수정 금지)

11.3 테이블 개요(필수)

tb_site: 테넌트 root, kill switch is_active

tb_admin, tb_admin_site, tb_admin_invite: 관리자/스코프/초대

tb_zone: INNER/SUB 폴리곤, level은 zone_type으로 generated

tb_place: POI(geography), zone_id 캐싱

tb_device: 기기(geography), auth_token_hash, zone_id 캐싱

tb_document: 문서 메타(status/해시/청킹 파라미터)

tb_doc_chunk: RAG 검색 엔진(embedding/tsv/trgm)

tb_place_detail: 운영 정보(공지/시간/가격 등), 활성 토글

tb_daily_info: 날짜 기반(행사/식단/임시운영), (place_id, site_id) 복합 FK

tb_audit_log: 폴리모픽 감사 로그

12. Zone/Place 재계산 정책(핵심)
12.1 재계산 원칙

zone 수정 시 zone_source='AUTO'만 재계산

MANUAL은 관리자 의도 유지

후보 선별은 “변경 전/후 폴리곤 차이 + buffer” 권장

12.2 재할당 규칙(고정)

후보 row에 대해:

SUB 포함이면 SUB

아니면 INNER 포함이면 INNER

아니면 OUTER(zone_id=null)

13. 문서 검색(RAG) 정책(권장)
13.1 검색 전략(하이브리드)

keyword(tsv/trgm)로 후보 축소 → vector로 재정렬(topK)

필수: site_id 필터

13.2 다국어 질의와 한국어 문서

사용자 입력 언어가 한국어가 아니고 RAG가 필요하면:

(권장) 질의만 한국어로 번역 → 검색 → 답변은 reply_lang으로 생성

“번역 정책/도구”는 코드에서 고정하고, Claude 출력은 항상 정책 선택을 명시

14. Claude 출력 규칙(이 문서를 사용하는 방식)

Claude는 아래 규칙을 지킨다:

API/DB/프로토콜을 작성할 때, 이 문서의 필드명/규약을 우선한다.

확정되지 않은 항목은 “선택지 + 장단점 + 기본값 추천” 형태로 쓴다.

Unity 최종 패킷은 반드시 “Spring 조립” 기준으로 작성한다.

site_id 격리, 복합 FK, zone 캐싱, updated_at 트리거 원칙을 깨지 않는다.

성능/운영 이슈(base64 오디오, 대용량 PDF 처리 등)는 “운영 권장안”을 함께 제시한다.

15. 당장 구현 시 체크리스트(요약)

 Device bootstrap 구현(기기/사이트/마스코트/정책 동시 내려주기)

 Device 인증: plain token → SHA-256 → DB hash 비교

 WebSocket STOMP: AUDIO_START/CHUNK/END 수신 파이프라인

 FastAPI chat/execute 연동 + Spring 최종 패킷 조립

 지도 WebView: token 기반 URL 생성 + 만료 처리

 문서 업로드: tb_document PENDING→PROCESSING→COMPLETED/FAILED + 콜백

 감사로그: Admin CRUD + System 작업 + Device 보안 이벤트

 Zone 재계산: AUTO만, affected-only 후보 정책

 다국어: supported_langs gate + fallback 안내 1회

16. 추가 제안(유용한 개선 아이디어)
16.1 “유료/출입 제한 구역” 문제(권장 확장)

현재 우려: 무료 구역 사용자에게 유료 구역 화장실을 추천할 수 있음.
최소 변경으로 해결하는 방식(추천 순서):

tb_place에 access_level(예: FREE/PAID/STAFF_ONLY) 컬럼 추가

tb_device 또는 요청 옵션에 visitor_access_level 포함(기본 FREE)

POI 검색 시 access_level <= visitor_access_level 필터 적용

필터로 후보가 0이면 “가장 가까운 곳은 유료 구역입니다” 안내 + 대체 후보 제시

zone에도 access_level을 둘 수 있지만, 운영 난이도가 올라가므로 place 단부터 시작을 추천합니다.

16.2 오디오 전송 최적화(운영 권장)

base64는 33% 용량 증가 + JSON 파싱 비용

운영 권장안:

Unity→Spring: WS 스트리밍 유지

Spring→FastAPI: multipart로 wav 전달 또는 내부 gRPC/WS 스트리밍

TTS는 base64 대신 audio_url 토큰 방식(캐시/만료/재생 안정)

16.3 관측성(운영 필수)

trace_id를 Spring↔FastAPI↔Unity 로그에 모두 남김

latency_ms를 단계별로 수집:

STT / intent-router / POI search / RAG / TTS / map_url

16.4 안전장치

site kill switch(is_active=false)면 Unity/관리자 모두 차단 정책 고정

rate limit(429) + device 인증 실패는 감사로그에 반드시 기록

/* =========================================================
   GUIDEON Schema v9.7 (Final)
   PostgreSQL 15+ / PostGIS / pgvector / pg_trgm

   OUTER = zone_id NULL (no boundary polygon)

   CHANGE (v9.7 patch):
   - tb_zone.level is NOT provided by client.
   - level is derived from zone_type (INNER=1, SUB=2) as a GENERATED column.
   ========================================================= */

BEGIN;

-- 0) Extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 1) updated_at auto trigger function
CREATE OR REPLACE FUNCTION guideon_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2) ENUM types
DO $$ BEGIN
  CREATE TYPE zone_source_enum AS ENUM ('AUTO', 'MANUAL');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE info_type_enum AS ENUM ('NOTICE', 'HOURS', 'MENU_SIMPLE', 'PRICE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE doc_status_enum AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE actor_type_enum AS ENUM ('ADMIN', 'SYSTEM', 'DEVICE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE action_type_enum AS ENUM ('CREATE', 'UPDATE', 'DELETE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- (추가) 관리자 역할: 플랫폼/관광지 운영자
DO $$ BEGIN
  CREATE TYPE admin_role_enum AS ENUM ('PLATFORM_ADMIN', 'SITE_ADMIN');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- (추가) 초대 상태: PENDING(미사용) / USED(사용됨) / EXPIRED(만료) / REVOKED(회수)
DO $$ BEGIN
  CREATE TYPE admin_invite_status_enum AS ENUM ('PENDING', 'USED', 'EXPIRED', 'REVOKED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- =========================================================
-- 3) Tables
-- =========================================================

-- 3.1 tb_site
CREATE TABLE IF NOT EXISTS tb_site (
  site_id     BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  is_active   BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE tb_site IS '관광지/테넌트 Root. 모든 데이터는 site 단위로 격리.';
COMMENT ON COLUMN tb_site.site_id IS '모든 하위 테이블의 Root Key';
COMMENT ON COLUMN tb_site.name IS '관광지명';
COMMENT ON COLUMN tb_site.is_active IS 'Kill switch. FALSE면 해당 관광지 기능 차단';
COMMENT ON COLUMN tb_site.created_at IS '생성 시각';
COMMENT ON COLUMN tb_site.updated_at IS '수정 시각(트리거 자동 갱신)';

CREATE TRIGGER trg_site_updated_at
BEFORE UPDATE ON tb_site
FOR EACH ROW EXECUTE FUNCTION guideon_set_updated_at();

-- =========================================================
-- 3.1.1 tb_admin
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_admin (
  admin_id       BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  email          VARCHAR(255) NOT NULL,
  password_hash  VARCHAR(255) NOT NULL, -- bcrypt/argon2 등 해시 결과 저장(평문 금지)
  role           admin_role_enum NOT NULL DEFAULT 'SITE_ADMIN',
  is_active      BOOLEAN NOT NULL DEFAULT TRUE,
  last_login_at  TIMESTAMPTZ NULL,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uk_admin_email UNIQUE (email)
);

COMMENT ON TABLE tb_admin IS '관리자 계정(PLATFORM_ADMIN / SITE_ADMIN).';
COMMENT ON COLUMN tb_admin.admin_id IS '관리자 PK';
COMMENT ON COLUMN tb_admin.email IS '로그인 이메일(유니크). 애플리케이션에서 소문자 정규화 권장';
COMMENT ON COLUMN tb_admin.password_hash IS '비밀번호 해시';
COMMENT ON COLUMN tb_admin.role IS 'PLATFORM_ADMIN(전체) / SITE_ADMIN(특정 관광지)';
COMMENT ON COLUMN tb_admin.is_active IS '비활성 시 로그인/기능 차단';
COMMENT ON COLUMN tb_admin.last_login_at IS '마지막 로그인 시각';
COMMENT ON COLUMN tb_admin.created_at IS '생성 시각';
COMMENT ON COLUMN tb_admin.updated_at IS '수정 시각(트리거 자동 갱신)';

CREATE INDEX IF NOT EXISTS idx_admin_role_active ON tb_admin (role, is_active);

CREATE TRIGGER trg_admin_updated_at
BEFORE UPDATE ON tb_admin
FOR EACH ROW EXECUTE FUNCTION guideon_set_updated_at();

-- =========================================================
-- 3.1.2 tb_admin_site : SITE_ADMIN 접근 스코프
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_admin_site (
  admin_id   BIGINT NOT NULL REFERENCES tb_admin(admin_id) ON DELETE CASCADE,
  site_id    BIGINT NOT NULL REFERENCES tb_site(site_id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (admin_id, site_id)
);

COMMENT ON TABLE tb_admin_site IS 'SITE_ADMIN이 접근 가능한 관광지(site) 스코프 매핑.';
COMMENT ON COLUMN tb_admin_site.admin_id IS '관리자 PK';
COMMENT ON COLUMN tb_admin_site.site_id IS '관광지 PK';
COMMENT ON COLUMN tb_admin_site.created_at IS '매핑 생성 시각';

CREATE INDEX IF NOT EXISTS idx_admin_site_site_id ON tb_admin_site (site_id);

-- =========================================================
-- 3.1.3 tb_admin_invite : 초대 기반 계정 발급
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_admin_invite (
  invite_id           BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  site_id             BIGINT NOT NULL REFERENCES tb_site(site_id) ON DELETE CASCADE,

  email               VARCHAR(255) NOT NULL,
  role                admin_role_enum NOT NULL DEFAULT 'SITE_ADMIN',

  token_hash          CHAR(64) NOT NULL, -- SHA-256 HEX (평문 저장 금지)
  status              admin_invite_status_enum NOT NULL DEFAULT 'PENDING',
  expires_at          TIMESTAMPTZ NOT NULL,
  used_at             TIMESTAMPTZ NULL,

  created_by_admin_id BIGINT NULL REFERENCES tb_admin(admin_id) ON DELETE SET NULL,

  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uk_invite_token_hash UNIQUE (token_hash)
);

COMMENT ON TABLE tb_admin_invite IS '관광지 운영자 초대(회원가입 대체). token은 hash만 저장.';
COMMENT ON COLUMN tb_admin_invite.invite_id IS '초대 PK';
COMMENT ON COLUMN tb_admin_invite.site_id IS '초대 대상 관광지';
COMMENT ON COLUMN tb_admin_invite.email IS '초대 이메일';
COMMENT ON COLUMN tb_admin_invite.role IS '기본 SITE_ADMIN';
COMMENT ON COLUMN tb_admin_invite.token_hash IS '초대 토큰 SHA-256(HEX)';
COMMENT ON COLUMN tb_admin_invite.status IS 'PENDING/USED/EXPIRED/REVOKED';
COMMENT ON COLUMN tb_admin_invite.expires_at IS '만료 시각';
COMMENT ON COLUMN tb_admin_invite.used_at IS '사용 시각(수락 완료)';
COMMENT ON COLUMN tb_admin_invite.created_by_admin_id IS '초대 발급한 관리자(보통 PLATFORM_ADMIN)';
COMMENT ON COLUMN tb_admin_invite.created_at IS '생성 시각';
COMMENT ON COLUMN tb_admin_invite.updated_at IS '수정 시각(트리거 자동 갱신)';

-- 같은 site+email로 "활성(PENDING)" 초대는 1개만 허용
CREATE UNIQUE INDEX IF NOT EXISTS uk_invite_site_email_pending
ON tb_admin_invite (site_id, email)
WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_invite_site_status ON tb_admin_invite (site_id, status);
CREATE INDEX IF NOT EXISTS idx_invite_expires_at ON tb_admin_invite (expires_at);

CREATE TRIGGER trg_admin_invite_updated_at
BEFORE UPDATE ON tb_admin_invite
FOR EACH ROW EXECUTE FUNCTION guideon_set_updated_at();

-- =========================================================
-- 3.2 tb_zone (INNER/SUB only. OUTER is NULL)
--   - level: GENERATED ALWAYS AS (zone_type 기반)
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_zone (
  zone_id        BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  site_id        BIGINT NOT NULL REFERENCES tb_site(site_id) ON DELETE CASCADE,
  name           VARCHAR(50) NOT NULL,
  code           VARCHAR(50) NOT NULL,
  zone_type      VARCHAR(10) NOT NULL, -- 'INNER' or 'SUB'

  -- IMPORTANT: 클라이언트/서버가 입력하지 않음. zone_type으로 자동 결정(INNER=1, SUB=2)
  level          SMALLINT GENERATED ALWAYS AS (
    CASE zone_type
      WHEN 'INNER' THEN 1
      WHEN 'SUB'   THEN 2
    END
  ) STORED,

  parent_zone_id BIGINT NULL REFERENCES tb_zone(zone_id) ON DELETE SET NULL,
  area_geometry  geometry(Polygon, 4326) NOT NULL,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uk_zone_code UNIQUE (site_id, code),
  -- composite FK를 위해 (zone_id, site_id) 유니크 제공
  CONSTRAINT uk_zone_id_site UNIQUE (zone_id, site_id),
  CONSTRAINT ck_zone_type CHECK (zone_type IN ('INNER', 'SUB'))
);

COMMENT ON TABLE tb_zone IS '구역(Zone). INNER/SUB만 저장. OUTER는 zone_id NULL로 표현.';
COMMENT ON COLUMN tb_zone.zone_id IS '구역 ID';
COMMENT ON COLUMN tb_zone.site_id IS '소속 관광지';
COMMENT ON COLUMN tb_zone.name IS '관리자 UI 표시용';
COMMENT ON COLUMN tb_zone.code IS '시스템 식별 코드(site 내 유니크). 예: INNER, SUB_A';
COMMENT ON COLUMN tb_zone.zone_type IS 'INNER 또는 SUB. OUTER는 저장하지 않음';
COMMENT ON COLUMN tb_zone.level IS '파생 컬럼(Generated). zone_type으로 자동 결정: INNER=1, SUB=2 (클라이언트 입력 금지)';
COMMENT ON COLUMN tb_zone.parent_zone_id IS 'SUB의 부모 INNER';
COMMENT ON COLUMN tb_zone.area_geometry IS '구역 폴리곤(geometry, SRID 4326). ST_Contains 기준';
COMMENT ON COLUMN tb_zone.created_at IS '생성 시각';
COMMENT ON COLUMN tb_zone.updated_at IS '수정 시각(트리거 자동 갱신)';

CREATE INDEX IF NOT EXISTS idx_zone_geom_gist ON tb_zone USING GIST (area_geometry);
CREATE INDEX IF NOT EXISTS idx_zone_site_type ON tb_zone (site_id, zone_type, level);
CREATE INDEX IF NOT EXISTS idx_zone_parent ON tb_zone (parent_zone_id);

CREATE TRIGGER trg_zone_updated_at
BEFORE UPDATE ON tb_zone
FOR EACH ROW EXECUTE FUNCTION guideon_set_updated_at();

-- =========================================================
-- 3.3 tb_place
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_place (
  place_id     BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  site_id      BIGINT NOT NULL REFERENCES tb_site(site_id) ON DELETE CASCADE,

  zone_id      BIGINT NULL,
  zone_source  zone_source_enum NOT NULL DEFAULT 'AUTO',

  name         VARCHAR(100) NOT NULL,
  name_json    JSONB NULL,
  category     VARCHAR(50) NOT NULL,

  -- 정확한 거리(미터) 계산을 위해 geography 사용
  location     geography(Point, 4326) NOT NULL,

  description  TEXT NULL,
  image_url    VARCHAR(500) NULL,
  is_active    BOOLEAN NOT NULL DEFAULT TRUE,

  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  -- 하위 테이블 무결성용(복합 FK 타겟)
  CONSTRAINT uk_place_id_site UNIQUE (place_id, site_id),

  -- zone도 site가 일치해야 함(OUTER면 zone_id NULL)
  CONSTRAINT fk_place_zone_site
    FOREIGN KEY (zone_id, site_id)
    REFERENCES tb_zone(zone_id, site_id)
    ON DELETE SET NULL
);

COMMENT ON TABLE tb_place IS '장소(POI). 거리검색 기준점. OUTER는 zone_id NULL.';
COMMENT ON COLUMN tb_place.place_id IS '장소 ID';
COMMENT ON COLUMN tb_place.site_id IS '소속 관광지';
COMMENT ON COLUMN tb_place.zone_id IS '캐싱된 구역 ID. OUTER면 NULL';
COMMENT ON COLUMN tb_place.zone_source IS '구역 할당 출처(AUTO=자동계산, MANUAL=관리자 보정)';
COMMENT ON COLUMN tb_place.name IS '기본 장소명(한국어)';
COMMENT ON COLUMN tb_place.name_json IS '다국어 장소명 JSONB(선택)';
COMMENT ON COLUMN tb_place.category IS '카테고리(TOILET/TICKET/RESTAURANT 등)';
COMMENT ON COLUMN tb_place.location IS '장소 좌표(geography). ST_Distance로 미터 계산';
COMMENT ON COLUMN tb_place.description IS 'UI 표시용 설명';
COMMENT ON COLUMN tb_place.image_url IS '썸네일 이미지 URL';
COMMENT ON COLUMN tb_place.is_active IS 'FALSE면 공사/폐쇄 등으로 검색/안내 제외';
COMMENT ON COLUMN tb_place.created_at IS '생성 시각';
COMMENT ON COLUMN tb_place.updated_at IS '수정 시각(트리거 자동 갱신)';

CREATE INDEX IF NOT EXISTS idx_place_loc_gist ON tb_place USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_place_filter ON tb_place (site_id, category, is_active);
CREATE INDEX IF NOT EXISTS idx_place_site_active ON tb_place (site_id, is_active);

CREATE TRIGGER trg_place_updated_at
BEFORE UPDATE ON tb_place
FOR EACH ROW EXECUTE FUNCTION guideon_set_updated_at();

-- =========================================================
-- 3.4 tb_device
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_device (
  device_id      VARCHAR(50) PRIMARY KEY,
  auth_token_hash CHAR(64) NOT NULL, -- SHA-256 hex

  site_id         BIGINT NOT NULL REFERENCES tb_site(site_id) ON DELETE CASCADE,

  zone_id         BIGINT NULL,
  zone_source     zone_source_enum NOT NULL DEFAULT 'AUTO',

  location_name   VARCHAR(100) NOT NULL,
  location        geography(Point, 4326) NOT NULL,

  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  last_ping       TIMESTAMPTZ NULL,
  last_auth_at    TIMESTAMPTZ NULL,

  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_device_zone_site
    FOREIGN KEY (zone_id, site_id)
    REFERENCES tb_zone(zone_id, site_id)
    ON DELETE SET NULL
);

COMMENT ON TABLE tb_device IS '키오스크 기기. 토큰은 해시만 저장. OUTER면 zone_id NULL.';
COMMENT ON COLUMN tb_device.device_id IS 'Unity config.json의 기기 ID';
COMMENT ON COLUMN tb_device.auth_token_hash IS '기기 인증 토큰 SHA-256(HEX) 해시';
COMMENT ON COLUMN tb_device.site_id IS '소속 관광지';
COMMENT ON COLUMN tb_device.zone_id IS '캐싱된 구역 ID. OUTER면 NULL';
COMMENT ON COLUMN tb_device.zone_source IS '구역 할당 출처(AUTO/MANUAL)';
COMMENT ON COLUMN tb_device.location_name IS '설치 위치명(UI 표시)';
COMMENT ON COLUMN tb_device.location IS '기기 좌표(geography)';
COMMENT ON COLUMN tb_device.is_active IS 'FALSE면 접속 차단';
COMMENT ON COLUMN tb_device.last_ping IS '헬스 체크(마지막 핑)';
COMMENT ON COLUMN tb_device.last_auth_at IS '마지막 인증 성공 시간';
COMMENT ON COLUMN tb_device.created_at IS '생성 시각';
COMMENT ON COLUMN tb_device.updated_at IS '수정 시각(트리거 자동 갱신)';

CREATE INDEX IF NOT EXISTS idx_device_loc_gist ON tb_device USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_device_site_active ON tb_device (site_id, is_active);

CREATE TRIGGER trg_device_updated_at
BEFORE UPDATE ON tb_device
FOR EACH ROW EXECUTE FUNCTION guideon_set_updated_at();

-- =========================================================
-- 3.5 tb_mascot (site 1:1)
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_mascot (
  mascot_id      BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  site_id        BIGINT NOT NULL UNIQUE REFERENCES tb_site(site_id) ON DELETE CASCADE,

  name           VARCHAR(50) NOT NULL,
  model_id       VARCHAR(80) NOT NULL,
  default_anim   VARCHAR(50) NOT NULL DEFAULT 'IDLE_A',

  greeting_msg   VARCHAR(200) NOT NULL,
  system_prompt  TEXT NOT NULL,

  tts_voice_id   VARCHAR(50) NOT NULL,
  tts_voice_json JSONB NULL,

  is_active      BOOLEAN NOT NULL DEFAULT TRUE,

  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE tb_mascot IS '관광지별 마스코트 설정(외형/페르소나/보이스/인사말). site 1:1.';

CREATE TRIGGER trg_mascot_updated_at
BEFORE UPDATE ON tb_mascot
FOR EACH ROW EXECUTE FUNCTION guideon_set_updated_at();

-- =========================================================
-- 3.6 tb_document (meta)
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_document (
  doc_id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  site_id         BIGINT NOT NULL REFERENCES tb_site(site_id) ON DELETE CASCADE,

  original_name   VARCHAR(255) NOT NULL,
  storage_url     VARCHAR(500) NOT NULL,
  file_hash       CHAR(64) NOT NULL,
  file_size       BIGINT NULL,

  chunk_size      INT NOT NULL DEFAULT 500,
  chunk_overlap   INT NOT NULL DEFAULT 50,
  embedding_model VARCHAR(100) NOT NULL DEFAULT 'text-embedding-3-small',

  status          doc_status_enum NOT NULL DEFAULT 'PENDING',
  failed_reason   TEXT NULL,
  processed_at    TIMESTAMPTZ NULL,

  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uk_doc_hash UNIQUE (site_id, file_hash),
  CONSTRAINT uk_doc_id_site UNIQUE (doc_id, site_id) -- tb_doc_chunk composite FK용
);

CREATE INDEX IF NOT EXISTS idx_doc_site_status ON tb_document (site_id, status);

CREATE TRIGGER trg_document_updated_at
BEFORE UPDATE ON tb_document
FOR EACH ROW EXECUTE FUNCTION guideon_set_updated_at();

-- =========================================================
-- 3.7 tb_doc_chunk (search engine)
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_doc_chunk (
  chunk_id     BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

  site_id      BIGINT NOT NULL,
  doc_id       BIGINT NOT NULL,

  chunk_index  INT NOT NULL,
  content      TEXT NOT NULL,
  metadata     JSONB NULL,

  embedding    vector(1536) NULL,

  content_tsv  TSVECTOR GENERATED ALWAYS AS (
    to_tsvector('simple', coalesce(content, ''))
  ) STORED,

  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_chunk_doc_site
    FOREIGN KEY (doc_id, site_id)
    REFERENCES tb_document(doc_id, site_id)
    ON DELETE CASCADE,

  CONSTRAINT uk_chunk_order UNIQUE (doc_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_chunk_embedding_hnsw
ON tb_doc_chunk USING hnsw (embedding vector_cosine_ops)
WHERE embedding IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_chunk_tsv_gin
ON tb_doc_chunk USING GIN (content_tsv);

CREATE INDEX IF NOT EXISTS idx_chunk_content_trgm
ON tb_doc_chunk USING GIN (content gin_trgm_ops);

-- =========================================================
-- 3.8 tb_place_detail
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_place_detail (
  detail_id   BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

  site_id     BIGINT NOT NULL,
  place_id    BIGINT NOT NULL,

  info_type   info_type_enum NOT NULL,
  content     TEXT NOT NULL,
  sub_info    VARCHAR(255) NULL,

  is_active   BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_detail_place_site
    FOREIGN KEY (place_id, site_id)
    REFERENCES tb_place(place_id, site_id)
    ON DELETE CASCADE,

  CONSTRAINT uk_place_detail UNIQUE (place_id, info_type)
);

CREATE INDEX IF NOT EXISTS idx_place_detail_site ON tb_place_detail (site_id, place_id, is_active);

CREATE TRIGGER trg_place_detail_updated_at
BEFORE UPDATE ON tb_place_detail
FOR EACH ROW EXECUTE FUNCTION guideon_set_updated_at();

-- =========================================================
-- 3.9 tb_daily_info
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_daily_info (
  id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

  site_id     BIGINT NOT NULL,
  place_id    BIGINT NOT NULL,

  target_date DATE NOT NULL,
  info_type   VARCHAR(50) NOT NULL,
  content     TEXT NOT NULL,

  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_daily_place_site
    FOREIGN KEY (place_id, site_id)
    REFERENCES tb_place(place_id, site_id)
    ON DELETE CASCADE,

  CONSTRAINT uk_daily UNIQUE (place_id, target_date, info_type)
);

CREATE INDEX IF NOT EXISTS idx_daily_site_date ON tb_daily_info (site_id, target_date);

CREATE TRIGGER trg_daily_updated_at
BEFORE UPDATE ON tb_daily_info
FOR EACH ROW EXECUTE FUNCTION guideon_set_updated_at();

-- =========================================================
-- 3.10 tb_audit_log
-- =========================================================
CREATE TABLE IF NOT EXISTS tb_audit_log (
  log_id       BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  site_id      BIGINT NOT NULL REFERENCES tb_site(site_id) ON DELETE CASCADE,

  actor_type   actor_type_enum NOT NULL,
  actor_id     VARCHAR(50) NOT NULL,

  target_table VARCHAR(50) NOT NULL,
  target_pk    VARCHAR(64) NOT NULL,

  action_type  action_type_enum NOT NULL,
  changes_json JSONB NULL,

  ip_address   VARCHAR(45) NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE tb_audit_log IS '감사 로그. actor_id는 ADMIN이면 admin:{admin_id} 권장.';
CREATE INDEX IF NOT EXISTS idx_audit_search ON tb_audit_log (site_id, target_table, created_at);

-- =========================================================
-- 4) Constraints (tb_zone consistency)
--   - level은 generated이므로 "parent 조건"만 강제하면 충분
-- =========================================================
ALTER TABLE tb_zone
ADD CONSTRAINT ck_zone_type_parent_consistency
CHECK (
  (zone_type = 'INNER' AND parent_zone_id IS NULL)
  OR
  (zone_type = 'SUB'   AND parent_zone_id IS NOT NULL)
);

COMMIT;
