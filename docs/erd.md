# 전통시장 ERD

현재 1차 구현 범위는 `Market` 도메인과 `markets` 테이블입니다. 이후 점포 기능을 구현할 때 `stores.market_id`가 `markets.id`를 참조하도록 확장합니다.

```mermaid
erDiagram
    MARKETS {
        BIGINT id PK
        VARCHAR_100 name "시장명, 필수"
        TEXT description "시장 소개"
        VARCHAR_255 address "기본 주소, 필수"
        VARCHAR_255 detail_address "상세 주소"
        VARCHAR_20 phone "연락처"
        VARCHAR_100 opening_hours "영업시간"
        VARCHAR_100 closed_days "휴무일"
        DECIMAL_10_7 latitude "위도"
        DECIMAL_10_7 longitude "경도"
        VARCHAR_500 image_url "대표 이미지 URL"
        VARCHAR_30 status "OPEN, TEMPORARILY_CLOSED, CLOSED"
        DATETIME created_at "생성일시"
        DATETIME updated_at "수정일시"
    }
```

## 설계 기준

- 시장명과 기본 주소는 필수 값입니다.
- 좌표는 부동소수점 오차를 피하기 위해 `DECIMAL(10, 7)`로 저장합니다.
- 운영 상태는 문자열 Enum으로 저장하여 DB 값의 의미를 바로 확인할 수 있습니다.
- 시장명과 운영 상태에는 목록 검색을 위한 인덱스를 생성합니다.
- 생성·수정 시간은 Spring Data JPA Auditing으로 자동 기록합니다.
