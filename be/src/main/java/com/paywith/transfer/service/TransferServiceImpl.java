package com.paywith.transfer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import com.paywith.exception.BusinessException;
import com.paywith.exception.TransferIrrecoverableException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.service.FdsEvaluationService;
import com.paywith.recipient.mapper.RecipientMapper;
import com.paywith.transaction.domain.TransactionStatus;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.transfer.dto.*;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService{

    // 송금 내역 조회할 때 필요한 상수들
    private static final Set<String> ALLOWED_SORTS = Set.of("RECENT", "NAME");
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final Logger log = LoggerFactory.getLogger(TransferServiceImpl.class);

    private final OpenBankingClient openBankingClient;
    private final FdsEvaluationService fdsEvaluationService;
    private final TransferPreparationService transferPreparationService;
    private final TransferFinalizationService transferFinalizationService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final RecipientMapper recipientMapper;
    private final TransactionMapper transactionMapper;
    private final TransactionApprovalMapper transactionApprovalMapper;
    private final ApprovalRequestMapper approvalRequestMapper;


    @Override
    public RecipientInquiryResponse inquireRecipient(Long userId, RecipientInquiryRequest request) {

        User user = userMapper.findById(userId);
        if (user.getRole() != Role.WARD) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "AUTH_004", "피보호자만 접근할 수 있습니다.");
        }
        if (!recipientMapper.existsActivePairing(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "WARD_001", "페어링 완료 후 이용할 수 있습니다.");
        }

        RealNameInquiryResponse inquiryResponse = openBankingClient.inquireRealName(
                request.getBankCode(),
                request.getAccountNo(),
                null
        );

        if(!inquiryResponse.isSuccess()){
            throw new BusinessException(HttpStatus.NOT_FOUND, "RECIPIENT_001", "수취 계좌를 확인할 수 없습니다.");
        }

        return RecipientInquiryResponse.builder()
                .bankCode(request.getBankCode())
                .bankName(inquiryResponse.getBankName())
                .accountNo(request.getAccountNo())
                .recipientName(inquiryResponse.getAccountHolderName())
                .build();
    }

    @Override
    // @Transactional 처리 하지 않음
    public TransferResponse transfer(Long userId, String idempotencyKey, TransferRequest request) {
        String key = "idempotency:transfer:" + userId + ":" + idempotencyKey;
        String requestHash = hashRequest(request); // bankCode+accountNo+amount+memo 기준 (pin은 제외)

        IdempotencyRecord initial = new IdempotencyRecord(IdempotencyStatus.PROCESSING, requestHash, null);

        // 키가 없을 때만 원자적으로 세팅. 두 요청이 동시에 와도 하나만 성공한다.
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, toJson(initial), Duration.ofMinutes(5));

        // 만약 키가 있다면
        if (Boolean.FALSE.equals(acquired)) {
            IdempotencyRecord existing = fromJson(redisTemplate.opsForValue().get(key));

            if (existing.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_002", "동일한 송금 요청을 처리중입니다.");
            }
            if (!existing.getIdempotencyKey().equals(requestHash)) {
                throw new BusinessException(HttpStatus.CONFLICT, "TRANSFER_005", "동일한 요청 식별자로 다른 송금이 요청되었습니다.");
            }
            if (existing.getStatus() == IdempotencyStatus.FAILED) {
                // 입금(deposit) 호출 이후 실패로 확정된 요청 -> 자동 재실행 금지, 사람이 확인해야 함
                throw new BusinessException(HttpStatus.CONFLICT,
                        "이전 요청이 처리 중 실패했습니다. 잔액을 확인 후 고객센터로 문의해주세요.");
            }
            // 같은 키 + 같은 요청 + 이미 완료 → 재실행 없이 이전 결과 그대로 반환
            return existing.getResponse();
        }

        // 없다면 -> 송금 시작
        Long transactionId = null;
        try {
            // 1~5 fds 평가 전까지
            PreparedTransfer prepared = transferPreparationService.prepare(userId, request);
            transactionId = prepared.getTransaction().getTransactionId();

            // FDS 평가 (트랜잭션 밖에서 실행)
            FdsEvaluationRequest fdsRequest = new FdsEvaluationRequest(
                    prepared.getTransaction().getTransactionId(),
                    prepared.getWallet().getWalletId(),
                    prepared.getRecipient().getRecipientId(),
                    BigDecimal.valueOf(request.getAmount()),
                    request.getMemo()
            );
            RiskLevel riskLevel = fdsEvaluationService.evaluate(fdsRequest);

            // 6~12 fds 판정 이후 상태 업데이트, 잔액 확인, 이체, 완료 상태 업데이트
            TransferResponse response = transferFinalizationService.finalize(prepared, riskLevel, request);

            // 완료됐으니 결과를 저장해두고 TTL을 늘려서, 재전송이 들어와도 재실행 없이 이 값을 돌려줌
            IdempotencyRecord done = new IdempotencyRecord(IdempotencyStatus.DONE, requestHash, response);
            redisTemplate.opsForValue().set(key, toJson(done), Duration.ofHours(24));

            return response;

        } catch (TransferIrrecoverableException e) {
            // 입금(deposit) 호출 이후("돌아올 수 없는 지점" 통과 후) 실패 -> 키를 지우면 안 됨.
            // 지우고 재시도를 허용하면 prepare()부터 다시 돌면서 deposit()이 또 호출되어 이중 입금이 된다.
            // 그래서 삭제 대신 FAILED로 확정 기록해서, 같은 키로 다시 오면 재실행 없이 바로 실패 안내한다.
            IdempotencyRecord failed = new IdempotencyRecord(IdempotencyStatus.FAILED, requestHash, null);
            redisTemplate.opsForValue().set(key, toJson(failed), Duration.ofHours(24));
            throw e;
        } catch (RuntimeException e) {
            // 그 이전 단계(prepare, FDS 평가, 잔액 차감) 실패는 외부에 아무 영향이 없으므로
            // 키를 지워서 같은 idempotencyKey로 재시도할 수 있게 한다
            // (안 지우면 TTL 5분 동안 "처리중"에 계속 걸려있게 됨)
            // REQUESTED => FAILED로 상태 업데이트
            if (transactionId != null){
                markFailed(transactionId);
            }
            redisTemplate.delete(key);
            throw e;
        }
    }

    private void markFailed(Long transactionId){
        try{
            if (transactionMapper.markFailedIfRequested(transactionId) == 0){
                log.warn("거래가 이미 다른 상태로 진행돼 FAILED로 종결하지 않음. transactionId={}", transactionId);
            }
        } catch (RuntimeException e){
            log.error("거래를 FAILED로 종결하지 못함. transactionId={}", transactionId,e);
        }
    }

    @Override
    public RecipientHistoryListResponse getRecipientHistory(Long userId, String keyword, String sort, Integer size) {

        // 1. 피보호자 맞는지 확인
        User user = userMapper.findById(userId);
        if (user.getRole() != Role.WARD){
            throw new BusinessException(HttpStatus.FORBIDDEN, "AUTH_004", "피보호자만 접근할 수 있습니다.");
        }

        // 2. 보호자랑 페어링 되어 있는 사람 맞는지 확인
        if(!recipientMapper.existsActivePairing(userId)){
            throw new BusinessException(HttpStatus.FORBIDDEN, "WARD_001", "페어링 완료 후 이용할 수 있습니다.");
        }

        // 3. sort 기본값 처리 및 검증
        String resolvedSort = (sort == null || sort.isBlank() ? "RECENT" : sort);
        if(!ALLOWED_SORTS.contains(resolvedSort)){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RECIPIENT_004","조회 조건이 올바르지 않습니다.");
        }

        // 4. size 기본값 처리 및 검증
        int resolvedSize = (size == null) ? DEFAULT_SIZE : size;
        if(resolvedSize < 1 || resolvedSize > MAX_SIZE){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RECIPIENT_004", "조회 조건이 올바르지 않습니다.");
        }

        // 5. 실제 조회
        List<RecipientHistoryItem> recipients = recipientMapper.findRecipientHistory(userId, keyword, resolvedSort, resolvedSize);

        // 6. 응답
        return RecipientHistoryListResponse.builder()
                .recipients(recipients)
                .build();
    }

    @Override
    @Transactional
    public TransferCancelResponse cancelHeldTransfer(Long userId, Long transactionId) {
        // 1. 피보호자 계정인지 확인
        User user = userMapper.findById(userId);
        if (user.getRole() != Role.WARD) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "AUTH_004", "피보호자만 접근할 수 있습니다.");
        }

        // 2. 존재 + 본인 소유 + 송금 여부 확인
        TransactionStatus status = transactionMapper.findTransferStatusForCancel(transactionId, userId);
        if (status == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TRANSFER_007", "송금 거래를 찾을 수 없습니다.");
        }

        // 3. HELD가 아니면 취소 불가
        if (status != TransactionStatus.HELD) {
            throw new BusinessException(HttpStatus.CONFLICT, "TRANSFER_008", "승인 대기 중인 송금만 취소할 수 있습니다.");
        }

        // 4. 실제 취소 (조건부 UPDATE).
        int canceled = transactionApprovalMapper.cancelHeldByWard(transactionId);
        if (canceled == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "TRANSFER_008", "승인 대기 중인 송금만 취소할 수 있습니다.");
        }

        // 5. 승인 요청 종결
        int approvalCanceled = approvalRequestMapper.cancelPendingByTransactionId(transactionId);
        if (approvalCanceled == 0) {
            log.warn("거래는 취소됐지만 승인요청이 이미 다른 상태였음. transactionId={}", transactionId);
        }

        return new TransferCancelResponse(transactionId, TransactionStatus.CANCELED);
    }

    private String hashRequest(TransferRequest request) {
        // pin은 민감정보라 해시 대상에서 제외. bankCode+accountNo+amount+memo 조합으로 요청 내용 식별
        String raw = request.getBankCode() + "|" + request.getAccountNo() + "|"
                + request.getAmount() + "|" + (request.getMemo() == null ? "" : request.getMemo());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("해시 알고리즘을 찾을 수 없습니다.", e);
        }
    }

    private String toJson(IdempotencyRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("IdempotencyRecord 직렬화 실패", e);
        }
    }

    private IdempotencyRecord fromJson(String json) {
        try {
            return objectMapper.readValue(json, IdempotencyRecord.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("IdempotencyRecord 역직렬화 실패", e);
        }
    }

}
