package com.paywith.exception;

import org.springframework.http.HttpStatus;

// 외부 입금(deposit) 호출 이후("돌아올 수 없는 지점" 통과 후) 발생한 실패.
// 자동 재시도를 허용하면 이중 입금으로 이어질 수 있어 별도 타입으로 구분한다.
public class TransferIrrecoverableException extends BusinessException {
    public TransferIrrecoverableException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
