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

점포 상세 화면에서는 상품 검색, 판매 상태 필터, 가격·이름·최신순 정렬과 페이지 이동을 제공합니다. 상품 카드를 선택하면 가격, 재고, 판매 상태를 확인할 수 있는 상세 화면으로 이동합니다. 로그인 회원은 상품 상세에서 수량을 선택해 장바구니에 담고, 주문 생성·주문 내역 조회·토스페이먼츠 테스트 결제를 할 수 있습니다. 관리자는 점포 상세에서 상품을 등록하고 상품 상세에서 수정할 수 있으며, `HIDDEN`으로 설정한 상품은 공개 화면에서 제외됩니다.

토스페이먼츠 개발자센터의 API 키 메뉴에서 **주문서형·결제창형 테스트 클라이언트 키**를 복사해 `frontend/.env.local`에 설정합니다. 결제위젯 연동 키는 `test_gck_`로 시작해야 합니다.

```powershell
Set-Location frontend
Copy-Item .env.example .env.local
# .env.local의 VITE_TOSS_PAYMENTS_CLIENT_KEY 값을 본인의 test_gck_... 키로 변경
npm run dev
```

클라이언트 키는 브라우저에서 사용하는 공개 키지만, 아래의 시크릿 키는 절대로 프론트 코드나 Git에 올리면 안 됩니다.

## 백엔드 실행

Java 17 이상이 필요합니다. 별도 Gradle 설치는 필요하지 않습니다.

Windows:

```powershell
cd backend
$env:TOSS_PAYMENTS_SECRET_KEY = 'test_gsk_본인의_테스트_시크릿_키'
.\gradlew.bat bootRun
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

H2에서도 Flyway가 `backend/src/main/resources/db/migration`의 SQL을 실행하며, Hibernate는 생성된 스키마와 엔티티가 일치하는지 검증합니다.

### Docker MySQL 실행

Docker Desktop을 실행한 뒤 프로젝트 최상위 폴더에서 MySQL 8.4 컨테이너를 시작합니다. 기본 개발용 계정은 Compose 설정과 `mysql` 프로필에 동일하게 맞춰져 있어 별도 환경변수 없이 실행할 수 있습니다.

PowerShell:

```powershell
docker compose up -d mysql
docker compose ps

cd backend
.\gradlew.bat bootRun --args="--spring.profiles.active=mysql"
```

MySQL 데이터는 `mysql-data` Docker 볼륨에 저장되므로 컨테이너를 내려도 유지됩니다.

```powershell
docker compose down
```

포트나 비밀번호를 바꾸려면 `.env.example`을 `.env`로 복사한 뒤 값을 수정하고, 백엔드에는 같은 값을 환경변수로 전달합니다.

```powershell
Copy-Item .env.example .env
$env:DB_USERNAME = 'market'
$env:DB_PASSWORD = '변경한 비밀번호'
```

Flyway는 서버 시작 시 아직 적용되지 않은 `V숫자__설명.sql` 파일을 순서대로 한 번씩 실행합니다. 상품 테이블을 추가할 때 기존 `V1__create_initial_schema.sql`을 고치지 말고 `V2__create_products.sql`처럼 새 파일을 추가해야 합니다.

현재 적용된 마이그레이션은 다음 명령으로 확인할 수 있습니다.

```powershell
docker compose exec mysql mysql -umarket -pmarket-local-password traditional_market -e "SELECT installed_rank, version, description, success FROM flyway_schema_history;"
```

현재 전체 데이터베이스 설계는 [`docs/erd.md`](docs/erd.md)에서 확인할 수 있습니다.

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

### 점포별 상품 API

```http
GET  /api/stores/{storeId}/products?keyword=사과&status=ON_SALE&page=0&size=12&sort=PRICE_LOW
GET  /api/stores/{storeId}/products/{productId}
POST /api/stores/{storeId}/products
PUT  /api/stores/{storeId}/products/{productId}
```

| 파라미터 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `storeId` | 예 | 없음 | 상품이 소속된 점포 ID |
| `keyword` | 아니요 | 없음 | 상품명 또는 상품 설명 검색 |
| `status` | 아니요 | 없음 | `ON_SALE`, `SOLD_OUT`, `HIDDEN` |
| `page` | 아니요 | `0` | 0부터 시작하는 페이지 번호 |
| `size` | 아니요 | `12` | 페이지 크기, 최대 50 |
| `sort` | 아니요 | `LATEST` | `LATEST`, `NAME`, `PRICE_LOW`, `PRICE_HIGH` |

상품 등록·수정은 관리자만 가능하며 CSRF 토큰이 필요합니다.

```json
{
  "name": "새콤달콤 사과 1kg",
  "description": "아삭하고 달콤한 제철 사과입니다.",
  "price": 8500.00,
  "stockQuantity": 30,
  "imageUrl": null,
  "status": "ON_SALE"
}
```

- 가격은 0원보다 크고 정수 10자리·소수 2자리 이하여야 합니다.
- 재고 수량은 `0`부터 `1000000`까지 입력할 수 있습니다.
- `HIDDEN` 상품은 공개 목록과 상세 API에서 조회되지 않습니다.
- 존재하지 않는 점포는 `STORE_NOT_FOUND`, 존재하지 않거나 다른 점포에 속한 상품은 `PRODUCT_NOT_FOUND`를 반환합니다.
- `local` 프로필에서는 화면 개발용 샘플 상품 8건이 자동 등록됩니다.

### 장바구니 API

장바구니는 로그인 회원만 이용할 수 있으며 데이터 변경 요청에는 CSRF 토큰이 필요합니다.

```http
GET    /api/cart
POST   /api/cart/items
PUT    /api/cart/items/{cartItemId}
DELETE /api/cart/items/{cartItemId}
DELETE /api/cart
```

상품 담기 요청:

```json
{
  "productId": 1,
  "quantity": 2
}
```

수량 변경 요청:

```json
{
  "quantity": 3
}
```

- 회원당 하나의 장바구니를 사용하며 같은 상품을 다시 담으면 기존 수량에 합산합니다.
- 상품별 수량은 1~99개이며 현재 재고보다 많이 담을 수 없습니다.
- `ON_SALE` 상태이고 재고가 남은 상품만 새로 담을 수 있습니다.
- 한 장바구니에는 한 점포의 상품만 담을 수 있습니다. 다른 점포 상품을 담으려면 기존 장바구니를 먼저 비워야 합니다.
- 품절이나 재고 변경으로 구매할 수 없게 된 상품도 장바구니에서 확인하고 삭제할 수 있습니다.
- 다른 점포 상품 충돌은 `CART_STORE_MISMATCH`, 재고 초과는 `CART_QUANTITY_EXCEEDED`, 판매 불가 상품은 `PRODUCT_NOT_AVAILABLE` 코드로 응답합니다.

### 주문 및 재고 API

주문 API는 로그인 회원만 이용할 수 있으며 생성·취소 요청에는 CSRF 토큰이 필요합니다.

```http
POST /api/orders
GET  /api/orders
GET  /api/orders/{orderId}
POST /api/orders/{orderId}/cancel
```

- 주문 생성 요청에는 별도 본문이 없으며 현재 장바구니 전체를 한 주문으로 전환합니다.
- 주문 생성 시 상품 행을 ID 순서로 잠근 뒤 최신 판매 상태와 재고를 다시 검증합니다. 검증이 끝나면 재고를 차감하고 장바구니를 비웁니다.
- 생성된 주문 상태는 `PENDING_PAYMENT`이며 토스페이먼츠 승인이 완료되면 `PAID`로 전환됩니다.
- `PENDING_PAYMENT` 주문을 취소하면 상태가 `CANCELED`로 바뀌고 차감했던 재고가 복원됩니다. 같은 주문을 두 번 취소해 재고가 중복 복원되는 것은 차단합니다.
- 주문 상품에는 주문 당시 상품명, 이미지, 단가와 수량을 저장하므로 이후 상품 정보가 변경되어도 주문 내역은 그대로 유지됩니다.
- 주문자가 아닌 회원은 해당 주문을 조회하거나 취소할 수 없으며 존재하지 않는 주문과 동일하게 `ORDER_NOT_FOUND`를 반환합니다.
- 빈 장바구니는 `EMPTY_CART`, 주문 시점의 판매 불가·재고 부족은 `ORDER_STOCK_UNAVAILABLE`, 취소할 수 없는 상태는 `ORDER_NOT_CANCELABLE` 코드로 응답합니다.

재고는 주문 생성 시 결제 대기 수량으로 먼저 확보됩니다. 결제 전 주문 취소 또는 결제 완료 후 전액 취소가 확정되면 재고를 한 번만 복원합니다. 장시간 결제하지 않은 주문을 자동 만료하는 스케줄러는 추후 운영 단계에서 추가할 수 있습니다.

### 토스페이먼츠 테스트 결제 API

주문 상세 화면은 토스페이먼츠 결제위젯 SDK v2를 사용합니다. 브라우저의 성공 URL에 전달된 값을 백엔드로 보내면, 백엔드는 로그인 회원의 주문을 잠그고 DB에 저장된 주문 금액과 요청 금액을 비교한 뒤 토스 결제 승인 API를 호출합니다.

```http
POST /api/payments/toss/confirm
POST /api/payments/toss/orders/{orderId}/cancel
POST /api/payments/toss/webhook
```

```json
{
  "paymentKey": "토스가_전달한_결제키",
  "orderId": "ORD-주문번호",
  "amount": 17000
}
```

- 로그인과 CSRF 토큰이 필요하며 본인 주문만 승인할 수 있습니다.
- 서버는 브라우저가 보낸 금액을 그대로 신뢰하지 않고 주문 DB 금액을 다시 확인합니다.
- `TOSS_PAYMENTS_SECRET_KEY`가 `test_`로 시작하지 않으면 외부 승인 요청을 차단합니다.
- 승인 성공 시 `payments`에 결제 결과를 저장하고 주문 상태와 `paid_at`을 함께 갱신합니다.
- 테스트 키로 진행한 결제는 실제 금액이 청구되지 않습니다.
- 승인과 전액 취소 요청에는 주문·결제 식별자로 만든 안정적인 `Idempotency-Key`를 토스 API에 전달합니다. 서버 재시도나 브라우저 중복 요청이 발생해도 저장된 첫 결과를 재사용합니다.

결제 전액 취소 요청 본문:

```json
{
  "cancelReason": "구매자 요청"
}
```

- 결제 취소는 본인의 `PAID` 주문만 가능하며 토스 취소 응답의 결제 키, 주문번호, 금액, 잔액과 취소 거래 키를 검증합니다.
- 취소가 검증되면 `payment_cancellations`에 취소 거래를 저장하고 결제와 주문을 `CANCELED`로 변경한 뒤 재고를 정확히 한 번 복원합니다.
- 사용자 API는 전액 취소만 제공합니다. 토스 조회 결과에서 부분 취소가 확인되면 결제 잔액과 취소 거래는 동기화하지만 주문 전체 취소나 재고 복원은 하지 않습니다.

웹훅 URL은 다음 경로로 등록합니다.

```text
https://배포도메인/api/payments/toss/webhook
```

- 현재 처리 이벤트는 `PAYMENT_STATUS_CHANGED`이며 토스의 `tosspayments-webhook-transmission-id` 헤더가 필요합니다.
- 일반 결제 웹훅에는 서명 헤더가 없으므로 수신 본문을 바로 신뢰하지 않습니다. `paymentKey`로 토스 결제 조회 API를 다시 호출하고 조회 결과만 DB에 반영합니다.
- 처리한 전송 ID와 본문 지문을 `toss_webhook_events`에 저장하여 같은 웹훅 재전송을 중복 처리하지 않습니다.
- 웹훅은 외부에서 접근할 수 있는 HTTPS 주소가 필요하므로 로컬 전용 주소는 등록할 수 없습니다.

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
- 누락되거나 잘못된 CSRF 토큰: HTTP 403, `INVALID_CSRF_TOKEN`
- 인증된 사용자의 권한 부족: HTTP 403, `ACCESS_DENIED`

## Swagger·로그·CI

백엔드를 실행하면 다음 주소에서 OpenAPI 문서와 Swagger UI를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

- API는 시장, 점포, 상품, 리뷰, 찜, 장바구니, 주문, 결제와 인증 태그로 구분됩니다.
- Swagger UI에서 데이터 변경 API를 호출하려면 먼저 `GET /api/auth/csrf`를 호출합니다. Swagger UI의 CSRF 지원이 활성화되어 표준 `XSRF-TOKEN` 쿠키를 이후 요청의 `X-XSRF-TOKEN` 헤더로 전달합니다.
- 모든 HTTP 응답에는 `X-Request-Id`가 포함됩니다. 클라이언트가 안전한 형식의 요청 ID를 보내면 유지하고, 없거나 올바르지 않으면 서버가 UUID를 발급합니다.
- 요청 로그에는 요청 ID, HTTP 메서드, 경로, 응답 상태와 처리 시간이 기록됩니다. 요청 본문, 비밀번호, 세션 쿠키와 결제 키는 기록하지 않습니다.
- 예상하지 못한 서버 오류는 전체 예외를 서버 로그에 기록하되 클라이언트에는 `INTERNAL_SERVER_ERROR` 표준 응답만 반환합니다.
- `.github/workflows/ci.yml`은 `main` 브랜치 push와 pull request에서 Java 17 백엔드 전체 테스트, Node.js 22 프론트엔드 테스트와 프로덕션 빌드를 자동 실행합니다.

## 빌드 및 테스트

```bash
cd frontend
npm run build

cd ../backend
gradlew.bat test
```
