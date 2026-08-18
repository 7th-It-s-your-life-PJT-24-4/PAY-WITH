package com.paywith.payment.fds.rule;

import com.paywith.payment.fds.domain.LastCompletedPayment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 결제 룰·단축평가가 참조하는 판정 입력. 수집 단계에서 한 번에 만들어지고 이후로는 DB를
 * 다시 보지 않는다. 가맹점 카테고리·좌표는 실행 경로가 이미 조회한 merchants 값을 전달받아
 * 담은 것으로, 여기서 재조회하지 않는다.
 */
@Getter
@Builder
public class PaymentRuleContext {

    private final Long amount;
    private final LocalDateTime requestedAt;

    /** merchants.category_code — NULL이면 카테고리 룰은 모두 스킵한다(데이터 미비 케이스). */
    private final String merchantCategoryCode;

    private final BigDecimal merchantLatitude;
    private final BigDecimal merchantLongitude;

    /** null이면 24시간 내 COMPLETED 결제 이력 없음 — 이동 속도 단축평가 스킵. */
    private final LastCompletedPayment lastCompletedPayment;

    /** 분할 결제 윈도 내 과거 상품권 의심 COMPLETED 결제 건수. 현재 건은 포함되지 않는다. */
    private final int giftCardSuspectRecentCount;

    /** 위험 업종 반복 윈도 내 과거 위험 업종 COMPLETED 결제 건수. 현재 건은 포함되지 않는다. */
    private final int riskyCategoryRecentCount;

    private final boolean pendingApprovalExists;

    /** 두 목록은 서로소로 관리한다 — 같은 코드가 겹치면 위험 업종 판정이 우선. */
    private final List<String> riskyCategories;
    private final List<String> giftCardCategories;
    private final long giftCardAmountUnit;

    /**
     * 상품권 의심 결제 — 상품권 취급 업종이면서 금액이 단위(5,000원)의 배수.
     * GIFT_CARD_AMOUNT와 SPLIT_PAYMENT가 같은 전제를 공유하므로 판정식을 한 곳에 둔다.
     * 목록이 겹치는 설정 오류 시 위험 업종 판정이 우선이라 risky 코드는 여기서 배제한다.
     */
    public boolean isGiftCardSuspect() {
        return merchantCategoryCode != null
            && giftCardCategories.contains(merchantCategoryCode)
            && !riskyCategories.contains(merchantCategoryCode)
            && amount % giftCardAmountUnit == 0;
    }

    /**
     * 위험 업종 결제 — 현금 교환이 쉬운 물품 판매 업종. RISKY_CATEGORY 와 RISKY_REPEATED 가
     * 같은 전제를 공유하므로 판정식을 한 곳에 둔다. category_code NULL 이면 스킵(데이터 미비).
     */
    public boolean isRiskyCategory() {
        return merchantCategoryCode != null
            && riskyCategories.contains(merchantCategoryCode);
    }
}
