package com.gorapaduk.makingpet.pet.domain;

/** 규칙 §2-3 친밀도 단계표. 수치 조정은 여기서만. */
public final class AffectionLevel {

    private AffectionLevel() {}

    /** 단계 → 필요 누적 친밀도 (index 0 = 1단계) */
    private static final int[] THRESHOLDS = {0, 30, 100, 250, 500};
    /** 단계 → 도달 시 코인 보상 */
    private static final int[] COIN_REWARDS = {0, 0, 50, 0, 200};

    public static final int MAX = THRESHOLDS.length;

    public static int levelFor(int affection) {
        int level = 1;
        for (int i = 1; i < THRESHOLDS.length; i++) {
            if (affection >= THRESHOLDS[i]) level = i + 1;
        }
        return level;
    }

    /** 다음 단계 필요 누적치. 최고 단계면 null */
    public static Integer nextThreshold(int level) {
        return level >= MAX ? null : THRESHOLDS[level];
    }

    public static int coinReward(int level) {
        return COIN_REWARDS[level - 1];
    }
}
