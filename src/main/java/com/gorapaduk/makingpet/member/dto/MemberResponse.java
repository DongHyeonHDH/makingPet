package com.gorapaduk.makingpet.member.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gorapaduk.makingpet.member.domain.Member;
import com.gorapaduk.makingpet.member.domain.Role;
import com.gorapaduk.makingpet.pet.dto.PetSummary;
import com.gorapaduk.makingpet.reward.dto.RewardDtos;
import com.gorapaduk.makingpet.wallet.dto.WalletResponse;

/**
 * M-1 / M-5 / M-6 응답.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberResponse(
        UUID id,
        String email,
        String nickname,
        Role role,
        WalletResponse wallet,
        PetSummary representativePet,
        RewardDtos.Today today,
        Integer petCount,
        LocalDateTime createdAt) {

    public static MemberResponse from(Member member, long balance, PetSummary representativePet,
                                      RewardDtos.Today today, int petCount) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getNickname(), member.getRole(),
                new WalletResponse(balance), representativePet, today, petCount, member.getCreatedAt());
    }
}
