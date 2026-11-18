#!/usr/bin/env python3
"""makingPet 임시 데이터 생성기. 출력: <out>/*.sql (도메인별). 시드 고정 -> 항상 같은 결과."""
import hashlib, math, os, random, sys
from datetime import datetime, timedelta, date

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
R = random.Random(20261118)
NOW = datetime(2026, 11, 18, 9, 0, 0)
TODAY = date(2026, 11, 18)

# ---------- 공통 ----------
def uid(prefix, n):
    return f"{prefix}-0000-4000-8000-{n:012d}"
P_MEMBER, P_ITEM, P_PRODUCT, P_PURCHASE = "aa010000", "aa020000", "aa030000", "aa040000"
P_PET, P_DRAW, P_BATCH, P_CLAIM = "aa050000", "aa060000", "aa070000", "aa080000"
P_FURN, P_LIKE, P_LISTING, P_TRADE = "aa090000", "aa0a0000", "aa0b0000", "aa0c0000"
P_NOTICE, P_TX = "aa0d0000", "aa0e0000"

def q(s):
    return "null" if s is None else "'" + str(s).replace("'", "''") + "'"
def ts(d):
    return "null" if d is None else "'" + d.strftime("%Y-%m-%d %H:%M:%S") + "'"
def dt(day, h=None, m=None):
    return datetime(day.year, day.month, day.day, h if h is not None else R.randint(8, 22), m if m is not None else R.randint(0, 59), R.randint(0, 59))
def b(v):
    return "true" if v else "false"

def pbkdf2_hex(pw):
    salt = bytes(R.randrange(256) for _ in range(16))
    return (salt + hashlib.pbkdf2_hmac("sha256", pw.encode(), salt, 310000, 32)).hex()

def header(title, extra=""):
    return (f"-- {title}\n-- 임시 데이터(temp_data). 검토용이며 운영 데이터가 아니다. 정리는 00_cleanup.sql.\n{extra}\n")

files = {}
def put(name, text):
    files[name] = text

# ---------- 회원 ----------
N = 50
members = []
for i in range(1, N + 1):
    created = dt(date(2026, 11, 1) + timedelta(days=(i - 1) * 11 // N), 9 + i % 10, R.randint(0, 59))
    members.append(dict(n=i, id=uid(P_MEMBER, i), email=f"tmp{i:02d}@temp.test", nick=f"임시유저{i:02d}",
                        role="ADMIN" if i == 50 else "USER", created=created, rep=None))
M = {m["n"]: m for m in members}
PW = pbkdf2_hex("Test1234!")

# ---------- 아이템 / 상점 ----------
ITEMS = [  # code, name, category, rarity, desc
    ("tmp_top_stripe", "줄무늬 티", "TOP", "COMMON", "가장 무난한 줄무늬"), ("tmp_top_hoodie", "후드티", "TOP", "COMMON", "모자가 있어서 든든하다"),
    ("tmp_top_raincoat", "노란 우비", "TOP", "COMMON", "비 오는 날 필수"), ("tmp_top_apron", "요리사 앞치마", "TOP", "COMMON", "입으면 요리가 늘 것 같다"),
    ("tmp_top_knit", "털 스웨터", "TOP", "RARE", "겨울엔 이거"), ("tmp_top_hanbok", "꼬마 한복", "TOP", "RARE", "명절 한정 느낌"),
    ("tmp_top_kimono", "유카타", "TOP", "RARE", "여름 축제용"), ("tmp_top_space", "우주복", "TOP", "EPIC", "달에서도 따뜻하다"),
    ("tmp_top_armor", "반짝 갑옷", "TOP", "EPIC", "아무도 못 이긴다"),
    ("tmp_bottom_jeans", "청바지", "BOTTOM", "COMMON", "튼튼하다"), ("tmp_bottom_shorts", "반바지", "BOTTOM", "COMMON", "시원하다"),
    ("tmp_bottom_skirt", "체크 치마", "BOTTOM", "COMMON", "살랑살랑"), ("tmp_bottom_pj", "잠옷 바지", "BOTTOM", "COMMON", "낮잠 전용"),
    ("tmp_bottom_overall", "멜빵바지", "BOTTOM", "RARE", "농장 감성"), ("tmp_bottom_cargo", "카고 바지", "BOTTOM", "RARE", "주머니가 많다"),
    ("tmp_bottom_snow", "눈꽃 레깅스", "BOTTOM", "RARE", "반짝이는 눈꽃 무늬"), ("tmp_bottom_tutu", "튀튀 치마", "BOTTOM", "EPIC", "발레리나 펫"),
    ("tmp_acc_glasses", "동그란 안경", "ACCESSORY", "COMMON", "똑똑해 보인다"), ("tmp_acc_scarf", "빨간 목도리", "ACCESSORY", "COMMON", "목이 따뜻하다"),
    ("tmp_acc_bow", "나비넥타이", "ACCESSORY", "COMMON", "격식 있는 자리용"), ("tmp_acc_cap", "야구 모자", "ACCESSORY", "COMMON", "챙이 살짝 휘었다"),
    ("tmp_acc_headphone", "헤드폰", "ACCESSORY", "RARE", "음악이 들리는 척"), ("tmp_acc_flower", "꽃 머리핀", "ACCESSORY", "RARE", "봄 내음"),
    ("tmp_acc_eyepatch", "해적 안대", "ACCESSORY", "RARE", "보물을 찾는 중"), ("tmp_acc_crown", "반짝 왕관", "ACCESSORY", "EPIC", "왕은 아니고 펫이다"),
    ("tmp_acc_wings", "천사 날개", "ACCESSORY", "EPIC", "날지는 못한다"), ("tmp_acc_halo", "달빛 후광", "ACCESSORY", "LEGENDARY", "은은하게 빛난다"),
    ("tmp_furn_sofa", "말랑 소파", "FURNITURE", "COMMON", "앉으면 일어나기 싫다"), ("tmp_furn_plant", "화분", "FURNITURE", "COMMON", "물은 가끔만"),
    ("tmp_furn_lamp", "별 조명", "FURNITURE", "COMMON", "밤에 켜면 예쁘다"), ("tmp_furn_shelf", "작은 책장", "FURNITURE", "COMMON", "책은 한 권"),
    ("tmp_furn_bed", "펫 침대", "FURNITURE", "RARE", "펫이 제일 좋아하는 자리"), ("tmp_furn_tv", "꼬마 TV", "FURNITURE", "RARE", "채널은 하나"),
    ("tmp_furn_piano", "장난감 피아노", "FURNITURE", "RARE", "도레미만 친다"), ("tmp_furn_clock", "뻐꾸기 시계", "FURNITURE", "RARE", "정각마다 운다"),
    ("tmp_furn_fish", "어항", "FURNITURE", "EPIC", "물고기가 펫을 구경한다"), ("tmp_furn_tent", "캠핑 텐트", "FURNITURE", "EPIC", "방 안에서 캠핑"),
    ("tmp_theme_forest", "숲속 방", "THEME", "RARE", "나뭇잎 소리가 난다"), ("tmp_theme_beach", "해변 오두막", "THEME", "RARE", "파도 소리 포함"),
    ("tmp_theme_space", "우주 정거장", "THEME", "EPIC", "창밖이 전부 별"), ("tmp_theme_night", "별밤 지붕", "THEME", "LEGENDARY", "지붕이 없다"),
    ("fruit_fire", "불꽃 열매", "FRUIT", "COMMON", "먹으면 속이 따뜻해진다"), ("fruit_water", "물방울 열매", "FRUIT", "COMMON", "한입 베면 촉촉하다"),
    ("fruit_grass", "풀잎 열매", "FRUIT", "COMMON", "상큼한 풀 향기"), ("fruit_electric", "번개 열매", "FRUIT", "COMMON", "먹으면 찌릿하다"),
    ("tmp_motion_nap", "낮잠", "MOTION", "COMMON", "꾸벅꾸벅"), ("tmp_motion_spin", "빙글빙글", "MOTION", "COMMON", "제자리에서 돈다"),
    ("tmp_motion_wave", "손 흔들기", "MOTION", "COMMON", "안녕!"), ("tmp_motion_dance", "춤추기", "MOTION", "RARE", "리듬을 탄다"),
    ("tmp_motion_jump", "폴짝", "MOTION", "RARE", "신나면 뛴다"),
]
assert len(ITEMS) == 50
PRICE = {"COMMON": (40, 100), "RARE": (120, 200), "EPIC": (300, 480), "LEGENDARY": (900, 1500)}
items = []
cnt_rar = {}
for k, (code, name, cat, rar, desc) in enumerate(ITEMS, 1):
    lo, hi = PRICE[rar]
    cnt_rar[rar] = cnt_rar.get(rar, 0) + 1
    price = lo + ((cnt_rar[rar] * 7) % (hi - lo + 1)) // 10 * 10 if rar == "COMMON" else lo + ((cnt_rar[rar] * 37) % (hi - lo + 1)) // 10 * 10
    price = max(lo, min(hi, price))
    items.append(dict(n=k, id=uid(P_ITEM, k), code=code, name=name, cat=cat, rar=rar, desc=desc, price=price,
                      created=datetime(2026, 11, 10, 10, k % 60, 0)))
I = {it["code"]: it for it in items}
by_id = {it["id"]: it for it in items}
POOL = [it for it in items if it["rar"] == "COMMON" and it["price"] <= 100 and it["cat"] != "MOTION"]
assert len(POOL) >= 20, len(POOL)

# ---------- 전역 이벤트 (원장) ----------
events = []  # dict(ts, member, amount|None, reason, ref_type, ref_id, memo, fn)
def ev(t, member, amount, reason, ref_type=None, ref_id=None, memo=None, fn=None):
    events.append(dict(ts=t, member=member, amount=amount, reason=reason, ref_type=ref_type, ref_id=ref_id, memo=memo, fn=fn))

for m in members:
    ev(m["created"] + timedelta(seconds=1), m["n"], 300, "SIGNUP_GIFT", None, None, "가입 선물")
RICH = list(range(1, 11))
for n in RICH:
    ev(datetime(2026, 11, 11, 12, n, 0), n, 3000, "ADMIN_ADJUST", None, None, "임시 데이터: 테스트용 지급")

# ---------- 보상(reward_claims) 50건 ----------
claims = []
cid = [0]
def claim(member, typ, d, seq, streak, detail, coins, hour, minute=None):
    cid[0] += 1
    c = dict(id=uid(P_CLAIM, cid[0]), member=member, typ=typ, d=d, seq=seq, streak=streak, detail=detail, coins=coins, ts=dt(d, hour, minute))
    claims.append(c)
    ev(c["ts"], member, coins, "ATTENDANCE" if typ == "ATTENDANCE" else "RPS", "REWARD_CLAIM", c["id"])
    return c
for n in (1, 2, 3):                       # 7일 연속(7일째 보너스)
    for k in range(7):
        d = date(2026, 11, 11) + timedelta(days=k)
        claim(n, "ATTENDANCE", d, 0, k + 1, None, 30 + (100 if k == 6 else 0), 9)
for n in (4, 5):                          # 3일 연속
    for k in range(3):
        claim(n, "ATTENDANCE", date(2026, 11, 15) + timedelta(days=k), 0, k + 1, None, 30, 9)
for n in range(6, 19):                    # 단발 출석 13건
    claim(n, "ATTENDANCE", date(2026, 11, R.randint(max(M[n]["created"].day, 12), 17)), 0, 1, None, 30, 10)
RPS_COINS = {"WIN": 50, "DRAW": 20, "LOSE": 10}
rps_days = {}
for n in range(19, 29):                   # 가위바위보 10건
    r = R.choice(["WIN", "WIN", "DRAW", "LOSE"])
    d = date(2026, 11, R.randint(M[n]["created"].day, 17))
    claim(n, "RPS", d, 1, 0, r, RPS_COINS[r], 20)
    rps_days[(n, d)] = 1
assert len(claims) == 50, len(claims)

# ---------- 상점 구매 50건 ----------
purchases = []
def purchase(member, item, qty, when):
    pid = uid(P_PURCHASE, len(purchases) + 1)
    total = item["price"] * qty
    purchases.append(dict(id=pid, member=member, item=item, qty=qty, unit=item["price"], total=total, ts=when))
    ev(when, member, -total, "SHOP_PURCHASE", "SHOP_PURCHASE", pid)
for n in RICH:                            # 부자 10명 x 3건 = 30
    for k in range(3):
        purchase(n, POOL[(n * 3 + k * 5) % len(POOL)], R.randint(2, 5), dt(date(2026, 11, 12 + k), 11 + k, R.randint(0, 59)))
for n in range(11, 31):                   # 20건
    purchase(n, POOL[(n * 7) % len(POOL)], 1, dt(date(2026, 11, 13), R.randint(12, 20)))
assert len(purchases) == 50

# ---------- 가챠 50건 + 펫 ----------
SPECIES = {"duck": ("COMMON", 3000), "dog": ("COMMON", 3000), "cat": ("RARE", 2800), "dinosaur": ("EPIC", 1200)}
REFUND = {"COMMON": 20, "RARE": 50, "EPIC": 150, "LEGENDARY": 500}
codes = list(SPECIES); weights = [SPECIES[c][1] for c in codes]
draws, pets = [], []
first_species = {}
def make_pet(member, sp, obtained, when):
    pets.append(dict(id=uid(P_PET, len(pets) + 1), member=member, sp=sp, obtained=obtained, ts=when))
    return pets[-1]
for n in range(1, 48):
    sp = R.choices(codes, weights)[0]
    first_species[n] = sp
    when = dt(date(2026, 11, R.randint(max(M[n]["created"].day, 13), 16)), R.randint(10, 22))
    pet = make_pet(n, sp, "GACHA", when)
    draws.append(dict(member=n, sp=sp, rar=SPECIES[sp][0], type="NEW_PET", pet=pet, refund=0, ts=when))
for n in (1, 2, 3):                       # 중복 -> 환급
    sp = first_species[n]
    when = dt(date(2026, 11, 16), R.randint(10, 22))
    draws.append(dict(member=n, sp=sp, rar=SPECIES[sp][0], type="REFUND", pet=None, refund=REFUND[SPECIES[sp][0]], ts=when))
assert len(draws) == 50
for n, sp in zip((48, 49, 50), ("duck", "dog", "cat")):      # 선물 펫 3마리 -> 총 50
    make_pet(n, sp, "EVENT", datetime(2026, 11, 16, 15, n % 60, 0))
assert len(pets) == 50
for k, d in enumerate(draws, 1):
    d["id"], d["batch"] = uid(P_DRAW, k), uid(P_BATCH, k)
    ev(d["ts"], d["member"], -100, "GACHA", "GACHA_BATCH", d["batch"])
    if d["type"] == "REFUND":
        ev(d["ts"] + timedelta(seconds=1), d["member"], d["refund"], "GACHA_REFUND", "GACHA_BATCH", d["batch"])

NAMES = ["뭉치", "보리", "콩이", "두부", "만두", "호두", "모찌", "나비", "초코", "감자", "밤톨", "구름", "별이", "하늘", "솜이"]
LEVEL_T = [0, 30, 100, 250, 500]
def lvl(a):
    return max(i + 1 for i, t in enumerate(LEVEL_T) if a >= t)
for p in pets:
    p["name"] = NAMES[(int(p["id"][-12:]) - 1) % len(NAMES)]
    p["aff"] = R.choice([0, 5, 12, 28, 35, 60, 99, 120, 180, 260, 320, 480, 510])
    p["lvl"] = lvl(p["aff"])
    p["mood"] = R.randint(40, 100)
    for L, coin in ((3, 50), (5, 200)):       # 레벨 보상 (AffectionLevel.COIN_REWARDS)
        if p["lvl"] >= L:
            ev(p["ts"] + timedelta(hours=R.randint(5, 40)), p["member"], coin, "AFFECTION_REWARD", "USER_PET", p["id"], f"친밀도 레벨 {L} 보상")
pet_of = {}
for p in pets:
    pet_of.setdefault(p["member"], p)       # 대표 펫 = 첫 펫
for n, p in pet_of.items():
    M[n]["rep"] = p["id"]

# ---------- 인벤토리 / 장착 / 가구 (최종 상태) ----------
inv = {}   # (member, item_id) -> [quantity, reserved]
def addinv(n, item_id, q_):
    inv.setdefault((n, item_id), [0, 0])[0] += q_
theme_basic = "THEME_BASIC"               # 시드 아이템은 서브쿼리로 참조
for p in purchases:
    addinv(p["member"], p["item"]["id"], p["qty"])
equips, furns = [], []
SLOT = {"TOP": ["TOP"], "BOTTOM": ["BOTTOM"], "ACCESSORY": ["ACCESSORY_1", "ACCESSORY_2"]}
used_slots = {}
for (n, item_id), qv in sorted(inv.items()):
    it = by_id[item_id]
    if it["cat"] in SLOT and n in pet_of:
        for s in SLOT[it["cat"]]:
            if (n, s) not in used_slots:
                used_slots[(n, s)] = 1
                equips.append(dict(pet=pet_of[n]["id"], slot=s, item=it))
                qv[1] += 1
                break
    elif it["cat"] == "FURNITURE" and sum(1 for f in furns if f["member"] == n) < 3:
        furns.append(dict(id=uid(P_FURN, len(furns) + 1), member=n, item=it, x=R.randint(40, 560), y=R.randint(220, 380), layer=len(furns) % 3))
        qv[1] += 1

# ---------- 경매 50건 ----------
LIST_STATUS = ["ON_SALE"] * 20 + ["SOLD"] * 15 + ["CANCELLED"] * 5 + ["EXPIRED"] * 5 + ["REMOVED"] * 5
R.shuffle(LIST_STATUS)
listings, trades = [], []
for k, status in enumerate(LIST_STATUS, 1):
    seller = RICH[(k - 1) % len(RICH)]
    cands = [(key, v) for key, v in sorted(inv.items()) if key[0] == seller and by_id[key[1]]["cat"] != "THEME" and v[0] - v[1] > 0]
    if not cands:
        cands = [(key, v) for key, v in sorted(inv.items()) if key[0] == seller and v[0] - v[1] > 0]
    key, v = cands[(k * 3) % len(cands)]
    it = by_id[key[1]]
    avail = v[0] - v[1]
    qty = min(avail, R.randint(2, 10)) if it["cat"] == "FRUIT" else 1
    unit = max(10, int(it["price"] * R.choice([0.8, 1.0, 1.2, 1.5]) // 10 * 10))
    price = min(unit * qty, 99999)
    created = dt(date(2026, 11, R.randint(12, 17)))
    if status in ("ON_SALE",):
        exp = NOW + timedelta(days=R.randint(1, 6), hours=R.randint(0, 20))
    elif status == "EXPIRED":
        exp = created + timedelta(hours=R.randint(24, 72)); exp = min(exp, NOW - timedelta(hours=2))
    else:
        exp = created + timedelta(days=7)
    L = dict(id=uid(P_LISTING, k), seller=seller, item=it, qty=qty, price=price, unit=round(price / qty + 1e-9, 1), status=status,
             created=created, exp=exp, closed=None, key=key)
    if status == "ON_SALE":
        v[1] += qty                           # 에스크로
    elif status == "SOLD":
        L["closed"] = created + timedelta(hours=R.randint(1, 20))
        L["closed"] = min(L["closed"], NOW - timedelta(hours=1))
        v[0] -= qty                           # 판매자 수량 감소 (구매자 증가는 아래 이벤트에서)
        fee = max(1, math.ceil(price * 0.05))
        tid = uid(P_TRADE, len(trades) + 1)
        T = dict(id=tid, listing=L, qty=qty, price=price, fee=fee, income=price - fee, ts=L["closed"], buyer=None)
        trades.append(T)
        def mk(T=T):
            def fn(bal):
                for cand in RICH:
                    if cand != T["listing"]["seller"] and bal.get(cand, 0) >= T["price"]:
                        T["buyer"] = cand
                        return cand
                raise RuntimeError("buyer shortage for " + T["id"])
            return fn
        ev(L["closed"], None, -price, "AUCTION_BUY", "AUCTION_LISTING", L["id"], None, mk())
        ev(L["closed"] + timedelta(seconds=1), seller, price, "AUCTION_SELL", "AUCTION_LISTING", L["id"])
        ev(L["closed"] + timedelta(seconds=1), seller, -fee, "AUCTION_FEE", "AUCTION_LISTING", L["id"])
    elif status in ("CANCELLED", "REMOVED", "EXPIRED"):
        L["closed"] = exp if status == "EXPIRED" else min(created + timedelta(hours=R.randint(2, 30)), NOW - timedelta(hours=1))
    listings.append(L)

# ---------- 원장 시뮬레이션 ----------
events.sort(key=lambda e: e["ts"])
bal, ledger = {}, []
last_ts = None
for e in events:
    t = e["ts"]
    if last_ts is not None and t <= last_ts:
        t = last_ts + timedelta(seconds=1)        # 같은 초 방지 (정렬 안정성)
    last_ts = t
    member = e["member"] if e["member"] is not None else e["fn"](bal)
    nb = bal.get(member, 0) + e["amount"]
    if nb < 0:
        raise RuntimeError(f"balance<0 member={member} ts={t} {e['reason']}")
    bal[member] = nb
    ledger.append(dict(id=uid(P_TX, len(ledger) + 1), member=member, amount=e["amount"], after=nb, reason=e["reason"],
                       ref_type=e["ref_type"], ref_id=e["ref_id"], memo=e["memo"], ts=t))
for T in trades:                              # 구매자 인벤토리 반영
    addinv(T["buyer"], T["listing"]["item"]["id"], T["qty"])

# ---------- 홈 / 좋아요 ----------
likes = []
for n in range(1, 51):
    for off in (1, 2) if n % 5 else (1, 2, 3):
        owner = (n + off - 1) % 50 + 1
        likes.append(dict(id=uid(P_LIKE, len(likes) + 1), liker=n, owner=owner, d=date(2026, 11, 14 + (n + off) % 4)))
like_cnt = {}
for l in likes:
    like_cnt[l["owner"]] = like_cnt.get(l["owner"], 0) + 1

# ---------- 공지 50건 ----------
NOTICE_T = ["점검 안내", "업데이트 소식", "이벤트 안내", "뽑기 확률 안내", "경매장 이용 안내", "출석 보상 안내", "상점 신상품", "버그 수정 안내", "커뮤니티 규칙", "펫 돌보기 팁"]
notices = []
for k in range(1, 51):
    pub = datetime(2026, 11, 1, 9, 0, 0) + timedelta(hours=(k - 1) * 8)
    if k > 48:
        pub = NOW + timedelta(days=k - 48)       # 예약 게시 2건
    notices.append(dict(id=uid(P_NOTICE, k), title=f"[임시] {NOTICE_T[(k - 1) % 10]} #{k}",
                        content=f"임시 공지 {k}번 본문입니다. 화면 표시와 페이징을 확인하기 위한 더미 텍스트입니다.\n두 번째 줄입니다.",
                        pinned=k in (1, 11, 21), pub=pub, author=M[50]["id"] if k % 5 == 0 else None))

# =====================================================================
# 파일 쓰기
# =====================================================================
def ins(table, cols, rows, chunk=25):
    out = []
    for i in range(0, len(rows), chunk):
        out.append(f"insert into {table} ({', '.join(cols)}) values\n" + ",\n".join("  (" + r + ")" for r in rows[i:i + chunk]) + ";")
    return "\n".join(out) + "\n"

# 01 member
rows = [f"{q(m['id'])}, {q(m['email'])}, {q(m['nick'])}, {q(PW)}, {q(m['role'])}, null, {ts(m['created'])}, {ts(m['created'])}, null" for m in members]
t = header("01 회원 50명 (비밀번호는 모두 Test1234!)", "-- tmp01~tmp49 = USER, tmp50 = ADMIN. 이메일은 tmpNN@temp.test")
t += ins("members", ["id", "email", "nickname", "password_hash", "role", "representative_pet_id", "created_at", "updated_at", "deleted_at"], rows)
put("01_member.sql", t)

# 02 item
rows = [f"{q(i['id'])}, {q(i['code'])}, {q(i['name'])}, {q(i['cat'])}, {q(i['rar'])}, {q('items/' + i['code'] + '.png')}, {q(i['desc'])}, true, true, {ts(i['created'])}, {ts(i['created'])}, null" for i in items]
t = header("02 아이템 50종", "-- 카테고리: TOP 9, BOTTOM 8, ACCESSORY 10, FURNITURE 10, THEME 4, FRUIT 4(D-14 열매), MOTION 5")
t += ins("items", ["id", "code", "name", "category", "rarity", "asset_key", "description", "tradable", "active", "created_at", "updated_at", "deleted_at"], rows)
put("02_item.sql", t)

# 03 shop
rows = []
for k, i in enumerate(items, 1):
    s_start = "null"; s_end = "null"
    if k in (5, 25, 40):
        s_start, s_end = ts(datetime(2026, 11, 15)), ts(datetime(2026, 11, 25))
    rows.append(f"{q(uid(P_PRODUCT, k))}, {q(i['id'])}, {i['price']}, {s_start}, {s_end}, {b(k % 17 != 0)}, {k * 10}, {ts(i['created'])}, {ts(i['created'])}, null")
t = header("03 상점 상품 50건 + 구매 50건", "-- 상품 #17·#34 는 visible=false. #5·#25·#40 은 판매 기간(11/15~11/25) 지정.")
t += ins("shop_products", ["id", "item_id", "price", "sale_starts_at", "sale_ends_at", "visible", "sort_order", "created_at", "updated_at", "deleted_at"], rows)
prod_id = {it["id"]: uid(P_PRODUCT, it["n"]) for it in items}
rows = [f"{q(p['id'])}, {q(M[p['member']]['id'])}, {q(prod_id[p['item']['id']])}, {q(p['item']['id'])}, {p['qty']}, {p['unit']}, {p['total']}, {ts(p['ts'])}, {ts(p['ts'])}, null" for p in purchases]
t += ins("shop_purchases", ["id", "member_id", "product_id", "item_id", "quantity", "unit_price", "total_price", "created_at", "updated_at", "deleted_at"], rows)
put("03_shop.sql", t)

# 04 pet
sp = lambda c: f"(select id from pet_species where code = '{c}')"
rows = [f"{q(p['id'])}, {q(M[p['member']]['id'])}, {sp(p['sp'])}, null, {q(p['name'])}, 1, {p['aff']}, {p['lvl']}, {p['mood']}, {ts(datetime(2026, 11, 18, 8, 0, 0))}, {q(p['obtained'])}, {ts(p['ts'])}, {ts(p['ts'])}, null" for p in pets]
t = header("04 펫 50마리 + 장착", "-- 선행 조건: 앱을 한 번 실행해 PetSpeciesSeeder 가 duck/dog/cat/dinosaur 를 만든 상태.\n-- obtained_from: GACHA 47, EVENT 3(tmp48~50 선물 펫). 대표 펫 = 회원별 첫 펫.")
t += ins("user_pets", ["id", "member_id", "species_id", "released_species_id", "name", "stage", "affection", "affection_level", "mood", "mood_updated_at", "obtained_from", "created_at", "updated_at", "deleted_at"], rows)
t += "\n-- 대표 펫 지정\n" + "\n".join(f"update members set representative_pet_id = {q(M[n]['rep'])} where id = {q(M[n]['id'])};" for n in sorted(pet_of)) + "\n"
t += "\n" + ins("pet_equipments", ["pet_id", "slot", "item_id"], [f"{q(e['pet'])}, {q(e['slot'])}, {q(e['item']['id'])}" for e in equips])
put("04_pet.sql", t)

# 05 gacha
pool = "(select id from gacha_pools where name = '상시 뽑기')"
rows = [f"{q(d['id'])}, {q(M[d['member']]['id'])}, {pool}, {q(d['batch'])}, 1, {sp(d['sp'])}, {q(d['rar'])}, false, false, {q(d['type'])}, {q(d['pet']['id']) if d['pet'] else 'null'}, {d['refund']}, 100, {ts(d['ts'])}, {ts(d['ts'])}, null" for d in draws]
t = header("05 뽑기 기록 50건 (단일 뽑기 100코인)", "-- 선행 조건: GachaPoolSeeder 가 '상시 뽑기' 풀을 만든 상태.\n-- tmp01~47 첫 뽑기 NEW_PET 47건 + tmp01~03 두 번째 뽑기 중복 → REFUND 3건.\n-- 현재 코드(고양이 RARE, 공룡 EPIC) 기준. D-13 반영 후엔 등급 컬럼을 다시 생성해야 한다.")
t += ins("gacha_draws", ["id", "member_id", "pool_id", "batch_id", "draw_order", "species_id", "rarity", "pity_triggered", "guaranteed", "result_type", "user_pet_id", "refund_coins", "cost", "created_at", "updated_at", "deleted_at"], rows)
counter = {}
for d in draws:
    counter[d["member"]] = 0 if d["rar"] == "EPIC" else counter.get(d["member"], 0) + 1
t += "\n" + ins("gacha_pity_counters", ["member_id", "pool_id", "count"], [f"{q(M[n]['id'])}, {pool}, {c}" for n, c in sorted(counter.items())])
put("05_gacha.sql", t)

# 06 reward
rows = [f"{q(c['id'])}, {q(M[c['member']]['id'])}, {q(c['typ'])}, {q(c['d'].isoformat())}, {c['seq']}, {c['streak']}, {q(c['detail'])}, {c['coins']}, {ts(c['ts'])}, {ts(c['ts'])}, null" for c in claims]
t = header("06 보상 기록 50건", "-- 출석: tmp01~03 7일 연속(7일째 +100 보너스), tmp04~05 3일 연속, tmp06~18 단발. 가위바위보: tmp19~28.")
t += ins("reward_claims", ["id", "member_id", "type", "reward_date", "seq", "streak", "detail", "coin_delta", "created_at", "updated_at", "deleted_at"], rows)
t += "\n" + ins("daily_counters", ["member_id", "counter_date", "type", "count"], [f"{q(M[n]['id'])}, {q(d.isoformat())}, 'RPS', 1" for (n, d) in sorted(rps_days)])
put("06_reward.sql", t)

# 07 home
rows = [f"{q(m['id'])}, (select id from items where code = 'theme_basic'), {q('PRIVATE' if m['n'] % 5 == 0 else 'PUBLIC')}, {like_cnt.get(m['n'], 0)}, {ts(m['created'])}, {ts(m['created'])}, null" for m in members]
t = header("07 홈 50개 + 가구 + 좋아요", "-- 선행 조건: ItemCatalogSeeder 의 theme_basic. 비공개는 5의 배수 회원. like_count = home_likes 행 수.")
t += ins("homes", ["member_id", "theme_item_id", "visibility", "like_count", "created_at", "updated_at", "deleted_at"], rows)
t += "\n" + ins("home_furnitures", ["id", "member_id", "item_id", "posx", "posy", "layer"], [f"{q(f['id'])}, {q(M[f['member']]['id'])}, {q(f['item']['id'])}, {f['x']}, {f['y']}, {f['layer']}" for f in furns])
t += "\n" + ins("home_likes", ["id", "liker_id", "owner_id", "like_date"], [f"{q(l['id'])}, {q(M[l['liker']]['id'])}, {q(M[l['owner']]['id'])}, {q(l['d'].isoformat())}" for l in likes])
put("07_home.sql", t)

# 08 auction (스키마 초안 포함)
ddl = """-- ⚠ 경매 테이블은 아직 Flyway 에 없다(V1·V2 에 없음). 아래는 03_erd/01_ERD.md 기준의 임시 초안이다.
--   * 확정 스키마(V3)가 생기면 이 DDL 은 지우고, 컬럼이 다르면 insert 를 맞춘다.
--   * ERD 의 timestamptz 는 H2 호환을 위해 timestamp(6) 으로 적었다.
create table if not exists auction_listings (
    id            uuid          not null,
    seller_id     uuid          not null,
    item_id       uuid          not null,
    item_name     varchar(40)   not null,
    item_category varchar(12)   not null,
    quantity      integer       not null,
    price         integer       not null,
    unit_price    numeric(7,1)  not null,
    status        varchar(10)   not null,
    expires_at    timestamp(6)  not null,
    closed_at     timestamp(6),
    created_at    timestamp(6)  not null,
    updated_at    timestamp(6)  not null,
    deleted_at    timestamp(6),
    constraint pk_auction_listings primary key (id),
    constraint ck_auction_listings_status check (status in ('ON_SALE', 'SOLD', 'CANCELLED', 'EXPIRED', 'REMOVED')),
    constraint ck_auction_listings_quantity check (quantity between 1 and 10),
    constraint ck_auction_listings_price check (price between 1 and 99999)
);
create table if not exists auction_trades (
    id            uuid          not null,
    listing_id    uuid          not null,
    buyer_id      uuid          not null,
    seller_id     uuid          not null,
    item_id       uuid          not null,
    quantity      integer       not null,
    price         integer       not null,
    fee           integer       not null,
    seller_income integer       not null,
    unit_price    numeric(7,1)  not null,
    created_at    timestamp(6)  not null,
    updated_at    timestamp(6)  not null,
    deleted_at    timestamp(6),
    constraint pk_auction_trades primary key (id),
    constraint uk_auction_trades_listing unique (listing_id)
);
create table if not exists auction_favorites (
    member_id  uuid         not null,
    listing_id uuid         not null,
    created_at timestamp(6) not null,
    constraint pk_auction_favorites primary key (member_id, listing_id)
);
"""
t = header("08 경매 매물 50건 + 거래 15건 + 즐겨찾기", "-- 매물 상태: ON_SALE 20, SOLD 15, CANCELLED 5, EXPIRED 5, REMOVED 5. 열매(FRUIT)만 2~10개 묶음.\n-- 수수료 5%(올림, 최소 1). 판매자 원장: AUCTION_SELL +판매가, AUCTION_FEE -수수료.\n") + ddl + "\n"
rows = [f"{q(l['id'])}, {q(M[l['seller']]['id'])}, {q(l['item']['id'])}, {q(l['item']['name'])}, {q(l['item']['cat'])}, {l['qty']}, {l['price']}, {l['unit']:.1f}, {q(l['status'])}, {ts(l['exp'])}, {ts(l['closed'])}, {ts(l['created'])}, {ts(l['closed'] or l['created'])}, null" for l in listings]
t += ins("auction_listings", ["id", "seller_id", "item_id", "item_name", "item_category", "quantity", "price", "unit_price", "status", "expires_at", "closed_at", "created_at", "updated_at", "deleted_at"], rows)
rows = [f"{q(T['id'])}, {q(T['listing']['id'])}, {q(M[T['buyer']]['id'])}, {q(M[T['listing']['seller']]['id'])}, {q(T['listing']['item']['id'])}, {T['qty']}, {T['price']}, {T['fee']}, {T['income']}, {T['listing']['unit']:.1f}, {ts(T['ts'])}, {ts(T['ts'])}, null" for T in trades]
t += "\n" + ins("auction_trades", ["id", "listing_id", "buyer_id", "seller_id", "item_id", "quantity", "price", "fee", "seller_income", "unit_price", "created_at", "updated_at", "deleted_at"], rows)
fav = []
on_sale = [l for l in listings if l["status"] == "ON_SALE"]
for k, l in enumerate(on_sale):
    for n in (11 + k, 31 + k):
        fav.append(f"{q(M[n]['id'])}, {q(l['id'])}, {ts(l['created'] + timedelta(hours=3))}")
t += "\n" + ins("auction_favorites", ["member_id", "listing_id", "created_at"], fav)
put("08_auction.sql", t)

# 09 notice
rows = [f"{q(n['id'])}, {q(n['title'])}, {q(n['content'])}, {q(n['author'])}, {b(n['pinned'])}, {ts(n['pub'])}, {ts(n['pub'])}, {ts(n['pub'])}, null" for n in notices]
t = header("09 공지 50건", "-- 고정 3건(#1, #11, #21), 예약 게시 2건(#49, #50 은 published_at 이 미래), 작성자는 5의 배수만 tmp50(ADMIN).")
t += ins("notices", ["id", "title", "content", "author_id", "pinned", "published_at", "created_at", "updated_at", "deleted_at"], rows)
put("09_notice.sql", t)

# 10 wallet
rows = [f"{q(M[n]['id'])}, {bal[n]}, {ts(M[n]['created'])}, {ts(NOW)}, null" for n in sorted(bal)]
t = header("10 지갑 50개 + 코인 원장", f"-- 원장 {len(ledger)}건. 지갑 잔액 = 원장 합계 (시뮬레이션으로 계산). 가입 선물 300, 출석·가위바위보, 상점, 뽑기·환급, 친밀도 보상, 경매 매매·수수료, tmp01~10 에 ADMIN_ADJUST +3000.")
t += ins("wallets", ["member_id", "balance", "created_at", "updated_at", "deleted_at"], rows)
rows = [f"{q(l['id'])}, {q(M[l['member']]['id'])}, {l['amount']}, {l['after']}, {q(l['reason'])}, {q(l['ref_type'])}, {q(l['ref_id'])}, {q(l['memo'])}, {ts(l['ts'])}, {ts(l['ts'])}, null" for l in ledger]
t += "\n" + ins("coin_transactions", ["id", "member_id", "amount", "balance_after", "reason", "ref_type", "ref_id", "memo", "created_at", "updated_at", "deleted_at"], rows)
put("10_wallet.sql", t)

# 11 inventory (최종 상태; 테마 기본 아이템 포함)
rows = [f"{q(M[n]['id'])}, (select id from items where code = 'theme_basic'), 1, 1, {ts(M[n]['created'])}, {ts(M[n]['created'])}, null" for n in range(1, 51)]
t = header("11 인벤토리 (마지막에 적용)", "-- 모든 회원이 theme_basic 1개(홈 테마로 예약 1)를 가진다.\n-- 그 외: 상점 구매 + 경매 거래 반영. reserved = 장착 + 가구 배치 + 에스크로(판매 중).")
t += ins("inventory_items", ["member_id", "item_id", "quantity", "reserved", "created_at", "updated_at", "deleted_at"], rows)
rows = [f"{q(M[n]['id'])}, {q(iid)}, {v[0]}, {v[1]}, {ts(NOW)}, {ts(NOW)}, null" for (n, iid), v in sorted(inv.items()) if v[0] > 0]
t += "\n" + ins("inventory_items", ["member_id", "item_id", "quantity", "reserved", "created_at", "updated_at", "deleted_at"], rows)
put("11_inventory.sql", t)

# 00 cleanup
cl = """-- 임시 데이터 정리 (temp_data 로 넣은 행만 지운다). 순서 중요. PostgreSQL · H2 공통.
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
"""
put("00_cleanup.sql", cl)

for name, text in files.items():
    with open(os.path.join(OUT, name), "w", encoding="utf-8", newline="\n") as f:
        f.write(text)

# 통계 출력
print("ledger", len(ledger), "total balance", sum(bal.values()))
print("equips", len(equips), "furn", len(furns), "likes", len(likes), "trades", len(trades), "fav", len(fav))
print("species", {c: sum(1 for p in pets if p["sp"] == c) for c in codes})
