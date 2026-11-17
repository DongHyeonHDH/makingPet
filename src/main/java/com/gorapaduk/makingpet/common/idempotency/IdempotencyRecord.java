package com.gorapaduk.makingpet.common.idempotency;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 01_API_표준 §7. (member, key) 당 최초 응답을 24시간 보관한다. */
@Entity
@Table(name = "idempotency_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord {

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Key implements Serializable {
        @Column(name = "member_id", nullable = false)
        private UUID memberId;
        @Column(name = "idem_key", nullable = false, length = 64)
        private String key;
    }

    @EmbeddedId
    private Key id;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Column(nullable = false, length = 65535)
    private String responseJson;

    @Column(nullable = false)
    private int httpStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static IdempotencyRecord of(UUID memberId, String key, String requestHash, int httpStatus, String responseJson) {
        IdempotencyRecord r = new IdempotencyRecord();
        r.id = new Key(memberId, key);
        r.requestHash = requestHash;
        r.httpStatus = httpStatus;
        r.responseJson = responseJson;
        r.createdAt = LocalDateTime.now();
        return r;
    }
}
