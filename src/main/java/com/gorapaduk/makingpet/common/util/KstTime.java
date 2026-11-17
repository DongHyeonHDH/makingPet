package com.gorapaduk.makingpet.common.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** "하루"의 기준은 Asia/Seoul 00:00 (규칙 §0-3). 일일 한도·출석은 전부 이 클래스를 통해 날짜를 구한다. */
public final class KstTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private KstTime() {}

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /** 다음 KST 자정 */
    public static ZonedDateTime nextMidnight() {
        return today().plusDays(1).atStartOfDay(ZONE);
    }

    public static long secondsUntilMidnight() {
        return Math.max(1, Duration.between(ZonedDateTime.now(ZONE), nextMidnight()).toSeconds());
    }
}
