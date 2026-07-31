package com.paywith.recipient.mapper;

import com.paywith.recipient.domain.Recipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
