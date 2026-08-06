package com.paywith.transfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import com.paywith.exception.BusinessException;
import com.paywith.exception.TransferIrrecoverableException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.service.FdsEvaluationService;
import com.paywith.recipient.domain.Recipient;
import com.paywith.recipient.mapper.RecipientMapper;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.transfer.dto.IdempotencyRecord;
import com.paywith.transfer.dto.IdempotencyStatus;
import com.paywith.transfer.dto.PreparedTransfer;
import com.paywith.transfer.dto.RecipientHistoryItem;
import com.paywith.transfer.dto.RecipientHistoryListResponse;
import com.paywith.transfer.dto.RecipientInquiryRequest;
import com.paywith.transfer.dto.RecipientInquiryResponse;
import com.paywith.transfer.dto.TransferCancelResponse;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.transfer.dto.TransferResponse;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * TransferServiceImpl 은 이제 준비(TransferPreparationService)와 완료(TransferFinalizationService)를
 * 오케스트레이션하며 idempotency 만 책임진다. 준비/완료 단계 자체의 비즈니스 규칙(PIN 검증, 신규/기존
 * 수취인 분기, 잔액 차감 등)은 각 서비스의 별도 테스트에서 다뤄야 한다.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private OpenBankingClient openBankingClient;

    @Mock
    private FdsEvaluationService fdsEvaluationService;

    @Mock
    private TransferPreparationService transferPreparationService;

    @Mock
    private TransferFinalizationService transferFinalizationService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private UserMapper userMapper;

    @Mock
    private RecipientMapper recipientMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private TransactionApprovalMapper transactionApprovalMapper;

    @Mock
    private ApprovalRequestMapper approvalRequestMapper;

    @InjectMocks
    private TransferServiceImpl transferService;

    private final Long userId = 1L;
    private final Long transactionId = 500L;
    private final String idempotencyKey = "idem-key-1";
    private final String key = "idempotency:transfer:" + userId + ":" + idempotencyKey;

    private TransferRequest request;
    private PreparedTransfer preparedTransfer;

    @BeforeEach
    void setUp() {
        request = new TransferRequest("004", "11012300006781", 50_000L, "생활비", "123456");

        Wallet wallet = Wallet.builder().walletId(10L).userId(userId).balance(100_000L).build();
        Recipient recipient = Recipient.builder().recipientId(200L).build();
        Transaction transaction = Transaction.builder().transactionId(999L).build();
        preparedTransfer = PreparedTransfer.builder()
                .wallet(wallet)
                .recipient(recipient)
                .transaction(transaction)
                .build();
    }

    @Test
    void 수취인_조회_성공() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(userId)).willReturn(true);

        RealNameInquiryResponse mockResponse = new RealNameInquiryResponse();
        mockResponse.setRspCode("A0000");
        mockResponse.setBankName("KB국민은행");
        mockResponse.setAccountHolderName("홍길동");

        given(openBankingClient.inquireRealName("004", "11012300006781", null))
                .willReturn(mockResponse);

        RecipientInquiryRequest inquiryRequest = new RecipientInquiryRequest("004", "11012300006781");

        RecipientInquiryResponse result = transferService.inquireRecipient(userId, inquiryRequest);

        assertThat(result.getBankName()).isEqualTo("KB국민은행");
        assertThat(result.getRecipientName()).isEqualTo("홍길동");
    }

    @Test
    void 수취인_조회_실패시_예외() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(userId)).willReturn(true);

        RealNameInquiryResponse mockResponse = new RealNameInquiryResponse();
        mockResponse.setRspCode("A0004");

        given(openBankingClient.inquireRealName("004", "9999999999", null))
                .willReturn(mockResponse);

        RecipientInquiryRequest inquiryRequest = new RecipientInquiryRequest("004", "9999999999");

        assertThatThrownBy(() -> transferService.inquireRecipient(userId, inquiryRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("수취 계좌를 확인할 수 없습니다");
    }

    @Test
    void 정상_송금이면_완료_응답을_반환하고_결과를_24시간_캐시한다() {
        TransferResponse response = TransferResponse.builder()
                .transactionId(999L)
                .status("COMPLETED")
                .holderName("김시니어")
                .balanceAfter(50_000L)
                .build();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(key), anyString(), eq(Duration.ofMinutes(5))))
                .willReturn(true);
        given(transferPreparationService.prepare(userId, request)).willReturn(preparedTransfer);
        given(fdsEvaluationService.evaluate(any(FdsEvaluationRequest.class))).willReturn(RiskLevel.SAFE);
        given(transferFinalizationService.finalize(preparedTransfer, RiskLevel.SAFE, request))
                .willReturn(response);

        TransferResponse result = transferService.transfer(userId, idempotencyKey, request);

        assertThat(result).isEqualTo(response);
        verify(valueOperations).set(eq(key), anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    void FDS_위험등급으로_보류돼도_결과를_그대로_반환하고_캐시한다() {
        TransferResponse held = TransferResponse.builder()
                .transactionId(999L)
                .status("HELD")
                .build();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(key), anyString(), eq(Duration.ofMinutes(5))))
                .willReturn(true);
        given(transferPreparationService.prepare(userId, request)).willReturn(preparedTransfer);
        given(fdsEvaluationService.evaluate(any(FdsEvaluationRequest.class))).willReturn(RiskLevel.DANGER);
        given(transferFinalizationService.finalize(preparedTransfer, RiskLevel.DANGER, request))
                .willReturn(held);

        TransferResponse result = transferService.transfer(userId, idempotencyKey, request);

        assertThat(result.getStatus()).isEqualTo("HELD");
        verify(valueOperations).set(eq(key), anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    void 이전_요청이_아직_처리중이면_충돌_예외() throws Exception {
        IdempotencyRecord processing = new IdempotencyRecord(IdempotencyStatus.PROCESSING, "아무-해시", null);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(key), anyString(), eq(Duration.ofMinutes(5))))
                .willReturn(false);
        String processingJson = objectMapper.writeValueAsString(processing);
        given(valueOperations.get(key)).willReturn(processingJson);

        assertThatThrownBy(() -> transferService.transfer(userId, idempotencyKey, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("처리중");

        verify(transferPreparationService, never()).prepare(any(), any());
    }

    @Test
    void 완료된_같은_요청이_재전송되면_저장된_응답을_그대로_반환한다() throws Exception {
        TransferResponse previousResponse = TransferResponse.builder()
                .transactionId(999L)
                .status("COMPLETED")
                .build();
        IdempotencyRecord done = new IdempotencyRecord(IdempotencyStatus.DONE, hashRequest(request), previousResponse);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(key), anyString(), eq(Duration.ofMinutes(5))))
                .willReturn(false);
        String doneJson = objectMapper.writeValueAsString(done);
        given(valueOperations.get(key)).willReturn(doneJson);

        TransferResponse result = transferService.transfer(userId, idempotencyKey, request);

        assertThat(result).isEqualTo(previousResponse);
        verify(transferPreparationService, never()).prepare(any(), any());
        verify(transferFinalizationService, never()).finalize(any(), any(), any());
    }

    @Test
    void 같은_키로_다른_내용의_요청이면_충돌_예외() throws Exception {
        IdempotencyRecord done = new IdempotencyRecord(IdempotencyStatus.DONE, "다른-요청의-해시", null);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(key), anyString(), eq(Duration.ofMinutes(5))))
                .willReturn(false);
        String doneJson = objectMapper.writeValueAsString(done);
        given(valueOperations.get(key)).willReturn(doneJson);

        assertThatThrownBy(() -> transferService.transfer(userId, idempotencyKey, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("동일한 요청 식별자로 다른 송금이 요청되었습니다");
    }

    @Test
    void 송금_처리_중_예외가_나면_idempotency_키를_삭제해_재시도를_허용한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(key), anyString(), eq(Duration.ofMinutes(5))))
                .willReturn(true);
        given(transferPreparationService.prepare(userId, request)).willReturn(preparedTransfer);
        given(fdsEvaluationService.evaluate(any(FdsEvaluationRequest.class))).willReturn(RiskLevel.SAFE);
        given(transferFinalizationService.finalize(preparedTransfer, RiskLevel.SAFE, request))
                .willThrow(new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "송금 가능한 잔액이 부족합니다."));

        assertThatThrownBy(() -> transferService.transfer(userId, idempotencyKey, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("잔액이 부족합니다");

        verify(redisTemplate).delete(key);
        verify(valueOperations, never()).set(eq(key), anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    void 돌아올_수_없는_지점_이후_실패하면_키를_삭제하지_않고_FAILED로_확정한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(key), anyString(), eq(Duration.ofMinutes(5))))
                .willReturn(true);
        given(transferPreparationService.prepare(userId, request)).willReturn(preparedTransfer);
        given(fdsEvaluationService.evaluate(any(FdsEvaluationRequest.class))).willReturn(RiskLevel.SAFE);
        given(transferFinalizationService.finalize(preparedTransfer, RiskLevel.SAFE, request))
                .willThrow(new TransferIrrecoverableException("송금 처리 중 오류가 발생했습니다. transactionId=999"));

        assertThatThrownBy(() -> transferService.transfer(userId, idempotencyKey, request))
                .isInstanceOf(TransferIrrecoverableException.class);

        // 삭제하면 안 된다 -> 삭제하면 재시도가 허용되어 prepare()부터 다시 돌면서 deposit()이 또 호출된다
        verify(redisTemplate, never()).delete(key);
        verify(valueOperations).set(eq(key), anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    void 이전_요청이_FAILED로_확정된_상태면_재실행하지_않고_안내_에러를_던진다() throws Exception {
        IdempotencyRecord failed = new IdempotencyRecord(IdempotencyStatus.FAILED, hashRequest(request), null);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(key), anyString(), eq(Duration.ofMinutes(5))))
                .willReturn(false);
        String failedJson = objectMapper.writeValueAsString(failed);
        given(valueOperations.get(key)).willReturn(failedJson);

        assertThatThrownBy(() -> transferService.transfer(userId, idempotencyKey, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("고객센터");

        verify(transferPreparationService, never()).prepare(any(), any());
        verify(transferFinalizationService, never()).finalize(any(), any(), any());
    }

    // ===== getRecipientHistory =====

    private User userWithRole(Role role) {
        User user = new User();
        user.setId(userId);
        user.setRole(role);
        return user;
    }

    @Test
    void 이력조회시_피보호자가_아니면_예외() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.GUARD));

        assertThatThrownBy(() -> transferService.getRecipientHistory(userId, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("피보호자만 접근할 수 있습니다.");

        verify(recipientMapper, never()).existsActivePairing(any());
    }

    @Test
    void 이력조회시_활성_페어링이_없으면_예외() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(userId)).willReturn(false);

        assertThatThrownBy(() -> transferService.getRecipientHistory(userId, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(recipientMapper, never()).findRecipientHistory(any(), any(), any(), any());
    }

    @Test
    void 허용되지_않는_정렬값이면_예외() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(userId)).willReturn(true);

        assertThatThrownBy(() -> transferService.getRecipientHistory(userId, null, "INVALID", null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("조회 조건이 올바르지 않습니다.");
    }

    @Test
    void size가_1보다_작으면_예외() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(userId)).willReturn(true);

        assertThatThrownBy(() -> transferService.getRecipientHistory(userId, null, null, 0))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("조회 조건이 올바르지 않습니다.");
    }

    @Test
    void size가_50을_초과하면_예외() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(userId)).willReturn(true);

        assertThatThrownBy(() -> transferService.getRecipientHistory(userId, null, null, 51))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("조회 조건이 올바르지 않습니다.");
    }

    @Test
    void sort와_size를_생략하면_기본값_RECENT와_20으로_조회한다() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(userId)).willReturn(true);
        given(recipientMapper.findRecipientHistory(userId, null, "RECENT", 20)).willReturn(List.of());

        transferService.getRecipientHistory(userId, null, null, null);

        verify(recipientMapper).findRecipientHistory(userId, null, "RECENT", 20);
    }

    @Test
    void 정상_조회시_조회된_목록을_그대로_응답한다() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(userId)).willReturn(true);
        RecipientHistoryItem item = RecipientHistoryItem.builder()
                .recipientId(200L)
                .holderName("김수취")
                .bankCode("004")
                .bankName("KB국민은행")
                .accountNo("11012300006781")
                .sendCount(3)
                .isRegisteredSafe(true)
                .build();
        given(recipientMapper.findRecipientHistory(userId, "김", "NAME", 10)).willReturn(List.of(item));

        RecipientHistoryListResponse response = transferService.getRecipientHistory(userId, "김", "NAME", 10);

        assertThat(response.getRecipients()).containsExactly(item);
    }

    // ===== cancelHeldTransfer =====

    @Test
    void 취소_시도자가_피보호자가_아니면_예외() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.GUARD));

        assertThatThrownBy(() -> transferService.cancelHeldTransfer(userId, transactionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("피보호자만 접근할 수 있습니다.");

        verify(transactionMapper, never()).findTransferStatusForCancel(any(), any());
    }

    @Test
    void 본인_소유의_송금_거래가_아니면_예외() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(transactionMapper.findTransferStatusForCancel(transactionId, userId)).willReturn(null);

        assertThatThrownBy(() -> transferService.cancelHeldTransfer(userId, transactionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("송금 거래를 찾을 수 없습니다.");

        verify(transactionApprovalMapper, never()).cancelHeldByWard(any());
    }

    @Test
    void HELD_상태가_아니면_취소할_수_없다() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(transactionMapper.findTransferStatusForCancel(transactionId, userId)).willReturn("COMPLETED");

        assertThatThrownBy(() -> transferService.cancelHeldTransfer(userId, transactionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("승인 대기 중인 송금만 취소할 수 있습니다.");

        verify(transactionApprovalMapper, never()).cancelHeldByWard(any());
    }

    @Test
    void 취소_UPDATE가_0건이면_이미_다른_상태로_바뀐_것이므로_예외() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(transactionMapper.findTransferStatusForCancel(transactionId, userId)).willReturn("HELD");
        given(transactionApprovalMapper.cancelHeldByWard(transactionId)).willReturn(0);

        assertThatThrownBy(() -> transferService.cancelHeldTransfer(userId, transactionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("승인 대기 중인 송금만 취소할 수 있습니다.");

        verify(approvalRequestMapper, never()).cancelPendingByTransactionId(any());
    }

    @Test
    void 정상_취소시_거래와_승인요청을_모두_취소하고_응답을_반환한다() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(transactionMapper.findTransferStatusForCancel(transactionId, userId)).willReturn("HELD");
        given(transactionApprovalMapper.cancelHeldByWard(transactionId)).willReturn(1);
        given(approvalRequestMapper.cancelPendingByTransactionId(transactionId)).willReturn(1);

        TransferCancelResponse response = transferService.cancelHeldTransfer(userId, transactionId);

        assertThat(response.getTransactionId()).isEqualTo(transactionId);
        assertThat(response.getStatus()).isEqualTo("CANCELED");
        verify(transactionApprovalMapper).cancelHeldByWard(transactionId);
        verify(approvalRequestMapper).cancelPendingByTransactionId(transactionId);
    }

    @Test
    void 승인요청이_이미_다른_상태여도_거래_취소_자체는_성공한다() {
        given(userMapper.findById(userId)).willReturn(userWithRole(Role.WARD));
        given(transactionMapper.findTransferStatusForCancel(transactionId, userId)).willReturn("HELD");
        given(transactionApprovalMapper.cancelHeldByWard(transactionId)).willReturn(1);
        given(approvalRequestMapper.cancelPendingByTransactionId(transactionId)).willReturn(0);

        TransferCancelResponse response = transferService.cancelHeldTransfer(userId, transactionId);

        assertThat(response.getStatus()).isEqualTo("CANCELED");
    }

    /**
     * TransferServiceImpl#hashRequest 와 동일한 알고리즘(SHA-256, bankCode|accountNo|amount|memo).
     * private 메서드라 직접 호출할 수 없어 idempotency 재전송 시나리오를 만들기 위해 동일 로직을 복제했다.
     */
    private static String hashRequest(TransferRequest request) throws NoSuchAlgorithmException {
        String raw = request.getBankCode() + "|" + request.getAccountNo() + "|"
                + request.getAmount() + "|" + (request.getMemo() == null ? "" : request.getMemo());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
