# 우리동네 전통시장

React 프론트엔드와 Spring Boot 백엔드로 구성한 전통시장 프로젝트입니다.

## 프로젝트 구조

```text
전통시장/
├─ frontend/  React + TypeScript + Vite
└─ backend/   Java 17 + Spring Boot + Gradle
```

## 프론트엔드 실행

Node.js 18 이상이 필요합니다.

```bash
cd frontend
npm install
npm run dev
```

개발 서버는 `http://localhost:5173`에서 실행됩니다. `/api` 요청은 Vite 프록시를 통해 `http://localhost:8080`으로 전달됩니다.

## 백엔드 실행

Java 17 이상이 필요합니다. 별도 Gradle 설치는 필요하지 않습니다.

Windows:

```bash
cd backend
gradlew.bat bootRun
```

macOS/Linux:

```bash
cd backend
./gradlew bootRun
```

백엔드가 실행되면 `GET http://localhost:8080/api/health`에서 연결 상태를 확인할 수 있습니다.

### 데이터베이스

기본 실행은 별도 설치가 필요 없는 H2 인메모리 DB와 `local` 프로필을 사용합니다. 서버 실행 후 H2 콘솔은 `http://localhost:8080/h2-console`에서 확인할 수 있습니다.

```text
JDBC URL: jdbc:h2:mem:traditional_market
User Name: sa
Password: 입력하지 않음
```

MySQL을 사용할 때는 `traditional_market` 데이터베이스를 만든 뒤 환경변수를 설정하고 `mysql` 프로필로 실행합니다.

PowerShell:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/traditional_market?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '비밀번호'
./gradlew.bat bootRun --args='--spring.profiles.active=mysql'
```

현재 Market 테이블 설계는 [`docs/erd.md`](docs/erd.md)에서 확인할 수 있습니다.

### 시장 목록 API

```http
GET /api/markets?keyword=시장&status=OPEN&page=0&size=12&sort=LATEST
```

| 파라미터 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `keyword` | 아니요 | 없음 | 시장명 또는 주소 검색 |
| `status` | 아니요 | 없음 | `OPEN`, `TEMPORARILY_CLOSED`, `CLOSED` |
| `page` | 아니요 | `0` | 0부터 시작하는 페이지 번호 |
| `size` | 아니요 | `12` | 페이지 크기, 최대 50 |
| `sort` | 아니요 | `LATEST` | `LATEST` 또는 `NAME` |

`local` 프로필에서는 API 확인을 위한 샘플 시장 데이터 3건이 자동으로 등록됩니다.

### 시장 상세 조회 API

```http
GET /api/markets/{marketId}
```

존재하는 시장은 주소, 연락처, 영업시간, 휴무일, 좌표, 운영 상태와 생성·수정 시간을 포함한 상세 정보를 반환합니다. 존재하지 않는 ID는 `MARKET_NOT_FOUND` 코드와 함께 HTTP 404를 반환합니다.

### 시장별 점포 목록 API

```http
GET /api/markets/{marketId}/stores?keyword=수산&category=SEAFOOD&status=OPEN&page=0&size=12&sort=LATEST
```

| 파라미터 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `marketId` | 예 | 없음 | 점포가 소속된 시장 ID |
| `keyword` | 아니요 | 없음 | 점포명 또는 점포 소개 검색 |
| `category` | 아니요 | 없음 | `AGRICULTURAL`, `FOOD`, `SEAFOOD`, `LIVESTOCK`, `RESTAURANT`, `CLOTHING`, `HOUSEHOLD`, `ETC` |
| `status` | 아니요 | 없음 | `OPEN`, `TEMPORARILY_CLOSED`, `CLOSED` |
| `page` | 아니요 | `0` | 0부터 시작하는 페이지 번호 |
| `size` | 아니요 | `12` | 페이지 크기, 최대 50 |
| `sort` | 아니요 | `LATEST` | `LATEST` 또는 `NAME` |

존재하지 않는 시장 ID는 `MARKET_NOT_FOUND` 코드와 함께 HTTP 404를 반환합니다. `local` 프로필에서는 목록 API 확인을 위한 샘플 점포 데이터 6건이 자동 등록됩니다.

### 회원가입·로그인 API

인증은 `JSESSIONID` HttpOnly 쿠키를 사용하는 서버 세션 방식이며, 비밀번호는 BCrypt로 암호화하여 저장합니다. 세션 유효시간은 30분입니다.

```http
GET  /api/auth/csrf
POST /api/auth/signup
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout
```

회원가입 요청:

```json
{
  "email": "member@example.com",
  "password": "password123",
  "name": "시장사랑"
}
```

로그인 요청:

```json
{
  "email": "member@example.com",
  "password": "password123"
}
```

`POST`, `PUT` 같은 데이터 변경 요청 전에는 `GET /api/auth/csrf`를 호출하고, 응답의 `token` 값을 응답에 포함된 `headerName` 헤더로 전달해야 합니다. 로그인과 로그아웃 후에는 CSRF 토큰을 다시 요청하는 것을 권장합니다. 브라우저 요청에는 세션 쿠키 전달을 위해 `credentials: 'include'`를 사용합니다.

- 중복 이메일: HTTP 409, `EMAIL_ALREADY_EXISTS`
- 잘못된 로그인 정보: HTTP 401, `INVALID_CREDENTIALS`
- 인증되지 않은 사용자: HTTP 401, `AUTHENTICATION_REQUIRED`
- 누락되거나 잘못된 CSRF 토큰: HTTP 403, `ACCESS_DENIED`

## 빌드 및 테스트

```bash
cd frontend
npm run build

cd ../backend
gradlew.bat test
```
