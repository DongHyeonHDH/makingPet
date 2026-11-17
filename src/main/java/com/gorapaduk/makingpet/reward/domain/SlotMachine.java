package com.gorapaduk.makingpet.reward.domain;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 규칙 §5-3. 결과 등급을 먼저 확률로 뽑고, 그 등급에 맞는 심볼 배치를 만든다.
 * 기대 배당 ≈ 24 코인/회 (참가비 20).
 */
public final class SlotMachine {

    public static final int ENTRY_FEE = 20;
    public static final int DAILY_LIMIT = 5;

    public enum Symbol { DUCK, CAT, DOG, DINO, STRAWBERRY, STAR }

    private static final List<Symbol> PETS = List.of(Symbol.DUCK, Symbol.CAT, Symbol.DOG, Symbol.DINO);

    @Getter
    @RequiredArgsConstructor
    public enum Tier {
        JACKPOT(100, 500),            // 1%
        TRIPLE_PET(400, 150),         // 4%
        TRIPLE_STRAWBERRY(500, 80),   // 5%
        PAIR(3000, 30),               // 30%
        NONE(6000, 0);                // 60%

        private final int weight;     // 만분율
        private final int payout;
    }

    public record Spin(List<Symbol> reels, Tier tier) {}

    private final SecureRandom random = new SecureRandom();

    public Spin spin() {
        Tier tier = rollTier();
        return new Spin(reelsFor(tier), tier);
    }

    private Tier rollTier() {
        int roll = random.nextInt(10_000);
        int acc = 0;
        for (Tier t : Tier.values()) {
            acc += t.getWeight();
            if (roll < acc) return t;
        }
        return Tier.NONE;
    }

    private List<Symbol> reelsFor(Tier tier) {
        return switch (tier) {
            case JACKPOT -> List.of(Symbol.STAR, Symbol.STAR, Symbol.STAR);
            case TRIPLE_STRAWBERRY -> List.of(Symbol.STRAWBERRY, Symbol.STRAWBERRY, Symbol.STRAWBERRY);
            case TRIPLE_PET -> { Symbol p = pick(PETS); yield List.of(p, p, p); }
            case PAIR -> {
                Symbol a = pick(List.of(Symbol.values()));
                Symbol b = pickOther(a);
                List<Symbol> r = new ArrayList<>(List.of(a, a, b));
                Collections.shuffle(r, random);
                yield List.copyOf(r);
            }
            case NONE -> {
                Symbol a = pick(List.of(Symbol.values()));
                Symbol b = pickOther(a);
                Symbol c = pickOther(a, b);
                yield List.of(a, b, c);
            }
        };
    }

    private Symbol pick(List<Symbol> from) { return from.get(random.nextInt(from.size())); }

    private Symbol pickOther(Symbol... not) {
        List<Symbol> pool = new ArrayList<>(List.of(Symbol.values()));
        pool.removeAll(List.of(not));
        return pick(pool);
    }
}
