package com.gorapaduk.makingpet.community.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 공지가 하나도 없을 때 환영 공지 1건(고정)을 만든다. 작성자는 비워 두고 "운영자"로 표시한다. */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class NoticeSeeder implements ApplicationRunner {

    static final String WELCOME_TITLE = "makingPet 에 오신 것을 환영합니다";

    private static final String WELCOME_CONTENT = """
            ## 처음 오셨나요?

            - 가입하면 **300코인**과 기본 방 테마를 드려요.
            - 매일 **출석**하면 30코인, 7일 연속이면 보너스 100코인!
            - 뽑기에서 오리·강아지·고양이·공룡 친구를 만나 보세요.
            - 상점에서 리본·모자·티셔츠로 펫을 꾸밀 수 있어요.

            즐거운 시간 보내세요 🐾
            """;

    private final NoticeService noticeService;

    @Override
    public void run(ApplicationArguments args) {
        if (!noticeService.isEmpty()) return;
        noticeService.seed(WELCOME_TITLE, WELCOME_CONTENT, null, true);
        log.info("공지 시드 1건 생성");
    }
}
