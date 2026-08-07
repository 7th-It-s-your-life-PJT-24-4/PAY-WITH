package com.paywith.fds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paywith.fds.domain.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("위험 등급 판정")
class RiskGraderTest {

    private static final int CAUTION = 25;
    private static final int DANGER = 50;

    private final RiskGrader grader = new RiskGrader(CAUTION, DANGER);

    @Test
    void gradesZeroAsSafe() {
        assertThat(grader.grade(0)).isEqualTo(RiskLevel.SAFE);
    }

    @Test
    void gradesJustBelowCautionAsSafe() {
        assertThat(grader.grade(CAUTION - 1)).isEqualTo(RiskLevel.SAFE);
    }

    @Test
    void gradesCautionBoundaryAsCaution() {
        assertThat(grader.grade(CAUTION)).isEqualTo(RiskLevel.CAUTION);
    }

    @Test
    void gradesJustBelowDangerAsCaution() {
        assertThat(grader.grade(DANGER - 1)).isEqualTo(RiskLevel.CAUTION);
    }

    @Test
    void gradesDangerBoundaryAsDanger() {
        assertThat(grader.grade(DANGER)).isEqualTo(RiskLevel.DANGER);
    }

    // 감점 룰 때문에 합산 결과가 음수가 될 수 있다. 그대로 저장하면 경계값과 견줄 수 없는 값이 된다
    @Test
    void normalizeScore_clampsNegativeToZero() {
        assertThat(grader.normalizeScore(-20)).isZero();
    }

    @Test
    void normalizeScore_keepsPositiveAsIs() {
        assertThat(grader.normalizeScore(37)).isEqualTo(37);
    }

    // 룰이 여럿 겹치면 합산이 상한을 넘는다(현재 배점 최대 151)
    @Test
    void normalizeScore_clampsAboveMaxToMax() {
        assertThat(grader.normalizeScore(151)).isEqualTo(RiskGrader.MAX_SCORE);
    }

    @Test
    void normalizeScore_keepsMaxAsIs() {
        assertThat(grader.normalizeScore(RiskGrader.MAX_SCORE)).isEqualTo(RiskGrader.MAX_SCORE);
    }

    // 상한을 잘라도 등급 판정은 그대로여야 한다
    @Test
    void gradesClampedScoreAsDanger() {
        assertThat(grader.grade(grader.normalizeScore(151))).isEqualTo(RiskLevel.DANGER);
    }

    @Test
    void constructor_rejectsCautionAboveDanger() {
        assertThatThrownBy(() -> new RiskGrader(60, 50))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fds.threshold.caution");
    }

    // 상한을 넘는 임계값은 클램프 때문에 도달 불가라 기동을 막는다
    @Test
    void constructor_rejectsDangerAboveMaxScore() {
        assertThatThrownBy(() -> new RiskGrader(25, RiskGrader.MAX_SCORE + 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fds.threshold.danger");
    }
}
