# temp_data — MVP 임시 데이터 (검토용)

도메인마다 대표 테이블 50건씩을 `.sql`로 만든 임시 데이터입니다. 운영 데이터가 아니고, 검토한 뒤 지우거나 고쳐 쓰는 용도입니다.

## 파일과 건수

| 파일 | 도메인 | 대표 테이블(50건) | 함께 들어가는 행 |
|---|---|---|---|
| `01_member.sql` | 회원 | members 50 | tmp50만 ADMIN |
| `02_item.sql` | 아이템 | items 50 | 카테고리 7종(열매 4종 포함) |
| `03_shop.sql` | 상점 | shop_products 50 | shop_purchases 50 |
| `04_pet.sql` | 펫 | user_pets 50 | pet_equipments 30, 대표 펫 지정 |
| `05_gacha.sql` | 뽑기 | gacha_draws 50 | gacha_pity_counters 47 |
| `06_reward.sql` | 보상 | reward_claims 50 | daily_counters 10 |
| `07_home.sql` | 홈 | homes 50 | home_furnitures 11, home_likes 110 |
| `08_auction.sql` | 경매 | auction_listings 50 | auction_trades 15, auction_favorites 40, **임시 DDL** |
| `09_notice.sql` | 공지 | notices 50 | 고정 3, 예약 게시 2 |
| `10_wallet.sql` | 지갑 | wallets 50 | coin_transactions 284 (활동에서 파생) |
| `11_inventory.sql` | 인벤토리 | inventory_items 110 | 마지막에 적용 |
| `00_cleanup.sql` | 정리 | — | 임시 행만 삭제 |
| `_generate_temp_data.py` | 생성기 | — | 시드 고정, 다시 돌리면 같은 결과 |

## 사용 순서

1. 앱을 한 번 실행해 시더를 돌립니다. `pet_species`(duck/dog/cat/dinosaur), `'상시 뽑기'` 풀, `theme_basic` 아이템이 있어야 합니다. 이 값들은 서브쿼리로 참조합니다.
2. `01` → `02` → `03` → `04` → `05` → `06` → `07` → `08` → `09` → `10` → `11` 순서로 적용합니다. 번호 순서가 곧 적용 순서입니다. `04`의 장착 행만 FK(`user_pets`)가 있습니다.
3. 다시 넣으려면 `00_cleanup.sql`을 먼저 실행합니다.

```bash
# PostgreSQL 예시
for f in temp_data/0[1-9]_*.sql temp_data/1[01]_*.sql; do psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f "$f"; done
```

## 약속된 값

- 로그인: `tmp01@temp.test` ~ `tmp50@temp.test`, 비밀번호 `Test1234!` (`tmp50`은 ADMIN).
- UUID는 도메인별 접두사로 고정했습니다 (`aa01…` 회원, `aa02…` 아이템, `aa03…` 상품 …).
- 기준 시각은 2026-11-18 09:00입니다. 시간이 지나면 ON_SALE 매물의 `expires_at`이 지날 수 있습니다.

## 데이터가 만드는 상황

- 출석: tmp01~03은 11/11~11/17 7일 연속(7일째 +100), tmp04~05는 3일 연속. 오늘 출석하면 이어집니다.
- 뽑기: tmp01~47 첫 뽑기 47건, tmp01~03 두 번째 뽑기는 같은 종 → 환급(REFUND) 3건. tmp48~50 펫은 `EVENT` 선물입니다.
- 경매: ON_SALE 20 / SOLD 15 / CANCELLED 5 / EXPIRED 5 / REMOVED 5. 열매만 2~10개 묶음입니다. 판매자는 tmp01~10입니다.
- 부자 계정: tmp01~10에 `ADMIN_ADJUST` +3000이 들어 있어 경매 구매자 역할을 합니다.

## 정합성 (생성 후 직접 검사한 항목)

지갑 잔액 = 원장 합계, `balance_after` 연쇄, `reserved` = 장착 + 가구 + 홈 테마 + ON_SALE 에스크로, `like_count` = 좋아요 행 수, 친밀도 레벨 매핑, 환급 합계 = 원장, 수수료·판매 대금, 중복 펫 없음, 자기 거래·자기 좋아요 없음.

## 주의 (사용 전에 알아둘 것)

1. **경매 테이블은 Flyway에 아직 없습니다.** `08_auction.sql` 위쪽에 ERD 기준 임시 DDL(`create table if not exists`)을 넣었습니다. V3가 확정되면 DDL을 지우고 컬럼을 맞추세요.
2. **뽑기 데이터는 현재 코드 기준입니다** (고양이 RARE, 공룡 EPIC, 천장 카운터 존재). D-13 코드 반영 후에는 `05_gacha.sql`의 등급·환급·천장 값을 다시 생성해야 합니다.
3. **열매 4종(`fruit_*`)과 진화 관련 컬럼은 현재 스키마에 없습니다.** 열매는 아이템으로만 넣었습니다.
4. 비밀번호 해시는 `Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()` 형식(솔트 16B + SHA-256 310000회, hex)으로 직접 계산했습니다. 이 환경에서 Spring으로 대조하지는 못했으니, 처음에 `tmp01` 로그인으로 확인하세요.
5. 경매 수수료 원장은 판매자 `AUCTION_SELL +판매가`, `AUCTION_FEE −수수료`로 기록했습니다. API 명세 §92는 `credit(price−fee)`와 `AUCTION_FEE −fee`를 함께 적어 이중 차감처럼 읽힙니다. 구현 때 한쪽으로 정해야 합니다.
6. 쿨다운(`pet_interaction_cooldowns`), 멱등 키, 슬롯 기록은 시간·요청에 묶인 상태라 넣지 않았습니다.
