package com.gorapaduk.makingpet.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 멱등 키 정리, (M2) 매물 만료 등 주기 작업. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
