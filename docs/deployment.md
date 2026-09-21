# Docker 운영 배포 가이드

이 문서는 Docker Compose를 지원하는 단일 Linux 서버에 전통시장 서비스를 배포하는 절차를 설명합니다. 클라우드 공급자에 종속되지 않으며 AWS EC2, Oracle Cloud VM, 개인 VPS 등에 동일한 구성을 사용할 수 있습니다.

## 1. 운영 구성

```text
인터넷
  ↓ 80/443
로드 밸런서 또는 HTTPS 리버스 프록시
  ↓
frontend: Nginx + React 정적 파일
  ↓ /api
backend: Spring Boot
  ↓
mysql: MySQL 8.4 + 영구 볼륨
```

- 외부에 공개하는 포트는 프론트엔드의 80번뿐입니다.
- 백엔드 8080번과 MySQL 3306번은 Compose 내부 네트워크에서만 접근합니다.
- Nginx가 React 라우팅을 처리하고 `/api` 요청을 백엔드로 전달합니다.
- 백엔드 시작 시 Flyway가 아직 적용되지 않은 DB 마이그레이션을 실행합니다.
- MySQL 데이터는 `mysql-prod-data` 볼륨에 저장됩니다.

## 2. 서버 요구사항

- Docker Engine과 Docker Compose v2
- Git
- 최소 메모리 2GB 권장
- HTTPS를 적용할 도메인 또는 클라우드 로드 밸런서
- 카카오 지도 JavaScript 키와 토스페이먼츠 테스트 키는 해당 기능을 공개할 때 준비

## 3. 저장소와 환경변수 준비

```bash
git clone https://github.com/oddoos7/traditional-market.git
cd traditional-market
cp .env.production.example .env.production
chmod 600 .env.production
```

`.env.production`에서 다음 값을 반드시 변경합니다.

| 변수 | 설명 | 비밀값 여부 |
| --- | --- | --- |
| `MYSQL_PASSWORD` | 애플리케이션 DB 비밀번호 | 비밀 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 | 비밀 |
| `SESSION_COOKIE_SECURE` | HTTPS 배포에서는 `true` | 아니요 |
| `TOSS_PAYMENTS_SECRET_KEY` | 토스 테스트 시크릿 키 | 비밀 |
| `VITE_TOSS_PAYMENTS_CLIENT_KEY` | 토스 브라우저용 테스트 키 | 공개 |
| `VITE_KAKAO_MAP_APP_KEY` | 카카오 JavaScript 키 | 공개 |
| `SWAGGER_ENABLED` | 운영 Swagger 공개 여부 | 아니요 |

`VITE_`로 시작하는 값은 Vite 빌드 과정에서 브라우저 번들에 포함됩니다. 시크릿 키나 DB 비밀번호를 절대로 `VITE_` 변수에 넣지 않습니다.

## 4. 설정 검증과 최초 실행

```bash
docker compose --env-file .env.production -f compose.prod.yaml config
docker compose --env-file .env.production -f compose.prod.yaml up -d --build
docker compose --env-file .env.production -f compose.prod.yaml ps
```

세 컨테이너가 모두 `running` 또는 `healthy`인지 확인합니다.

```bash
curl --fail http://localhost/api/health
docker compose --env-file .env.production -f compose.prod.yaml logs --tail=100 backend
```

정상 응답 예시:

```json
{"status":"ok","service":"traditional-market-backend"}
```

## 5. 최초 관리자 계정

운영 DB에는 로컬 샘플 관리자 계정이 생성되지 않습니다. 최초 한 번만 `.env.production`을 다음과 같이 설정합니다.

```dotenv
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=충분히_긴_임시_비밀번호
BOOTSTRAP_ADMIN_NAME=시장 관리자
```

백엔드를 다시 시작하고 생성 로그를 확인합니다.

```bash
docker compose --env-file .env.production -f compose.prod.yaml up -d backend
docker compose --env-file .env.production -f compose.prod.yaml logs --tail=100 backend
```

계정 생성 후 `BOOTSTRAP_ADMIN_ENABLED=false`로 되돌리고 관리자 비밀번호를 안전하게 보관합니다. 같은 이메일이 이미 존재하면 계정을 덮어쓰거나 비밀번호를 변경하지 않습니다.

## 6. HTTPS와 외부 서비스 설정

- 실제 공개 환경에서는 클라우드 로드 밸런서, Caddy 또는 Nginx Proxy Manager 등을 사용하여 HTTPS를 적용합니다.
- HTTPS 적용 시 `SESSION_COOKIE_SECURE=true`를 유지합니다.
- 방화벽은 일반적으로 SSH, 80, 443만 허용하고 3306과 8080은 열지 않습니다.
- 카카오 개발자 콘솔의 JavaScript 키 도메인에 실제 HTTPS 도메인을 등록합니다.
- 토스페이먼츠 웹훅 URL을 `https://배포도메인/api/payments/toss/webhook`으로 등록합니다.
- 운영 문서가 필요하지 않으면 `SWAGGER_ENABLED=false`를 유지합니다.

## 7. 새 버전 배포

배포 전에 DB 백업과 Git 커밋을 확인합니다.

```bash
git pull --ff-only origin main
docker compose --env-file .env.production -f compose.prod.yaml build
docker compose --env-file .env.production -f compose.prod.yaml up -d
docker compose --env-file .env.production -f compose.prod.yaml ps
curl --fail http://localhost/api/health
```

프론트엔드 API 키는 빌드 시 포함되므로 키를 변경한 경우에도 `build` 단계가 필요합니다.

## 8. 로그와 장애 확인

```bash
docker compose --env-file .env.production -f compose.prod.yaml logs -f frontend
docker compose --env-file .env.production -f compose.prod.yaml logs -f backend
docker compose --env-file .env.production -f compose.prod.yaml logs -f mysql
```

백엔드 응답의 `X-Request-Id`를 로그의 `requestId`와 대조하면 한 요청의 처리 흐름을 찾을 수 있습니다. 컨테이너를 재시작해도 MySQL 볼륨은 유지됩니다.

```bash
docker compose --env-file .env.production -f compose.prod.yaml restart backend
```

전체 서비스를 중지하되 DB 볼륨은 보존하려면 다음 명령을 사용합니다.

```bash
docker compose --env-file .env.production -f compose.prod.yaml down
```

`down -v`는 운영 DB 볼륨까지 삭제하므로 데이터 폐기가 명확히 필요한 상황이 아니면 사용하지 않습니다.

## 9. 배포 전 체크리스트

- [ ] `.env.production`이 Git 추적 대상이 아닌지 확인
- [ ] 기본 예제 비밀번호를 모두 긴 무작위 값으로 교체
- [ ] HTTPS와 `SESSION_COOKIE_SECURE=true` 적용
- [ ] 3306·8080 포트가 외부에 공개되지 않았는지 확인
- [ ] GitHub Actions CI가 통과했는지 확인
- [ ] `/api/health`가 정상 응답하는지 확인
- [ ] Flyway 마이그레이션 성공 로그 확인
- [ ] 카카오 허용 도메인과 토스 웹훅 URL 확인
- [ ] 실제 사용자 데이터가 있다면 DB 백업 정책 준비
