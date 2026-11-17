package com.gorapaduk.makingpet.common.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.constants.SuccessCode;

/**
 * 모든 API 응답 래퍼 (기획/04_api/01_API_표준.md §1).
 * 성공: { success: true,  message, data }
 * 실패: { success: false, message, error: { code, message, fields? } }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, ErrorBody error) {

    private static final String FAIL_MESSAGE = "해당 요청이 실패되었습니다.";

    public static <T> ApiResponse<T> ok(T data, SuccessCode code) {
        return new ApiResponse<>(true, code.getMessage(), data, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static ApiResponse<Void> ok(SuccessCode code) {
        return ok(null, code);
    }

    /** 201 응답도 본문 형식은 같다. 상태 코드는 컨트롤러의 @ResponseStatus 로 준다. */
    public static <T> ApiResponse<T> created(T data, SuccessCode code) {
        return ok(data, code);
    }

    /** 결과에 따라 메시지가 달라지는 경우 (출석 코인 수, 가위바위보 결과 등) */
    public static <T> ApiResponse<T> created(T data, String message) {
        return ok(data, message);
    }

    public static ApiResponse<Void> fail(ErrorCode code) {
        return fail(code, code.getDescription());
    }

    public static ApiResponse<Void> fail(ErrorCode code, String message) {
        return new ApiResponse<>(false, FAIL_MESSAGE, null, new ErrorBody(code.name(), message, null));
    }

    public static ApiResponse<Void> failWithFields(ErrorCode code, List<FieldError> fields) {
        return new ApiResponse<>(false, FAIL_MESSAGE, null,
                new ErrorBody(code.name(), code.getDescription(), fields));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(String code, String message, List<FieldError> fields) {}

    public record FieldError(String field, String reason) {}
}
