package com.paywith.fds.domain;

/**
 * 위험 등급이 확정된 경로. risk_evaluations.decided_by 와 이름이 일치한다.
 *
 * <p>단축평가로 확정된 건은 총점과 등급이 무관하므로, 등급만 저장해서는 판정 근거를 복원할 수 없다.
 * 이 값이 있어야 "총점 0인데 DANGER" 같은 결과를 설명할 수 있다.
 */
public enum DecidedBy {

    /** 블랙리스트 단축평가로 DANGER 확정. 총점과 무관. */
    BLACKLIST,

    /** 화이트리스트 단축평가로 SAFE 확정. 총점과 무관. */
    WHITELIST,

    /** 룰 점수 합산 결과로 등급 결정. */
    RULE
}
