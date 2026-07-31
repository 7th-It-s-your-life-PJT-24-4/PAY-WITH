package com.paywith.recipient.mapper;

import com.paywith.recipient.domain.Recipient;
import com.paywith.transfer.dto.RecipientHistoryItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecipientMapper {

    // 이미 등록된 수취인인지 확인하는 메서드
    Recipient findRecipient(
            @Param("wardId") Long wardId,
            @Param("bankCode") String bankCode,
            @Param("accountNo") String accountNo
    );

    // 신규 수취인 등록 메서드
    void insertRecipient(Recipient recipient);

    // 기존 수취인 송금 정보 갱신 메서드
    void updateSendInfo(@Param("recipientId") Long recipientId);

    // 수취인 단건 조회(안전계좌 등록 시 소유자 확인)
    Recipient findById(@Param("recipientId") Long recipientId);

    // 피보호자의 ACTIVE 페어링 존재 여부
    boolean existsActivePairing(@Param("wardId") Long wardId);

    // 송금 내역 조회 (송금 이력이 있는 최근 수취인 목록)
    List<RecipientHistoryItem> findRecipientHistory(
            @Param("wardId") Long wardId,
            @Param("keyword") String keyword,
            @Param("sort") String sort,
            @Param("size") Integer size
    );
}
