package com.paywith.fds.service.rule;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TriggeredRule {

    private final Long ruleId;
    private final int score;
}
