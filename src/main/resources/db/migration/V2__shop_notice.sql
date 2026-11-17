-- V2 상점·공지 (2026-11-18, W7)
-- 문법은 PostgreSQL · H2(MODE=PostgreSQL) 공통 부분만 쓴다.

-- ===== shop =====
create table shop_products (
    id             uuid         not null,
    item_id        uuid         not null,
    price          integer      not null,
    sale_starts_at timestamp(6),
    sale_ends_at   timestamp(6),
    visible        boolean      not null,
    sort_order     integer      not null,
    created_at     timestamp(6) not null,
    updated_at     timestamp(6) not null,
    deleted_at     timestamp(6),
    constraint pk_shop_products primary key (id),
    constraint ck_shop_products_price check (price between 1 and 99999)
);
create index idx_shop_products_visible_sort on shop_products (visible, sort_order);

-- 구매 기록 (S-3 응답의 purchaseId, 원장 coin_transactions.ref_id 가 가리키는 행)
create table shop_purchases (
    id          uuid         not null,
    member_id   uuid         not null,
    product_id  uuid         not null,
    item_id     uuid         not null,
    quantity    integer      not null,
    unit_price  integer      not null,
    total_price integer      not null,
    created_at  timestamp(6) not null,
    updated_at  timestamp(6) not null,
    deleted_at  timestamp(6),
    constraint pk_shop_purchases primary key (id),
    constraint ck_shop_purchases_quantity check (quantity between 1 and 10)
);
create index idx_shop_purchases_member_created on shop_purchases (member_id, created_at);

-- ===== community =====
create table notices (
    id           uuid           not null,
    title        varchar(100)   not null,
    content      varchar(20000) not null,
    author_id    uuid,
    pinned       boolean        not null,
    published_at timestamp(6)   not null,
    created_at   timestamp(6)   not null,
    updated_at   timestamp(6)   not null,
    deleted_at   timestamp(6),
    constraint pk_notices primary key (id)
);
create index idx_notices_pinned_published on notices (pinned, published_at);
