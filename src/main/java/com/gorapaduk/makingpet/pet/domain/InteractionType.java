package com.gorapaduk.makingpet.pet.domain;

import java.time.Duration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 규칙 §2-1. 쿨다운은 펫별·종류별. */
@Getter
@RequiredArgsConstructor
public enum InteractionType {
    PET(Duration.ofMinutes(1), 5, 1, "motion.pet"),
    PLAY(Duration.ofMinutes(10), 15, 3, "motion.play"),
    SNACK(Duration.ofHours(1), 25, 5, "motion.eat");

    private final Duration cooldown;
    private final int moodDelta;
    private final int affectionDelta;
    private final String defaultMotionKey;
}
