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

## 빌드 및 테스트

```bash
cd frontend
npm run build

cd ../backend
gradlew.bat test
```
