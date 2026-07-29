package com.paywith.fds.service.prefilter;

import static com.paywith.fds.support.RuleContexts.normal;
import static org.assertj.core.api.Assertions.assertThat;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.service.prefilter.impl.RejectedRecipientPreFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 단축평가는 확정적으로 위험한 경우만 둔다. 애매한 신호는 점수 룰이 담당한다.
 */
@DisplayName("단축평가 필터")
class FdsPreFilterTest {

    @Nested
    @DisplayName("보호자 거절 이력 계좌")
    class RejectedRecipient {

        private final RejectedRecipientPreFilter filter = new RejectedRecipientPreFilter();

        @Test
        void decidesDangerWhenRecipientWasRejectedBefore() {
            assertThat(filter.apply(normal().recipientRejectedBefore(true).build()))
                .contains(RiskLevel.DANGER);
            assertThat(filter.getDecidedBy()).isEqualTo(DecidedBy.BLACKLIST);
        }

        @Test
        void passesThroughWhenNoRejectionHistory() {
            assertThat(filter.apply(normal().build())).isEmpty();
        }
    }
}
