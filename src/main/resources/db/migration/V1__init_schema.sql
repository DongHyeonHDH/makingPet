-- V1 초기 스키마 (2026-11-17)
-- 기준: Hibernate 7.4 PostgreSQLDialect 가 엔티티에서 만드는 DDL 과 같다. 운영은 ddl-auto=validate 로 이 스키마를 검증만 한다.
-- 테스트(H2, MODE=PostgreSQL)에서도 같은 파일을 쓴다. 문법은 두 DB 공통 부분만 쓴다.
-- 이미 적용된 마이그레이션은 고치지 않는다. 변경은 V2__... 로 추가한다.

-- ===== member =====
create table members (
    id                    uuid         not null,
    email                 varchar(254) not null,
    nickname              varchar(24)  not null,
    password_hash         varchar(255) not null,
    role                  varchar(20)  not null,
    representative_pet_id uuid,
    created_at            timestamp(6) not null,
    updated_at            timestamp(6) not null,
    deleted_at            timestamp(6),
    constraint pk_members primary key (id),
    constraint uk_members_email unique (email),
    constraint uk_members_nickname unique (nickname),
    constraint ck_members_role check (role in ('USER', 'ADMIN', 'SUPER_ADMIN'))
);

-- ===== wallet =====
create table wallets (
    member_id  uuid         not null,
    balance    bigint       not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    deleted_at timestamp(6),
    constraint pk_wallets primary key (member_id),
    constraint ck_wallets_balance check (balance >= 0)
);

create table coin_transactions (
    id            uuid         not null,
    member_id     uuid         not null,
    amount        bigint       not null,
    balance_after bigint       not null,
    reason        varchar(30)  not null,
    ref_type      varchar(30),
    ref_id        uuid,
    memo          varchar(200),
    created_at    timestamp(6) not null,
    updated_at    timestamp(6) not null,
    deleted_at    timestamp(6),
    constraint pk_coin_transactions primary key (id),
    constraint ck_coin_transactions_reason check (reason in (
        'SIGNUP_GIFT', 'ATTENDANCE', 'RPS', 'SLOT_ENTRY', 'SLOT_PAYOUT', 'GACHA', 'GACHA_REFUND',
        'AFFECTION_REWARD', 'SHOP_PURCHASE', 'AUCTION_BUY', 'AUCTION_SELL', 'AUCTION_FEE', 'ADMIN_ADJUST'))
);
create index idx_coin_tx_member_created on coin_transactions (member_id, created_at);

-- ===== item =====
create table items (
    id          uuid         not null,
    code        varchar(40)  not null,
    name        varchar(40)  not null,
    category    varchar(12)  not null,
    rarity      varchar(12)  not null,
    asset_key   varchar(200) not null,
    description varchar(300),
    tradable    boolean      not null,
    active      boolean      not null,
    created_at  timestamp(6) not null,
    updated_at  timestamp(6) not null,
    deleted_at  timestamp(6),
    constraint pk_items primary key (id),
    constraint uk_items_code unique (code),
    constraint ck_items_category check (category in ('TOP', 'BOTTOM', 'ACCESSORY', 'MOTION', 'FURNITURE', 'THEME', 'FRUIT')),
    constraint ck_items_rarity check (rarity in ('COMMON', 'RARE', 'EPIC', 'LEGENDARY'))
);

create table inventory_items (
    member_id  uuid         not null,
    item_id    uuid         not null,
    quantity   integer      not null,
    reserved   integer      not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    deleted_at timestamp(6),
    constraint pk_inventory_items primary key (item_id, member_id),
    constraint ck_inventory_items_quantity check (quantity >= 0 and reserved >= 0 and reserved <= quantity)
);

-- ===== pet =====
create table pet_species (
    id             uuid         not null,
    code           varchar(40)  not null,
    name           varchar(40)  not null,
    rarity         varchar(12)  not null,
    base_asset_key varchar(200) not null,
    description    varchar(300),
    active         boolean      not null,
    created_at     timestamp(6) not null,
    updated_at     timestamp(6) not null,
    deleted_at     timestamp(6),
    constraint pk_pet_species primary key (id),
    constraint uk_pet_species_code unique (code),
    constraint ck_pet_species_rarity check (rarity in ('COMMON', 'RARE', 'EPIC', 'LEGENDARY'))
);

create table species_motions (
    species_id      uuid    not null,
    affection_level integer not null,
    motion_item_id  uuid    not null,
    constraint pk_species_motions primary key (affection_level, species_id)
);

create table user_pets (
    id                  uuid         not null,
    member_id           uuid         not null,
    species_id          uuid,
    released_species_id uuid,
    name                varchar(12)  not null,
    stage               integer      not null,
    affection           integer      not null,
    affection_level     integer      not null,
    mood                integer      not null,
    mood_updated_at     timestamp(6) not null,
    obtained_from       varchar(20)  not null,
    created_at          timestamp(6) not null,
    updated_at          timestamp(6) not null,
    deleted_at          timestamp(6),
    constraint pk_user_pets primary key (id),
    constraint uk_user_pets_member_species unique (member_id, species_id)
);
create index idx_user_pets_member on user_pets (member_id);

create table pet_equipments (
    pet_id  uuid        not null,
    slot    varchar(12) not null,
    item_id uuid        not null,
    constraint pk_pet_equipments primary key (slot, pet_id),
    constraint ck_pet_equipments_slot check (slot in ('TOP', 'BOTTOM', 'ACCESSORY_1', 'ACCESSORY_2', 'MOTION')),
    constraint fk_pet_equipments_pet foreign key (pet_id) references user_pets (id)
);

create table pet_interaction_cooldowns (
    pet_id            uuid         not null,
    type              varchar(10)  not null,
    next_available_at timestamp(6) not null,
    constraint pk_pet_interaction_cooldowns primary key (type, pet_id),
    constraint ck_pet_interaction_cooldowns_type check (type in ('PET', 'PLAY', 'SNACK')),
    constraint fk_pet_interaction_cooldowns_pet foreign key (pet_id) references user_pets (id)
);

-- ===== gacha =====
create table gacha_pools (
    id             uuid         not null,
    name           varchar(40)  not null,
    type           varchar(10)  not null,
    single_price   integer      not null,
    multi_price    integer      not null,
    pity_threshold integer      not null,
    starts_at      timestamp(6),
    ends_at        timestamp(6),
    active         boolean      not null,
    created_at     timestamp(6) not null,
    updated_at     timestamp(6) not null,
    deleted_at     timestamp(6),
    constraint pk_gacha_pools primary key (id),
    constraint ck_gacha_pools_type check (type in ('STANDARD', 'LIMITED'))
);

create table gacha_pool_entries (
    id         uuid    not null,
    pool_id    uuid    not null,
    species_id uuid    not null,
    weight     integer not null,
    constraint pk_gacha_pool_entries primary key (id),
    constraint uk_gacha_pool_entries_pool_species unique (pool_id, species_id),
    constraint fk_gacha_pool_entries_pool foreign key (pool_id) references gacha_pools (id)
);

create table gacha_pity_counters (
    member_id uuid    not null,
    pool_id   uuid    not null,
    count     integer not null,
    constraint pk_gacha_pity_counters primary key (member_id, pool_id)
);

create table gacha_draws (
    id             uuid         not null,
    member_id      uuid         not null,
    pool_id        uuid         not null,
    batch_id       uuid         not null,
    draw_order     integer      not null,
    species_id     uuid         not null,
    rarity         varchar(12)  not null,
    pity_triggered boolean      not null,
    guaranteed     boolean      not null,
    result_type    varchar(10)  not null,
    user_pet_id    uuid,
    refund_coins   integer      not null,
    cost           integer      not null,
    created_at     timestamp(6) not null,
    updated_at     timestamp(6) not null,
    deleted_at     timestamp(6),
    constraint pk_gacha_draws primary key (id),
    constraint ck_gacha_draws_rarity check (rarity in ('COMMON', 'RARE', 'EPIC', 'LEGENDARY')),
    constraint ck_gacha_draws_result_type check (result_type in ('NEW_PET', 'REFUND'))
);
create index idx_gacha_draws_member_created on gacha_draws (member_id, created_at);

-- ===== home =====
create table homes (
    member_id     uuid         not null,
    theme_item_id uuid         not null,
    visibility    varchar(10)  not null,
    like_count    integer      not null,
    created_at    timestamp(6) not null,
    updated_at    timestamp(6) not null,
    deleted_at    timestamp(6),
    constraint pk_homes primary key (member_id),
    constraint ck_homes_visibility check (visibility in ('PUBLIC', 'PRIVATE'))
);

create table home_furnitures (
    id        uuid    not null,
    member_id uuid    not null,
    item_id   uuid    not null,
    posx      integer not null,
    posy      integer not null,
    layer     integer not null,
    constraint pk_home_furnitures primary key (id)
);
create index idx_home_furnitures_member on home_furnitures (member_id);

create table home_likes (
    id        uuid not null,
    liker_id  uuid not null,
    owner_id  uuid not null,
    like_date date not null,
    constraint pk_home_likes primary key (id),
    constraint uk_home_likes_liker_owner_date unique (liker_id, owner_id, like_date)
);

-- ===== reward =====
create table reward_claims (
    id          uuid         not null,
    member_id   uuid         not null,
    type        varchar(12)  not null,
    reward_date date         not null,
    seq         integer      not null,
    streak      integer      not null,
    detail      varchar(100),
    coin_delta  integer      not null,
    created_at  timestamp(6) not null,
    updated_at  timestamp(6) not null,
    deleted_at  timestamp(6),
    constraint pk_reward_claims primary key (id),
    constraint uk_reward_claims_member_type_date_seq unique (member_id, type, reward_date, seq),
    constraint ck_reward_claims_type check (type in ('ATTENDANCE', 'RPS', 'SLOT'))
);

create table daily_counters (
    member_id    uuid        not null,
    counter_date date        not null,
    type         varchar(12) not null,
    count        integer     not null,
    constraint pk_daily_counters primary key (counter_date, type, member_id)
);

-- ===== common =====
create table idempotency_keys (
    member_id     uuid           not null,
    idem_key      varchar(64)    not null,
    request_hash  varchar(64)    not null,
    http_status   integer        not null,
    response_json varchar(65535) not null,
    created_at    timestamp(6)   not null,
    constraint pk_idempotency_keys primary key (member_id, idem_key)
);
