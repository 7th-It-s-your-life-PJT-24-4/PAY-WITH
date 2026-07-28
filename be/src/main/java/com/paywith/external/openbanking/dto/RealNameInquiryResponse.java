package com.paywith.external.openbanking.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RealNameInquiryResponse {
    private String apiTranId;
    private String apiTranDtm;
    private String rspCode;
    private String rspMessage;
    private String bankTranId;
    private String bankTranDate;
    private String bankCodeTran;
    private String bankRspCode;
    private String bankRspMessage;
    private String bankCodeStd;
    private String bankCodeSub;
    private String bankName;
    private String accountNum;
    private String accountHolderInfoType;
    private String accountHolderInfo;
    private String accountHolderName;
    private String accountType;

    // 성공 여부 판단 (금결원 응답코드 기준)
    public boolean isSuccess() {
        return "A0000".equals(rspCode);
    }
}