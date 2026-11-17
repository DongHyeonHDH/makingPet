package com.gorapaduk.makingpet.member.domain;

import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 기획/03_erd/01_ERD.md `members`. representative_pet_id 는 pet 도메인 엔티티를 import 하지 않고 ID 만 보관한다. */
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, unique = true, length = 24)
    private String nickname;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    private UUID representativePetId;

    public static Member create(String email, String nickname, String passwordHash) {
        Member member = new Member();
        member.id = UUID.randomUUID();
        member.email = email;
        member.nickname = nickname;
        member.passwordHash = passwordHash;
        member.role = Role.USER;
        return member;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void changeRepresentativePet(UUID petId) {
        this.representativePetId = petId;
    }

    /** 탈퇴: 이메일·닉네임을 치환해 재가입을 허용하고 소프트 딜리트. */
    public void withdraw() {
        String suffix = id.toString();
        this.email = "deleted_" + suffix;
        this.nickname = "deleted_" + suffix.substring(0, 8);
        softDelete();
    }
}
