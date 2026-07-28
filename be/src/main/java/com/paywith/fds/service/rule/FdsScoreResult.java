package com.paywith.fds.service.rule;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FdsScoreResult {

    private final int totalScore;
    private final int threshold;
    private final boolean held;
    private final List<TriggeredRule> triggeredRules;
}
