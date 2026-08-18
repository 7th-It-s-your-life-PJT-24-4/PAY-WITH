package com.paywith.fds.service.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.mapper.RiskRuleMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("위험 룰 캐시")
class RiskRuleCacheTest {

    private static final String NIGHT_LATE = "NIGHT_TIME_LATE";
    private static final String NIGHT_DEEP = "NIGHT_TIME_DEEP";
    private static final String UNKNOWN_CODE = "NOT_A_RULE";

    @Mock
    private RiskRuleMapper riskRuleMapper;

    // 생성자가 refresh() 를 부르므로 stub 을 먼저 걸고 나서 객체를 만들어야 한다.
    // @InjectMocks 를 쓰면 stub 이전에 생성돼 캐시가 빈 채로 초기화된다.
    //
    // 반환 목록은 MyBatis 가 주는 것과 같이 수정 가능한 ArrayList 여야 한다. List.of 로 두면 그
    // 자체가 불변이라, 캐시가 unmodifiableList 로 감싸는지를 검증할 수 없다.
    private RiskRuleCache cacheWith(RiskRule... rules) {
        given(riskRuleMapper.findAllActive()).willReturn(new ArrayList<>(List.of(rules)));
        return new RiskRuleCache(riskRuleMapper);
    }

    private RiskRule rule(long id, String code, int score) {
        RiskRule rule = new RiskRule();
        rule.setRuleId(id);
        rule.setRuleCode(code);
        rule.setScore(score);
        rule.setActive(true);
        return rule;
    }

    @Test
    void constructor_loadsActiveRulesOnCreation() {
        // Given - 테스트를 시작하기 전 필요한 환경, 변수, 객체 상태를 설정
        RiskRule nightLate = rule(1L, NIGHT_LATE, 6);
        // When - 테스트하고자 하는 행동이나 특정 이벤트를 발생
        RiskRuleCache cache = cacheWith(nightLate);
        // Then - 실행 결과로 나온 값이 예상과 일치하는지 확인
        assertThat(cache.getActiveRules()).containsExactly(nightLate);
    }

    // 조회 때마다 DB 를 다시 보면 캐시를 둔 의미가 없다. 판정 한 건마다 룰 테이블을 때리게 된다.
    @Test
    void getActiveRules_doesNotRequeryMapper() {
        RiskRuleCache cache = cacheWith(rule(1L, NIGHT_LATE, 6));

        cache.getActiveRules();
        cache.getActiveRules();
        cache.findByCode(NIGHT_LATE);

        then(riskRuleMapper).should(times(1)).findAllActive();
    }

    @Test
    void constructor_handlesEmptyRuleList() {
        RiskRuleCache cache = cacheWith();

        assertThat(cache.getActiveRules()).isEmpty();
    }

    @Test
    void findByCode_returnsRuleForRegisteredCode() {
        RiskRule nightLate = rule(1L, NIGHT_LATE, 6);

        RiskRuleCache cache = cacheWith(nightLate);

        assertThat(cache.findByCode(NIGHT_LATE)).contains(nightLate);
    }

    @Test
    void findByCode_returnsEmptyForUnknownCode() {
        RiskRuleCache cache = cacheWith(rule(1L, NIGHT_LATE, 6));

        assertThat(cache.findByCode(UNKNOWN_CODE)).isEmpty();
    }

    @Test
    void refresh_replacesRulesWithLatest() {
        RiskRule oldRule = rule(1L, NIGHT_LATE, 6);
        RiskRule newRule = rule(2L, NIGHT_DEEP, 14);
        RiskRuleCache cache = cacheWith(oldRule);
        given(riskRuleMapper.findAllActive()).willReturn(List.of(newRule));

        cache.refresh();

        assertThat(cache.getActiveRules()).containsExactly(newRule);
    }

    @Test
    void getActiveRules_returnsUnmodifiableList() {
        RiskRuleCache cache = cacheWith(rule(1L, NIGHT_LATE, 6));

        List<RiskRule> rules = cache.getActiveRules();

        assertThatThrownBy(() -> rules.add(rule(2L, NIGHT_DEEP, 14)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
