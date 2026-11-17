package com.gorapaduk.makingpet.member.domain;

/** 권한 위계: SUPER_ADMIN > ADMIN > USER (SecurityConfig.roleHierarchy). */
public enum Role {
    USER, ADMIN, SUPER_ADMIN
}
