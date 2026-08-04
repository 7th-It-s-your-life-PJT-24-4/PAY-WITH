package com.paywith.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.paywith.exception.BusinessException;
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

    @InjectMocks
    private TransactionHistoryServiceImpl transactionHistoryService;

    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        given(walletService.findMyBalance(userId)).willReturn(mock(WalletBalanceResponse.class));
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
}
