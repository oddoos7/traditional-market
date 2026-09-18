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

점포 상세 화면에서는 상품 검색, 판매 상태 필터, 가격·이름·최신순 정렬과 페이지 이동을 제공합니다. 상품 카드를 선택하면 가격, 재고, 판매 상태를 확인할 수 있는 상세 화면으로 이동합니다. 로그인 회원은 상품 상세에서 수량을 선택해 장바구니에 담고, 주문 생성·주문 내역 조회·결제 전 취소를 할 수 있습니다. 관리자는 점포 상세에서 상품을 등록하고 상품 상세에서 수정할 수 있으며, `HIDDEN`으로 설정한 상품은 공개 화면에서 제외됩니다.

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
- 생성된 주문 상태는 `PENDING_PAYMENT`입니다. 다음 결제 단계에서 승인되면 `PAID`로 전환할 예정입니다.
- `PENDING_PAYMENT` 주문을 취소하면 상태가 `CANCELED`로 바뀌고 차감했던 재고가 복원됩니다. 같은 주문을 두 번 취소해 재고가 중복 복원되는 것은 차단합니다.
- 주문 상품에는 주문 당시 상품명, 이미지, 단가와 수량을 저장하므로 이후 상품 정보가 변경되어도 주문 내역은 그대로 유지됩니다.
- 주문자가 아닌 회원은 해당 주문을 조회하거나 취소할 수 없으며 존재하지 않는 주문과 동일하게 `ORDER_NOT_FOUND`를 반환합니다.
- 빈 장바구니는 `EMPTY_CART`, 주문 시점의 판매 불가·재고 부족은 `ORDER_STOCK_UNAVAILABLE`, 취소할 수 없는 상태는 `ORDER_NOT_CANCELABLE` 코드로 응답합니다.

재고는 주문 생성 시 결제 대기 수량으로 먼저 확보됩니다. 장시간 결제하지 않은 주문을 자동 만료해 재고를 복원하는 작업은 결제 웹훅·멱등성 단계에서 추가할 예정입니다.

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

## 빌드 및 테스트

```bash
cd frontend
npm run build

cd ../backend
gradlew.bat test
```
