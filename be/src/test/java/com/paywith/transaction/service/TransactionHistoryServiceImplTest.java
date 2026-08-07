package com.paywith.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.paywith.exception.BusinessException;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.guard.service.GuardService;
import com.paywith.transaction.domain.TransactionCategory;
import com.paywith.transaction.domain.TransactionStatus;
import com.paywith.transaction.domain.TransactionType;
import com.paywith.transaction.dto.GuardTransactionDetailResponse;
import com.paywith.transaction.dto.GuardTransactionHistoryItem;
import com.paywith.transaction.dto.GuardTransactionHistoryListResponse;
import com.paywith.transaction.dto.TransactionDetailResponse;
import com.paywith.transaction.dto.TransactionHistoryItem;
import com.paywith.transaction.dto.TransactionHistoryListResponse;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.wallet.dto.WalletBalanceResponse;
import com.paywith.wallet.service.WalletService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TransactionHistoryServiceImplTest {

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private WalletService walletService;

    @Mock
    private GuardService guardService;

    @InjectMocks
    private TransactionHistoryServiceImpl transactionHistoryService;

    private final Long userId = 1L;
    private final Long guardId = 2L;
    private final Long wardId = 1L;
    private final Long transactionId = 500L;

    @BeforeEach
    void setUp() {
        lenient().when(walletService.findMyBalance(userId)).thenReturn(mock(WalletBalanceResponse.class));
        lenient().when(guardService.verifyGuardOfWard(guardId, wardId)).thenReturn(true);
    }

    @Test
    void 지갑이_없으면_WalletService의_예외를_그대로_전파한다() {
        given(walletService.findMyBalance(userId))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "지갑을 찾을 수 없습니다."));

        assertThatThrownBy(() -> transactionHistoryService.findMyTransactions(userId, null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("지갑을 찾을 수 없습니다.");

        verify(transactionMapper, never()).findMyTransactions(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void category가_허용목록에_없으면_예외를_던진다() {
        assertThatThrownBy(() -> transactionHistoryService.findMyTransactions(userId, "REFUND", null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("조회 조건이 올바르지 않습니다.");

        verify(transactionMapper, never()).countMyTransactions(any(), any(), any());
    }

    @Test
    void page가_음수면_예외를_던진다() {
        assertThatThrownBy(() -> transactionHistoryService.findMyTransactions(userId, null, null, -1, 20))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("조회 조건이 올바르지 않습니다.");
    }

    @Test
    void size가_0이면_예외를_던진다() {
        assertThatThrownBy(() -> transactionHistoryService.findMyTransactions(userId, null, null, 0, 0))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void size가_최대값을_넘으면_예외를_던진다() {
        assertThatThrownBy(() -> transactionHistoryService.findMyTransactions(userId, null, null, 0, 101))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void category와_page_size가_없으면_기본값_ALL_0_20으로_조회한다() {
        given(transactionMapper.findMyTransactions(eq(userId), isNull(), isNull(), eq(0), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countMyTransactions(eq(userId), isNull(), isNull())).willReturn(0);

        TransactionHistoryListResponse response =
                transactionHistoryService.findMyTransactions(userId, null, null, null, null);

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);
        assertThat(response.isHasNext()).isFalse();
        verify(transactionMapper).findMyTransactions(userId, null, null, 0, 20);
    }

    @Test
    void category가_TRANSFER면_type을_TRANSFER_OUT으로_변환해서_조회한다() {
        given(transactionMapper.findMyTransactions(eq(userId), eq(TransactionType.TRANSFER_OUT), isNull(), eq(0), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countMyTransactions(eq(userId), eq(TransactionType.TRANSFER_OUT), isNull())).willReturn(0);

        transactionHistoryService.findMyTransactions(userId, "TRANSFER", null, null, null);

        verify(transactionMapper).findMyTransactions(userId, TransactionType.TRANSFER_OUT, null, 0, 20);
        verify(transactionMapper).countMyTransactions(userId, TransactionType.TRANSFER_OUT, null);
    }

    @Test
    void category가_CHARGE_PAYMENT면_DB값_그대로_전달한다() {
        given(transactionMapper.findMyTransactions(eq(userId), eq(TransactionType.CHARGE), isNull(), eq(0), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countMyTransactions(eq(userId), eq(TransactionType.CHARGE), isNull())).willReturn(0);

        transactionHistoryService.findMyTransactions(userId, "CHARGE", null, null, null);

        verify(transactionMapper).findMyTransactions(userId, TransactionType.CHARGE, null, 0, 20);
    }

    @Test
    void page와_size로_offset을_계산해서_조회한다() {
        given(transactionMapper.findMyTransactions(eq(userId), isNull(), isNull(), eq(40), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countMyTransactions(eq(userId), isNull(), isNull())).willReturn(0);

        transactionHistoryService.findMyTransactions(userId, null, null, 2, 20);

        verify(transactionMapper).findMyTransactions(userId, null, null, 40, 20);
    }

    @Test
    void 정상_조회시_페이지_정보와_목록을_그대로_응답한다() {
        TransactionHistoryItem item = TransactionHistoryItem.builder()
                .transactionId(141L)
                .type(TransactionCategory.PAYMENT)
                .direction("OUT")
                .title("이마트 서울점")
                .amount(45_200L)
                .status(TransactionStatus.COMPLETED)
                .riskLevel(RiskLevel.SAFE)
                .build();
        given(transactionMapper.findMyTransactions(eq(userId), isNull(), eq("이마트"), eq(0), eq(20)))
                .willReturn(List.of(item));
        given(transactionMapper.countMyTransactions(eq(userId), isNull(), eq("이마트"))).willReturn(43);

        TransactionHistoryListResponse response =
                transactionHistoryService.findMyTransactions(userId, null, "이마트", 0, 20);

        assertThat(response.getTransactions()).containsExactly(item);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isEqualTo(43);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isHasNext()).isTrue();
    }

    @Test
    void 마지막_페이지면_hasNext가_false다() {
        given(transactionMapper.findMyTransactions(eq(userId), isNull(), isNull(), eq(40), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countMyTransactions(eq(userId), isNull(), isNull())).willReturn(43);

        TransactionHistoryListResponse response =
                transactionHistoryService.findMyTransactions(userId, null, null, 2, 20);

        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isHasNext()).isFalse();
    }

    // ===== findWardTransactions =====

    @Test
    void 담당_피보호자가_아니면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(false);

        assertThatThrownBy(() -> transactionHistoryService.findWardTransactions(guardId, wardId, null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("연동된 피보호자를 찾을 수 없습니다.");

        verify(transactionMapper, never()).findWardTransactions(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void type이_허용목록에_없으면_예외를_던진다() {
        assertThatThrownBy(() -> transactionHistoryService.findWardTransactions(guardId, wardId, "REFUND", null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("요청 값이 올바르지 않습니다.");

        verify(transactionMapper, never()).countWardTransactions(any(), any(), any(), any());
    }

    @Test
    void riskLevel이_허용목록에_없으면_예외를_던진다() {
        assertThatThrownBy(() -> transactionHistoryService.findWardTransactions(guardId, wardId, null, "HIGH", null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("요청 값이 올바르지 않습니다.");
    }

    @Test
    void 보호자_조회도_page가_음수면_예외를_던진다() {
        assertThatThrownBy(() -> transactionHistoryService.findWardTransactions(guardId, wardId, null, null, -1, 20))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void 보호자_조회도_size가_범위를_벗어나면_예외를_던진다() {
        assertThatThrownBy(() -> transactionHistoryService.findWardTransactions(guardId, wardId, null, null, 0, 101))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void type_riskLevel_page_size가_없으면_기본값_0_20으로_조회한다() {
        given(transactionMapper.findWardTransactions(eq(guardId), eq(wardId), isNull(), isNull(), eq(0), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countWardTransactions(eq(guardId), eq(wardId), isNull(), isNull())).willReturn(0);

        GuardTransactionHistoryListResponse response =
                transactionHistoryService.findWardTransactions(guardId, wardId, null, null, null, null);

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isEqualTo(0);
        verify(transactionMapper).findWardTransactions(guardId, wardId, null, null, 0, 20);
    }

    @Test
    void type이_TRANSFER면_TRANSFER_OUT으로_변환해서_조회한다() {
        given(transactionMapper.findWardTransactions(eq(guardId), eq(wardId), eq(TransactionType.TRANSFER_OUT), isNull(), eq(0), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countWardTransactions(eq(guardId), eq(wardId), eq(TransactionType.TRANSFER_OUT), isNull())).willReturn(0);

        transactionHistoryService.findWardTransactions(guardId, wardId, "TRANSFER", null, null, null);

        verify(transactionMapper).findWardTransactions(guardId, wardId, TransactionType.TRANSFER_OUT, null, 0, 20);
    }

    @Test
    void type이_CHARGE_PAYMENT면_DB값_그대로_전달한다() {
        given(transactionMapper.findWardTransactions(eq(guardId), eq(wardId), eq(TransactionType.CHARGE), isNull(), eq(0), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countWardTransactions(eq(guardId), eq(wardId), eq(TransactionType.CHARGE), isNull())).willReturn(0);

        transactionHistoryService.findWardTransactions(guardId, wardId, "CHARGE", null, null, null);

        verify(transactionMapper).findWardTransactions(guardId, wardId, TransactionType.CHARGE, null, 0, 20);
    }

    @Test
    void 보호자_조회도_page와_size를_곱해서_offset을_계산한다() {
        given(transactionMapper.findWardTransactions(eq(guardId), eq(wardId), isNull(), isNull(), eq(40), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countWardTransactions(eq(guardId), eq(wardId), isNull(), isNull())).willReturn(0);

        transactionHistoryService.findWardTransactions(guardId, wardId, null, null, 2, 20);

        verify(transactionMapper).findWardTransactions(guardId, wardId, null, null, 40, 20);
    }

    @Test
    void 정상_조회시_피보호자_거래내역과_페이지_정보를_그대로_응답한다() {
        GuardTransactionHistoryItem item = GuardTransactionHistoryItem.builder()
                .transactionId(141L)
                .type(TransactionCategory.PAYMENT)
                .status(TransactionStatus.COMPLETED)
                .counterpartyName("이마트 서울점")
                .amount(45_200L)
                .riskLevel(RiskLevel.CAUTION)
                .riskReason("평소보다 큰 금액")
                .build();
        given(transactionMapper.findWardTransactions(eq(guardId), eq(wardId), isNull(), eq(RiskLevel.CAUTION), eq(0), eq(20)))
                .willReturn(List.of(item));
        given(transactionMapper.countWardTransactions(eq(guardId), eq(wardId), isNull(), eq(RiskLevel.CAUTION))).willReturn(43);

        GuardTransactionHistoryListResponse response =
                transactionHistoryService.findWardTransactions(guardId, wardId, null, "CAUTION", 0, 20);

        assertThat(response.getTransactions()).containsExactly(item);
        assertThat(response.getTotalElements()).isEqualTo(43);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isHasNext()).isTrue();
    }

    @Test
    void 보호자_조회도_마지막_페이지면_hasNext가_false다() {
        given(transactionMapper.findWardTransactions(eq(guardId), eq(wardId), isNull(), isNull(), eq(40), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countWardTransactions(eq(guardId), eq(wardId), isNull(), isNull())).willReturn(43);

        GuardTransactionHistoryListResponse response =
                transactionHistoryService.findWardTransactions(guardId, wardId, null, null, 2, 20);

        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isHasNext()).isFalse();
    }

    // ===== findMyTransactionDetail =====

    @Test
    void 본인_거래상세_조회시_거래가_없으면_예외를_던진다() {
        given(transactionMapper.findMyTransactionDetail(transactionId, userId)).willReturn(null);

        assertThatThrownBy(() -> transactionHistoryService.findMyTransactionDetail(userId, transactionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("거래 내역을 찾을 수 없습니다.");

        verify(transactionMapper, never()).findRiskReasons(any());
        verify(transactionMapper, never()).findLlmSummary(any());
    }

    @Test
    void 본인_거래상세가_TRANSFER_PAYMENT가_아니면_riskAnalysis는_null이고_위험사유는_조회하지_않는다() {
        TransactionDetailResponse detail = TransactionDetailResponse.builder()
                .transactionId(transactionId)
                .type(TransactionCategory.CHARGE)
                .riskLevel(RiskLevel.CAUTION)
                .riskScore(50)
                .build();
        given(transactionMapper.findMyTransactionDetail(transactionId, userId)).willReturn(detail);

        TransactionDetailResponse response = transactionHistoryService.findMyTransactionDetail(userId, transactionId);

        assertThat(response.getRiskAnalysis()).isNull();
        verify(transactionMapper, never()).findRiskReasons(any());
        verify(transactionMapper, never()).findLlmSummary(any());
    }

    @Test
    void 본인_거래상세가_PAYMENT이고_CAUTION_DANGER면_riskAnalysis를_조립한다() {
        TransactionDetailResponse detail = TransactionDetailResponse.builder()
                .transactionId(transactionId)
                .type(TransactionCategory.PAYMENT)
                .riskLevel(RiskLevel.DANGER)
                .riskScore(70)
                .build();
        given(transactionMapper.findMyTransactionDetail(transactionId, userId)).willReturn(detail);
        given(transactionMapper.findRiskReasons(transactionId)).willReturn(List.of("HIGH_AMOUNT"));
        given(transactionMapper.findLlmSummary(transactionId)).willReturn("평소보다 큰 결제");

        TransactionDetailResponse response = transactionHistoryService.findMyTransactionDetail(userId, transactionId);

        assertThat(response.getRiskAnalysis()).isNotNull();
        assertThat(response.getRiskAnalysis().getRiskScore()).isEqualTo(70);
        assertThat(response.getRiskAnalysis().getSummary()).isEqualTo("평소보다 큰 결제");
        assertThat(response.getRiskAnalysis().getReasons()).containsExactly("HIGH_AMOUNT");
    }

    @Test
    void 본인_거래상세가_TRANSFER이고_SAFE면_riskAnalysis는_null이다() {
        TransactionDetailResponse detail = TransactionDetailResponse.builder()
                .transactionId(transactionId)
                .type(TransactionCategory.TRANSFER)
                .riskLevel(RiskLevel.SAFE)
                .riskScore(10)
                .build();
        given(transactionMapper.findMyTransactionDetail(transactionId, userId)).willReturn(detail);

        TransactionDetailResponse response = transactionHistoryService.findMyTransactionDetail(userId, transactionId);

        assertThat(response.getRiskAnalysis()).isNull();
        verify(transactionMapper, never()).findRiskReasons(any());
    }

    @Test
    void 본인_거래상세가_TRANSFER이고_riskScore가_없으면_riskAnalysis는_null이다() {
        TransactionDetailResponse detail = TransactionDetailResponse.builder()
                .transactionId(transactionId)
                .type(TransactionCategory.TRANSFER)
                .riskLevel(RiskLevel.CAUTION)
                .riskScore(null)
                .build();
        given(transactionMapper.findMyTransactionDetail(transactionId, userId)).willReturn(detail);

        TransactionDetailResponse response = transactionHistoryService.findMyTransactionDetail(userId, transactionId);

        assertThat(response.getRiskAnalysis()).isNull();
        verify(transactionMapper, never()).findRiskReasons(any());
    }

    @Test
    void 본인_거래상세가_TRANSFER이고_CAUTION_DANGER면_riskAnalysis를_조립한다() {
        TransactionDetailResponse detail = TransactionDetailResponse.builder()
                .transactionId(transactionId)
                .type(TransactionCategory.TRANSFER)
                .riskLevel(RiskLevel.DANGER)
                .riskScore(85)
                .build();
        given(transactionMapper.findMyTransactionDetail(transactionId, userId)).willReturn(detail);
        given(transactionMapper.findRiskReasons(transactionId)).willReturn(List.of("REPEATED", "NEW_RECIPIENT"));
        given(transactionMapper.findLlmSummary(transactionId)).willReturn("평소보다 큰 금액");

        TransactionDetailResponse response = transactionHistoryService.findMyTransactionDetail(userId, transactionId);

        assertThat(response.getRiskAnalysis()).isNotNull();
        assertThat(response.getRiskAnalysis().getRiskScore()).isEqualTo(85);
        assertThat(response.getRiskAnalysis().getSummary()).isEqualTo("평소보다 큰 금액");
        assertThat(response.getRiskAnalysis().getReasons()).containsExactly("REPEATED", "NEW_RECIPIENT");
    }

    @Test
    void 본인_거래상세_응답의_riskScore_평면값은_항상_비워진다() {
        TransactionDetailResponse transferDetail = TransactionDetailResponse.builder()
                .transactionId(transactionId)
                .type(TransactionCategory.TRANSFER)
                .riskLevel(RiskLevel.DANGER)
                .riskScore(85)
                .build();
        given(transactionMapper.findMyTransactionDetail(transactionId, userId)).willReturn(transferDetail);
        given(transactionMapper.findRiskReasons(transactionId)).willReturn(List.of());
        given(transactionMapper.findLlmSummary(transactionId)).willReturn(null);

        TransactionDetailResponse response = transactionHistoryService.findMyTransactionDetail(userId, transactionId);

        assertThat(response.getRiskScore()).isNull();
    }

    // ===== findWardTransactionDetail =====

    @Test
    void 담당_피보호자가_아니면_거래상세_조회시_예외를_던지고_매퍼를_호출하지_않는다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(false);

        assertThatThrownBy(() -> transactionHistoryService.findWardTransactionDetail(guardId, wardId, transactionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("연동된 피보호자를 찾을 수 없습니다.");

        verify(transactionMapper, never()).findWardTransactionDetail(any(), any());
    }

    @Test
    void 피보호자_거래상세_조회시_거래가_없으면_예외를_던진다() {
        given(transactionMapper.findWardTransactionDetail(transactionId, wardId)).willReturn(null);

        assertThatThrownBy(() -> transactionHistoryService.findWardTransactionDetail(guardId, wardId, transactionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("거래 내역을 찾을 수 없습니다.");

        verify(transactionMapper, never()).findRiskReasons(any());
        verify(transactionMapper, never()).findLlmSummary(any());
    }

    @Test
    void 피보호자_거래상세가_TRANSFER가_아니면_riskAnalysis는_null이다() {
        GuardTransactionDetailResponse detail = GuardTransactionDetailResponse.builder()
                .transactionId(transactionId)
                .type(TransactionCategory.CHARGE)
                .riskLevel(RiskLevel.CAUTION)
                .riskScore(50)
                .build();
        given(transactionMapper.findWardTransactionDetail(transactionId, wardId)).willReturn(detail);

        GuardTransactionDetailResponse response =
                transactionHistoryService.findWardTransactionDetail(guardId, wardId, transactionId);

        assertThat(response.getRiskAnalysis()).isNull();
        verify(transactionMapper, never()).findRiskReasons(any());
    }

    @Test
    void 피보호자_거래상세가_TRANSFER이고_CAUTION_DANGER면_riskAnalysis를_조립한다() {
        GuardTransactionDetailResponse detail = GuardTransactionDetailResponse.builder()
                .transactionId(transactionId)
                .type(TransactionCategory.TRANSFER)
                .riskLevel(RiskLevel.CAUTION)
                .riskScore(60)
                .build();
        given(transactionMapper.findWardTransactionDetail(transactionId, wardId)).willReturn(detail);
        given(transactionMapper.findRiskReasons(transactionId)).willReturn(List.of("HIGH_AMOUNT"));
        given(transactionMapper.findLlmSummary(transactionId)).willReturn(null);

        GuardTransactionDetailResponse response =
                transactionHistoryService.findWardTransactionDetail(guardId, wardId, transactionId);

        assertThat(response.getRiskAnalysis()).isNotNull();
        assertThat(response.getRiskAnalysis().getRiskScore()).isEqualTo(60);
        assertThat(response.getRiskAnalysis().getReasons()).containsExactly("HIGH_AMOUNT");
    }

    @Test
    void 피보호자_거래상세_응답의_riskScore_평면값도_항상_비워진다() {
        GuardTransactionDetailResponse detail = GuardTransactionDetailResponse.builder()
                .transactionId(transactionId)
                .type(TransactionCategory.TRANSFER)
                .riskLevel(RiskLevel.DANGER)
                .riskScore(90)
                .build();
        given(transactionMapper.findWardTransactionDetail(transactionId, wardId)).willReturn(detail);
        given(transactionMapper.findRiskReasons(transactionId)).willReturn(List.of());
        given(transactionMapper.findLlmSummary(transactionId)).willReturn(null);

        GuardTransactionDetailResponse response =
                transactionHistoryService.findWardTransactionDetail(guardId, wardId, transactionId);

        assertThat(response.getRiskScore()).isNull();
    }
}
