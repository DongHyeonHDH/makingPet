package com.gorapaduk.makingpet.member.repository;

import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByEmailAndDeletedAtIsNull(String email);

    Optional<Member> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Member> findByNicknameAndDeletedAtIsNull(String nickname);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
