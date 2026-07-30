package com.paywith.fds.domain;

/**
 * 등급이 확정된 경로. risk_evaluations.decided_by 와 이름이 일치한다.
 *
 * <p>단축평가로 확정된 건은 총점이 0이라 등급만으로는 판정 근거를 복원할 수 없다.
 * "총점 0인데 DANGER"를 설명하려면 이 값이 필요하다.
 */
public enum DecidedBy {

    /** 블랙리스트 확정. 총점과 무관. */
    BLACKLIST,

    /** 화이트리스트 확정. 현재 송금 FDS 에는 해당 필터가 없다. */
    WHITELIST,

    RULE
}
