-- 임시 데이터 정리 (temp_data 로 넣은 행만 지운다). 순서 중요. PostgreSQL · H2 공통.
-- 임시 회원 = email like 'tmp%@temp.test' / 임시 아이템·상품 = id 가 'aa02…', 'aa03…' 로 시작.
delete from auction_favorites where member_id in (select id from members where email like 'tmp%@temp.test');
delete from auction_trades    where seller_id in (select id from members where email like 'tmp%@temp.test');
delete from auction_listings  where seller_id in (select id from members where email like 'tmp%@temp.test');
delete from pet_equipments    where pet_id in (select id from user_pets where member_id in (select id from members where email like 'tmp%@temp.test'));
delete from pet_interaction_cooldowns where pet_id in (select id from user_pets where member_id in (select id from members where email like 'tmp%@temp.test'));
delete from gacha_draws          where member_id in (select id from members where email like 'tmp%@temp.test');
delete from gacha_pity_counters  where member_id in (select id from members where email like 'tmp%@temp.test');
delete from user_pets            where member_id in (select id from members where email like 'tmp%@temp.test');
delete from home_likes           where liker_id  in (select id from members where email like 'tmp%@temp.test');
delete from home_furnitures      where member_id in (select id from members where email like 'tmp%@temp.test');
delete from homes                where member_id in (select id from members where email like 'tmp%@temp.test');
delete from reward_claims        where member_id in (select id from members where email like 'tmp%@temp.test');
delete from daily_counters       where member_id in (select id from members where email like 'tmp%@temp.test');
delete from shop_purchases       where member_id in (select id from members where email like 'tmp%@temp.test');
delete from inventory_items      where member_id in (select id from members where email like 'tmp%@temp.test');
delete from coin_transactions    where member_id in (select id from members where email like 'tmp%@temp.test');
delete from idempotency_keys     where member_id in (select id from members where email like 'tmp%@temp.test');
delete from wallets              where member_id in (select id from members where email like 'tmp%@temp.test');
delete from notices              where title like '[임시]%';
delete from shop_products        where cast(id as varchar(36)) like 'aa03%';
delete from items                where cast(id as varchar(36)) like 'aa02%';
delete from members              where email like 'tmp%@temp.test';
-- auction_* 테이블을 임시 DDL 로 만들었다면, 확정 스키마(V3)와 충돌하니 필요 시 직접 drop 한다:
-- drop table auction_favorites; drop table auction_trades; drop table auction_listings;
