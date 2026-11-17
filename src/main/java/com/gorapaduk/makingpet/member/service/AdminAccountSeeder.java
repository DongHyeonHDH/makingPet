package com.gorapaduk.makingpet.member.service;

import com.gorapaduk.makingpet.member.domain.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 관리자(SUPER_ADMIN) 계정 시드 (마일스톤 W7).
 * makingpet.admin.password 가 비어 있으면 만들지 않는다 → 비밀번호가 코드·저장소에 남지 않는다.
 * 로컬에서 쓰려면 MAKINGPET_ADMIN_PASSWORD 환경변수를 주고 기동한다. 이미 같은 이메일이 있으면 건너뛴다(권한도 건드리지 않음).
 * 아이템 카탈로그(기본 테마) 시드 뒤에 실행되어야 한다.
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class AdminAccountSeeder implements ApplicationRunner {

    private final MemberService memberService;

    @Value("${makingpet.admin.email:admin@makingpet.local}")
    private String email;

    @Value("${makingpet.admin.nickname:운영자}")
    private String nickname;

    @Value("${makingpet.admin.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            log.info("관리자 계정 시드 건너뜀: makingpet.admin.password(MAKINGPET_ADMIN_PASSWORD) 미설정");
            return;
        }
        if (password.length() < 8) {
            log.warn("관리자 계정 시드 건너뜀: 비밀번호는 8자 이상이어야 합니다.");
            return;
        }
        if (memberService.existsByEmail(email)) return;
        memberService.createPrivileged(email, nickname, password, Role.SUPER_ADMIN);
        log.info("관리자 계정 시드 생성: {} ({})", email, Role.SUPER_ADMIN);
    }
}
