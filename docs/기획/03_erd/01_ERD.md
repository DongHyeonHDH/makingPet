# 01. ERD (초안)

> ⚠️ 2026-11-16 초안. API 명세서 작성 단계에서 컬럼·제약을 확정한다. 본 박스가 최신이다.
> 규칙: 테이블·컬럼은 snake_case. PK는 UUID(현재 `members`와 맞춤, 결정 D-3). 시간은 `timestamptz`, 서버 기준 KST로 해석.
> 모든 엔티티는 `created_at`, `updated_at`을 가진다(BaseEntity). 소프트 딜리트가 필요한 테이블만 `deleted_at`.

## 변경 박스
| 엔티티 | 변경 |
|---|---|
| members | 기존 테이블(id, email, nickname, password_hash)에 `role` 추가 예정 |
| user_pets | 중복 펫 = 코인 환급 확정(D-8)에 따라 `(member_id, species_id)` 유니크 제약 추가 (삭제된 펫 제외 부분 인덱스) |
| pet_species | D-13·D-14 (2026-11-18): 4계열 × 29형태 = 116종. `line_code`, `attribute_count`, `primary_attribute`, `sub_attributes` 추가. 진화형 112형태 추가(아트 전 비활성) |
| user_pets | D-13: `stage`는 쓰지 않는다. 단계는 `pet_species.attribute_count`에서 얻는다 |
| gacha_pity_counters, gacha_pools.pity_threshold | D-13: 천장 폐지로 미사용. 제거 검토 |
| evolution_costs, pet_evolutions, items.attribute | D-14: M2 진화용 신규 (마이그레이션 V3) |
| species_motions | D-14: PK를 `(species_id, affection_level)`에서 `(motion_group, affection_level)`로. 움직임은 계열·주속성 단위 공유 |
| items | D-14: `FRUIT` 4종 추가, `tradable = true` |

## 한 장 다이어그램
```
members 1──1 wallets 1──N coin_transactions
   │
   ├─1──N user_pets N──1 pet_species N──1 (rarity)
   │        │                │
   │        │                └─1──N species_motions ──N:1 items(MOTION)
   │        └─1──N pet_equipments N──1 items
   │        └─1──N pet_interaction_cooldowns
   │
   ├─1──N inventory_items N──1 items
   ├─1──1 homes 1──N home_furnitures N──1 items(FURNITURE)
   │
   ├─1──N gacha_draws N──1 gacha_pools 1──N gacha_pool_entries N──1 pet_species
   ├─1──N gacha_pity_counters (pool별)
   │
   ├─1──N reward_claims            (출석·가위바위보·슬롯 기록)
   ├─1──N daily_counters
   │
   ├─1──N auction_listings N──1 items
   │        └─1──0..1 auction_trades
   ├─N──M auction_listings  (auction_favorites)
   │
shop_products N──1 items
notices (작성자 = members)
```

## 테이블 상세

### members (기존)
| 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
|---|---|---|---|---|---|
| id | uuid | N | | PK | |
| email | varchar(254) | N | | UK | 소문자 정규화 |
| nickname | varchar(24) | N | | UK | 마이 홈 주소에 사용 |
| password_hash | varchar | N | | | PBKDF2 |
| role | varchar(20) | N | 'USER' | | USER / ADMIN / SUPER_ADMIN (추가) |
| representative_pet_id | uuid | Y | | | 대표 펫 (추가) |
| home_visibility | varchar(10) | N | 'PUBLIC' | | (추가, homes로 옮길 수도 있음) |

### pet_species (펫 종 카탈로그)
| 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
|---|---|---|---|---|---|
| id | uuid | N | | PK | |
| code | varchar(40) | N | | UK | `duck`, `cat` |
| name | varchar(40) | N | | | 오리 |
| rarity | varchar(12) | N | | | COMMON~LEGENDARY |
| base_asset_key | varchar(200) | N | | | 기본 이미지 키 |
| description | varchar(300) | Y | | | 도감 설명 |
| active | boolean | N | true | | 뽑기·도감 노출 |
| line_code | varchar(20) | N | | IDX | 계열: `duck` / `dog` / `cat` / `dinosaur` (D-13) |
| attribute_count | smallint | N | 0 | | 속성 수 0~3. 희귀도와 1:1 (0 = COMMON … 3 = LEGENDARY) |
| primary_attribute | varchar(10) | N | '' | | 주속성 `FIRE`/`WATER`/`GRASS`/`ELECTRIC`. 기본종은 빈 문자열 |
| sub_attributes | varchar(30) | N | '' | | 부속성 알파벳순 쉼표 목록 (`GRASS,WATER`). 없으면 빈 문자열 |
| motion_group | varchar(40) | N | | IDX | 움직임 그룹. 기본종 = 계열 코드, 진화형 = `<계열>_<주속성>` (D-14) |
- UK: `(line_code, primary_attribute, sub_attributes)` — 계열 안에서 형태당 1행. NULL 대신 빈 문자열을 쓰는 이유: PostgreSQL 유니크는 NULL끼리 다르다고 본다
- CHECK: `rarity`가 `attribute_count`와 맞는다 (0 COMMON / 1 RARE / 2 EPIC / 3 LEGENDARY). `primary_attribute`는 `sub_attributes`에 없다
- 뽑기 풀 엔트리는 `attribute_count = 0`인 종만 허용한다(서비스에서 검증)

### species_motions (친밀도 단계별 해금 움직임)
| 컬럼 | 타입 | NULL | 키 | 설명 |
|---|---|---|---|---|
| motion_group | varchar(40) | N | PK | D-14: 종이 아니라 움직임 그룹 단위 (`duck`, `duck_fire`). M1은 기본종 코드와 같아 그대로 동작 |
| affection_level | smallint | N | PK | 2~5 |
| motion_item_id | uuid | N | FK items | |

### evolution_costs (M2, 진화 비용 — D-14)
| 컬럼 | 타입 | NULL | 키 | 설명 |
|---|---|---|---|---|
| to_attribute_count | smallint | N | PK | 진화 **후** 속성 수 1~3 |
| required_affection_level | smallint | N | | 3 / 4 / 5 |
| fruit_quantity | smallint | N | | 고른 속성 열매 3 / 5 / 8 |
- 규칙 §8-5의 수치를 코드 수정 없이 조정하려고 테이블로 뺀다. 행은 3개뿐이다.
- 결과 형태는 (계열, 주속성, 부속성 + 새 속성)으로 `pet_species`에서 찾는다. 다음 종을 가리키는 FK는 두지 않는다(갈래가 여러 개라서).

### pet_evolutions (M2, 진화 기록 — D-14)
| 컬럼 | 타입 | NULL | 키 | 설명 |
|---|---|---|---|---|
| id | uuid | N | PK | |
| pet_id | uuid | N | FK user_pets | |
| from_species_id | uuid | N | FK | |
| to_species_id | uuid | N | FK | |
| attribute | varchar(10) | N | | 이번에 얻은 속성 |
| fruit_item_id | uuid | N | FK items | |
| fruit_quantity | smallint | N | | |
| created_at | timestamptz | N | | |

### user_pets (보유 펫)
| 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
|---|---|---|---|---|---|
| id | uuid | N | | PK | |
| member_id | uuid | N | | FK | idx_user_pets_member |
| species_id | uuid | N | | FK | |
| name | varchar(12) | N | | | 기본값 = 종 이름 |
| mood | smallint | N | 70 | | 마지막 갱신 시점 값 |
| mood_updated_at | timestamptz | N | now | | 지연 계산 기준 |
| affection | int | N | 0 | | 누적 |
| affection_level | smallint | N | 1 | | |
| stage | smallint | N | 1 | | D-13 이후 미사용. 단계는 `pet_species.attribute_count`에서 얻는다. 제거 검토 |
| obtained_from | varchar(20) | N | | | GACHA / GIFT / ADMIN |
| deleted_at | timestamptz | Y | | | 놓아주기 (소프트) |
- UK: `(member_id, species_id) WHERE deleted_at IS NULL` — 종당 1마리. 뽑기 중복 판정의 근거이자 동시 뽑기 시 안전장치

### pet_interaction_cooldowns
| 컬럼 | 타입 | NULL | 키 | 설명 |
|---|---|---|---|---|
| pet_id | uuid | N | PK | |
| type | varchar(10) | N | PK | PET / PLAY / SNACK |
| next_available_at | timestamptz | N | | |

### items (아이템 카탈로그)
| 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
|---|---|---|---|---|---|
| id | uuid | N | | PK | |
| code | varchar(40) | N | | UK | `ribbon` |
| name | varchar(40) | N | | | 딸기 리본 |
| category | varchar(12) | N | | | TOP / BOTTOM / ACCESSORY / MOTION / FURNITURE / THEME / (M2) FRUIT |
| rarity | varchar(12) | N | 'COMMON' | | |
| asset_key | varchar(200) | N | | | 이미지 또는 motion_key |
| tradable | boolean | N | true | | MOTION은 false |
| attribute | varchar(10) | Y | | | `FRUIT`만. 이 열매가 주는 속성 (D-14) |
| active | boolean | N | true | | |

### inventory_items (보유 수량)
| 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
|---|---|---|---|---|---|
| member_id | uuid | N | | PK | |
| item_id | uuid | N | | PK | |
| quantity | int | N | 0 | | 경매 에스크로 중인 수량은 이미 빠진 값 |
| reserved | int | N | 0 | | 장착·배치 중 수량 |
- CHECK: `quantity >= 0`, `reserved >= 0`, `reserved <= quantity`
- 거래 가능 수량 = `quantity - reserved`

### pet_equipments
| 컬럼 | 타입 | NULL | 키 | 설명 |
|---|---|---|---|---|
| pet_id | uuid | N | PK | |
| slot | varchar(12) | N | PK | TOP / BOTTOM / ACCESSORY_1 / ACCESSORY_2 / MOTION |
| item_id | uuid | N | FK | |

### homes
| 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
|---|---|---|---|---|---|
| member_id | uuid | N | | PK | 1:1 |
| theme_item_id | uuid | N | | FK | 기본 테마 |
| visibility | varchar(10) | N | 'PUBLIC' | | |
| like_count | int | N | 0 | | COULD |

### home_furnitures
| 컬럼 | 타입 | NULL | 키 | 설명 |
|---|---|---|---|---|
| id | uuid | N | PK | |
| member_id | uuid | N | FK | 최대 20행 |
| item_id | uuid | N | FK | |
| pos_x / pos_y | smallint | N | | 0~100 |
| layer | smallint | N | | |

### wallets
| 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
|---|---|---|---|---|---|
| member_id | uuid | N | | PK | |
| balance | bigint | N | 0 | | CHECK `balance >= 0` |

### coin_transactions (원장, append-only)
| 컬럼 | 타입 | NULL | 키 | 설명 |
|---|---|---|---|---|
| id | uuid | N | PK | |
| member_id | uuid | N | FK | idx (member_id, created_at desc) |
| amount | bigint | N | | +입금 / −출금 |
| balance_after | bigint | N | | |
| reason | varchar(30) | N | | SIGNUP_GIFT, ATTENDANCE, RPS, SLOT_ENTRY, SLOT_PAYOUT, GACHA, GACHA_REFUND, AFFECTION_REWARD, SHOP_PURCHASE, AUCTION_BUY, AUCTION_SELL, AUCTION_FEE, ADMIN_ADJUST |
| ref_type / ref_id | varchar / uuid | Y | | 원인 레코드 |
| memo | varchar(200) | Y | | 관리자 보정 사유 |

### gacha_pools / gacha_pool_entries
| gacha_pools 컬럼 | 타입 | 설명 |
|---|---|---|
| id | uuid PK | |
| name | varchar(40) | 상시 뽑기 |
| type | varchar(10) | STANDARD / LIMITED |
| single_price / multi_price | int | 100 / 900 |
| pity_threshold | int | 50 |
| starts_at / ends_at | timestamptz NULL | 한정 풀 |
| active | boolean | |

| gacha_pool_entries 컬럼 | 타입 | 설명 |
|---|---|---|
| pool_id, species_id | uuid PK | |
| weight | int | 확률 가중치 (예: 만분율, 합계 10000) |

### gacha_draws (뽑기 기록)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | uuid PK | |
| member_id | uuid | idx (member_id, created_at desc) |
| pool_id | uuid | |
| batch_id | uuid | 10연 묶음 |
| species_id | uuid | 결과 |
| rarity | varchar(12) | 스냅샷 |
| pity_triggered | boolean | |
| result_type | varchar(10) | NEW_PET / REFUND |
| user_pet_id | uuid NULL | |
| cost | int | 이 회차에 배분된 비용 |

### gacha_pity_counters
| member_id, pool_id (PK) | count int | 마지막 EPIC 이상 이후 횟수 |

### reward_claims / daily_counters
| reward_claims 컬럼 | 타입 | 설명 |
|---|---|---|
| id | uuid PK | |
| member_id | uuid | |
| type | varchar(12) | ATTENDANCE / RPS / SLOT |
| reward_date | date | KST 날짜 |
| detail | jsonb | 가위바위보 손·결과, 슬롯 심볼, 연속일 등 |
| coin_delta | int | |
- UK: `(member_id, type, reward_date)` — **ATTENDANCE, RPS에만** 부분 유니크 인덱스. SLOT은 `daily_counters`로 횟수 관리.

| daily_counters | `(member_id, counter_date, type)` PK, `count int` |

### shop_products
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | uuid PK | |
| item_id | uuid FK | |
| price | int | |
| sale_starts_at / sale_ends_at | timestamptz NULL | |
| visible | boolean | |
| sort_order | int | |

### shop_purchases (W7 추가)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | uuid PK | S-3 응답 `purchaseId` = 원장 `coin_transactions.ref_id` (ref_type `SHOP_PURCHASE`) |
| member_id | uuid | idx (member_id, created_at) |
| product_id / item_id | uuid | |
| quantity | int | 1~10 |
| unit_price / total_price | int | 구매 시점 가격 스냅샷 |

### auction_listings
| 컬럼 | 타입 | NULL | 키 | 설명 |
|---|---|---|---|---|
| id | uuid | N | PK | |
| seller_id | uuid | N | FK | idx (seller_id, status) |
| item_id | uuid | N | FK | idx (item_id, status, price) — 시세·검색 |
| item_name | varchar(40) | N | | 검색용 스냅샷 |
| item_category | varchar(12) | N | | 필터용 스냅샷 |
| quantity | int | N | | 1~10 (기본 1). 2 이상은 `FRUIT`만 (D-14, 서비스에서 검증) |
| price | int | N | | 1~99,999. 묶음이면 묶음 전체 가격 |
| unit_price | numeric(7,1) | N | | price ÷ quantity, 소수 첫째 자리 반올림. 단가 정렬·시세용. idx (item_id, status, unit_price) |
| status | varchar(10) | N | | ON_SALE / SOLD / CANCELLED / EXPIRED / REMOVED |
| expires_at | timestamptz | N | | idx (status, expires_at) — 만료 스케줄러 |
| closed_at | timestamptz | Y | | |

### auction_trades
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | uuid PK | |
| listing_id | uuid UK | 매물당 1건 |
| buyer_id, seller_id | uuid | |
| item_id | uuid | idx (item_id, created_at) — 시세 |
| quantity | int | 거래 수량 (묶음) |
| price, fee, seller_income | int | 묶음 전체 기준 |
| unit_price | numeric(7,1) | 시세(단가 평균)용 스냅샷 |

### auction_favorites
| member_id, listing_id (PK) | created_at |

### notices
| id uuid PK | title varchar(100) | content text | author_id uuid | pinned boolean | published_at timestamptz | deleted_at |

> W7 구현: `content`는 `varchar(20000)`(H2·PostgreSQL 공통), `author_id`는 NULL 허용(시드 공지는 작성자 없음 → "운영자" 표시). `published_at`이 미래면 예약 게시.

## 관계 요약
| 관계 | 카디널리티 | FK | ON DELETE |
|---|---|---|---|
| members – wallets | 1:1 | wallets.member_id | RESTRICT (소프트 딜리트만) |
| members – user_pets | 1:N | user_pets.member_id | RESTRICT |
| pet_species – user_pets | 1:N | user_pets.species_id | RESTRICT |
| user_pets – pet_equipments | 1:N | pet_equipments.pet_id | CASCADE |
| items – inventory_items | 1:N | inventory_items.item_id | RESTRICT |
| auction_listings – auction_trades | 1:0..1 | auction_trades.listing_id | RESTRICT |

## 설계 사유
- **아이템 수량형 + reserved 컬럼**: 의상이 개체별 속성(강화·내구도)을 갖지 않으므로 수량형이 단순하다. 장착·배치는 `reserved`로 잡아 거래와 충돌하지 않게 한다.
- **경매 에스크로 = quantity 차감**: 등록 시 인벤토리에서 바로 빼면 "등록 중인 아이템을 장착"하는 경우가 구조적으로 사라진다.
- **매물에 아이템 이름·카테고리 스냅샷**: 검색을 조인 없이 처리하고, 카탈로그 이름이 바뀌어도 거래 기록이 유지된다.
- **원장 분리**: 잔액 한 칸만으로는 "왜 이 금액인지" 추적할 수 없다.
- **기분 지연 계산**: 펫 수 × 주기 스케줄러를 돌리지 않아도 된다.

## M3 · v2 추가 테이블 (프로토콜 명세서 §6)

### notification_events (M3)
| 컬럼 | 타입 | NULL | 키 | 설명 |
|---|---|---|---|---|
| id | varchar(40) | N | PK | `<epochMillis>-<seq>` (SSE `id`와 동일) |
| member_id | uuid | N | FK | idx (member_id, created_at desc) |
| type | varchar(30) | N | | AUCTION_SOLD … |
| payload | jsonb | N | | |
| read_at | timestamptz | Y | | |
- 회원당 최근 100건 / 24시간만 보관. 스케줄러로 정리.

### companion_tokens (v2)
| 컬럼 | 타입 | NULL | 키 | 설명 |
|---|---|---|---|---|
| device_id | uuid | N | PK | |
| member_id | uuid | N | FK | 활성 10개 한도 |
| token_hash | varchar(64) | N | UK | SHA-256 |
| device_name | varchar(40) | N | | |
| scope | varchar(40) | N | | `companion:read` |
| expires_at | timestamptz | N | | 90일 |
| revoked_at | timestamptz | Y | | |

## 보류(v1.1) 테이블
- ~~`evolution_stages`, `evolution_requirements`~~ → D-14로 대체: `pet_species` 속성 컬럼 + `evolution_costs`·`pet_evolutions` (M2)
- `reward_tables(type, item_id|coin, weight)` — 열매 지급 (M2, R-06)
- `home_likes`, `guestbook_entries`
