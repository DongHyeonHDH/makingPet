package com.gorapaduk.makingpet.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(@NotBlank @Size(max = 24) String nickname) {
}
