package com.paywith.exception;

import org.springframework.http.HttpStatus;

//외부 출금(withdraw) 호출 이후 "돌아올 수 없는 시점"에서 발생한 실패
public class ChargeIrrecoverableException extends BusinessException{
    public ChargeIrrecoverableException(String message){
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
