package com.gorapaduk.makingpet.common.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * 코인·아이템이 움직이는 POST 의 멱등 처리 (01_API_표준 §7).
 *   - 같은 (member, key) 가 24시간 안에 다시 오면 최초 응답을 그대로 돌려준다.
 *   - 같은 key 에 다른 body 면 IDEMPOTENCY_KEY_REUSED.
 * 기록은 작업과 **같은 트랜잭션**에서 저장한다. 작업이 롤백되면 기록도 사라져 재시도가 가능하다.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository records;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * @param requestBody 요청 본문(해시 대상). null 이면 빈 문자열로 본다.
     * @param action      실제 작업. 호출자 트랜잭션 안에서 실행된다.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public <T> T execute(UUID memberId, String key, Object requestBody, Class<T> responseType, Supplier<T> action) {
        if (key == null || key.isBlank()) throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        if (key.length() > 64) throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key 는 64자 이하여야 합니다.");
        String hash = sha256(requestBody == null ? "" : toJson(requestBody));

        var existing = records.findById(new IdempotencyRecord.Key(memberId, key));
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(hash)) throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
            return objectMapper.readValue(existing.get().getResponseJson(), responseType);
        }

        T result = action.get();
        try {
            records.saveAndFlush(IdempotencyRecord.of(memberId, key, hash, 200, toJson(result)));
        } catch (DataIntegrityViolationException race) {
            // 같은 키로 동시에 두 요청이 들어온 경우: 이쪽을 롤백시켜 코인이 두 번 움직이지 않게 한다.
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "같은 요청이 처리 중입니다.");
        }
        return result;
    }

    /** 매시 정각: 24시간 지난 기록 정리 */
    @Scheduled(cron = "0 0 * * * *")
    public void purge() {
        transactionTemplate.executeWithoutResult(s -> records.deleteOlderThan(LocalDateTime.now().minusHours(24)));
    }

    private String toJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    private static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
