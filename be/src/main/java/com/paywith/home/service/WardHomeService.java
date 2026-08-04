package com.paywith.home.service;

import com.paywith.home.dto.WardHomeResponse;

public interface WardHomeService {

    /**
     * 피보호자 홈 화면 정보를 모아 반환한다.
     *
     * @param userId 인증된 사용자 ID. 대상은 항상 본인이다
     */
    WardHomeResponse findMyHome(Long userId);
}
