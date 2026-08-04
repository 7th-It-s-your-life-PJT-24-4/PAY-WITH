package com.paywith.guard.service;

import com.paywith.guard.dto.GuardHomeResponse;

public interface GuardHomeService {

    /**
     * 보호자 홈 화면. wardId를 생략하면 연동된 첫 번째 피보호자를 선택한다.
     *
     * @param wardId null이면 첫 번째 피보호자, 값이 있으면 담당 여부를 확인 후 그 피보호자
     */
    GuardHomeResponse getHome(Long guardId, Long wardId);
}