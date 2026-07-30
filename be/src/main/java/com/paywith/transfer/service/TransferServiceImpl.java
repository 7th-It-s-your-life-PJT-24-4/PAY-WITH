package com.paywith.transfer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.service.FdsEvaluationService;
import com.paywith.transfer.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService{

    private final OpenBankingClient openBankingClient;
    private final FdsEvaluationService fdsEvaluationService;
    private final TransferPreparationService transferPreparationService;
    private final TransferFinalizationService transferFinalizationService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public RecipientInquiryResponse inquireRecipient(RecipientInquiryRequest request) {

        RealNameInquiryResponse inquiryResponse = openBankingClient.inquireRealName(
                request.getBankCode(),
                request.getAccountNo(),
                null
        );

        if(!inquiryResponse.isSuccess()){
            throw new BusinessException(HttpStatus.NOT_FOUND, "해당 계좌를 찾을 수 없습니다.");
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
                throw new BusinessException(HttpStatus.CONFLICT, "이전 요청이 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }
            if (!existing.getIdempotencyKey().equals(requestHash)) {
                throw new BusinessException(HttpStatus.CONFLICT, "동일한 키로 다른 내용의 요청이 감지되었습니다.");
            }
            // 같은 키 + 같은 요청 + 이미 완료 → 재실행 없이 이전 결과 그대로 반환
            return existing.getResponse();
        }

        // 없다면 -> 송금 시작
        try {
            // 1~5 fds 평가 전까지
            PreparedTransfer prepared = transferPreparationService.prepare(userId, request);

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

        } catch (RuntimeException e) {
            // 실패하면 키를 지워서 같은 idempotencyKey로 재시도할 수 있게 한다
            // (안 지우면 TTL 5분 동안 "처리중"에 계속 걸려있게 됨)
            redisTemplate.delete(key);
            throw e;
        }
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
