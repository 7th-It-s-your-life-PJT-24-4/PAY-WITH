package com.paywith.merchant.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 가맹점 조회 도메인. merchants 테이블은 시드로만 관리되는 조회 전용 데이터라
 * 결제 파트는 읽기만 한다. 좌표는 결제 실행 시 transactions에 복사되는 서버 관리
 * 값이며 클라이언트와 주고받지 않는다.
 */
@Getter
@Setter
public class Merchant {

    private Long merchantId;
    private String name;
    private String categoryCode;
    private String region;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime createdAt;
}
