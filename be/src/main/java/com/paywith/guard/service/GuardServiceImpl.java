package com.paywith.guard.service;

import com.paywith.guard.mapper.GuardSeniorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuardServiceImpl implements GuardService{

    private final GuardSeniorMapper guardSeniorMapper;

    @Override
    public boolean verifyGuardOfWard(Long guardId, Long wardId) {
        return guardSeniorMapper.existsActiveRelation(guardId, wardId);
    }
}
