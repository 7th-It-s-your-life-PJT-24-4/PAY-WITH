package com.paywith.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "모든 API 의 공통 응답 래퍼. 성공이면 data 에 페이로드가 실리고 message 는 null, "
    + "실패면 data 는 null(일부 오류는 부가 정보)이고 message 에 사유가 실린다")
public class ApiResponse<T> {

    @ApiModelProperty(value = "처리 성공 여부. HTTP 2xx 이면 true, 오류 응답이면 false", example = "true")
    private final boolean success;
    @ApiModelProperty(value = "응답 페이로드. 각 API 의 응답 모델이 여기에 실린다. 실패 시 null(일부 오류는 부가 정보 객체, 예: PHONE_002 의 retryAfter)")
    private final T data;
    @ApiModelProperty(value = "실패 사유 메시지. 성공 응답에서는 항상 null", example = "null")
    private final String message;

    @ApiModelProperty(value = "오류 코드(예: REQUEST_001, AUTH_001). 실패 응답 중 코드가 부여된 경우에만 존재하며, "
        + "성공 응답과 코드 없는 오류(500 등)에서는 키 자체가 생략된다", example = "REQUEST_001")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String code;

    private ApiResponse(boolean success, T data, String message, String code) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.code = code;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, message, code);
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(false, data, message, code);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }
}
