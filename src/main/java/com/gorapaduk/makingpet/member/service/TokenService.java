package com.gorapaduk.makingpet.member.service;

import java.time.Instant;

import com.gorapaduk.makingpet.common.config.SecurityConfig;
import com.gorapaduk.makingpet.member.domain.Member;
import com.gorapaduk.makingpet.member.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Access Token 발급. Refresh 토큰(M2)은 여기에 추가한다. */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;

    @Value("${makingpet.jwt.access-seconds:1800}")
    private long accessSeconds;

    public TokenResponse issue(Member member) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(SecurityConfig.ISSUER)
                .subject(member.getId().toString())
                .claim(SecurityConfig.ROLE_CLAIM, member.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessSeconds))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return TokenResponse.accessOnly(token, accessSeconds);
    }
}
