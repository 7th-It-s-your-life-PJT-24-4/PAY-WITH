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
import com.paywith.guard.service.GuardService;
import com.paywith.transaction.dto.GuardTransactionHistoryItem;
import com.paywith.transaction.dto.GuardTransactionHistoryListResponse;
import com.paywith.transaction.dto.RiskReasonDetailResponse;
import com.paywith.transaction.dto.TransactionDetailResponse;
import com.paywith.transaction.dto.TransactionHistoryItem;
import com.paywith.transaction.dto.TransactionHistoryListResponse;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.wallet.dto.WalletBalanceResponse;
import com.paywith.wallet.service.WalletService;
import java.util.List;
import java.time.LocalDateTime;
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
        given(transactionMapper.findMyTransactions(eq(userId), eq("TRANSFER_OUT"), isNull(), eq(0), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countMyTransactions(eq(userId), eq("TRANSFER_OUT"), isNull())).willReturn(0);

        transactionHistoryService.findMyTransactions(userId, "TRANSFER", null, null, null);

        verify(transactionMapper).findMyTransactions(userId, "TRANSFER_OUT", null, 0, 20);
        verify(transactionMapper).countMyTransactions(userId, "TRANSFER_OUT", null);
    }

    @Test
    void category가_CHARGE_PAYMENT면_DB값_그대로_전달한다() {
        given(transactionMapper.findMyTransactions(eq(userId), eq("CHARGE"), isNull(), eq(0), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countMyTransactions(eq(userId), eq("CHARGE"), isNull())).willReturn(0);

        transactionHistoryService.findMyTransactions(userId, "CHARGE", null, null, null);

        verify(transactionMapper).findMyTransactions(userId, "CHARGE", null, 0, 20);
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
                .type("PAYMENT")
                .direction("OUT")
                .title("이마트 서울점")
                .amount(45_200L)
                .status("COMPLETED")
                .riskLevel("SAFE")
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
        given(transactionMapper.findWardTransactions(eq(guardId), eq(wardId), eq("TRANSFER_OUT"), isNull(), eq(0), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countWardTransactions(eq(guardId), eq(wardId), eq("TRANSFER_OUT"), isNull())).willReturn(0);

        transactionHistoryService.findWardTransactions(guardId, wardId, "TRANSFER", null, null, null);

        verify(transactionMapper).findWardTransactions(guardId, wardId, "TRANSFER_OUT", null, 0, 20);
    }

    @Test
    void type이_CHARGE_PAYMENT면_DB값_그대로_전달한다() {
        given(transactionMapper.findWardTransactions(eq(guardId), eq(wardId), eq("CHARGE"), isNull(), eq(0), eq(20)))
                .willReturn(List.of());
        given(transactionMapper.countWardTransactions(eq(guardId), eq(wardId), eq("CHARGE"), isNull())).willReturn(0);

        transactionHistoryService.findWardTransactions(guardId, wardId, "CHARGE", null, null, null);

        verify(transactionMapper).findWardTransactions(guardId, wardId, "CHARGE", null, 0, 20);
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
                .type("PAYMENT")
                .status("COMPLETED")
                .counterpartyName("이마트 서울점")
                .amount(45_200L)
                .riskLevel("CAUTION")
                .riskReason("평소보다 큰 금액")
                .build();
        given(transactionMapper.findWardTransactions(eq(guardId), eq(wardId), isNull(), eq("CAUTION"), eq(0), eq(20)))
                .willReturn(List.of(item));
        given(transactionMapper.countWardTransactions(eq(guardId), eq(wardId), isNull(), eq("CAUTION"))).willReturn(43);

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

    // ===== findWardTransactionDetail =====

    @Test
    void 거래상세도_담당_피보호자가_아니면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(false);

        assertThatThrownBy(() ->
                transactionHistoryService.findWardTransactionDetail(guardId, wardId, 141L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "LINK_001");

        verify(transactionMapper, never()).findWardTransactionDetail(any(), any(), any());
    }

    @Test
    void 거래상세가_없으면_예외를_던진다() {
        given(transactionMapper.findWardTransactionDetail(guardId, wardId, 141L))
                .willReturn(null);

        assertThatThrownBy(() ->
                transactionHistoryService.findWardTransactionDetail(guardId, wardId, 141L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "TRANSACTION_002");
    }

    @Test
    void 거래상세의_위험점수와_근거를_위험분석_객체로_조립한다() {
        LocalDateTime analyzedAt = LocalDateTime.of(2026, 7, 29, 10, 18);
        TransactionDetailResponse detail = TransactionDetailResponse.builder()
                .transactionId(141L)
                .riskLevel("DANGER")
                .riskScore(87)
                .analyzedAt(analyzedAt)
                .build();
        RiskReasonDetailResponse reason = RiskReasonDetailResponse.builder()
                .ruleCode("HIGH_AMOUNT")
                .description("평소보다 큰 금액의 거래에요.")
                .score(30)
                .build();
        given(transactionMapper.findWardTransactionDetail(guardId, wardId, 141L))
                .willReturn(detail);
        given(transactionMapper.findRiskReasons(141L)).willReturn(List.of(reason));
        given(transactionMapper.findLlmSummary(141L)).willReturn("평소와 다른 패턴이에요.");

        TransactionDetailResponse response =
                transactionHistoryService.findWardTransactionDetail(guardId, wardId, 141L);

        assertThat(response.getRiskScore()).isNull();
        assertThat(response.getAnalyzedAt()).isNull();
        assertThat(response.getRiskAnalysis().getRiskScore()).isEqualTo(87);
        assertThat(response.getRiskAnalysis().getSummary()).isEqualTo("평소와 다른 패턴이에요.");
        assertThat(response.getRiskAnalysis().getReasons()).containsExactly(reason);
        assertThat(response.getRiskAnalysis().getAnalyzedAt()).isEqualTo(analyzedAt);
    }

    @Test
    void 위험평가가_없는_거래상세는_위험분석을_조회하지_않는다() {
        TransactionDetailResponse detail = TransactionDetailResponse.builder()
                .transactionId(142L)
                .riskScore(null)
                .build();
        given(transactionMapper.findWardTransactionDetail(guardId, wardId, 142L))
                .willReturn(detail);

        TransactionDetailResponse response =
                transactionHistoryService.findWardTransactionDetail(guardId, wardId, 142L);

        assertThat(response.getRiskAnalysis()).isNull();
        verify(transactionMapper, never()).findRiskReasons(any());
        verify(transactionMapper, never()).findLlmSummary(any());
    }
}
