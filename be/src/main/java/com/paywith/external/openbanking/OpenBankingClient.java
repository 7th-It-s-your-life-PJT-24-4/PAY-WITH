package com.paywith.external.openbanking;

import com.paywith.external.openbanking.dto.DepositResponse;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.external.openbanking.dto.WithdrawResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OpenBankingClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private String clientUseCode;

    private static final DateTimeFormatter TRAN_DTIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${kftc.api.base-url}")
    private String baseUrl;

    @Value("${kftc.client-id}")
    private String clientId;

    @Value("${kftc.client-secret}")
    private String clientSecret;

    /** 접근토큰(Access Token) 발급 - 2-legged */
    public String getAccessToken() {
        String url = baseUrl + "/oauth/2.0/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("scope", "oob");
        params.add("grant_type", "client_credentials");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
        this.clientUseCode = (String) response.get("client_use_code");
        return (String) response.get("access_token");
    }

    /**
     * 현재 사용 중 — Mock 버전
     * TODO: 테스트베드 "테스트 정보 관리" 접근 권한 확보되면 inquireRealNameReal로 교체
     */
    public RealNameInquiryResponse inquireRealName(String bankCodeStd, String accountNum, String accountHolderInfo) {
        RealNameInquiryResponse response = new RealNameInquiryResponse();
        response.setRspCode("A0000");
        response.setBankCodeStd(bankCodeStd);
        response.setBankName(resolveMockBankName(bankCodeStd));
        response.setAccountNum(accountNum);
        response.setAccountHolderName(resolveMockHolderName(accountNum));
        response.setAccountType("1");
        return response;
    }

    // 계좌번호 별로 수취인 다르게 임시 매핑
    private String resolveMockHolderName(String accountNum) {
        return switch (accountNum) {
            case "11012300006781" -> "김시니어";
            case "11012300006782" -> "이보호자";
            case "22011122223333" -> "김준호";
            case "33044455556666" -> "박지연";
            case "44077788889999" -> "최영희";
            case "55011112222333" -> "정민수";
            default -> "홍길동";  // 등록 안 된 계좌번호는 기본값
        };
    }

    // bankCode에 맞는 은행명을 대충 흉내내기 위한 임시 매핑 (실제로는 banks 테이블/join으로 이미 처리 중)
    private String resolveMockBankName(String bankCodeStd) {
        return switch (bankCodeStd) {
            case "004" -> "KB국민은행";
            case "088" -> "신한은행";
            case "020" -> "우리은행";
            case "081" -> "하나은행";
            case "090" -> "카카오뱅크";
            default -> "알 수 없는 은행";
        };
    }

    public WithdrawResponse withdraw(String bankCodeStd, String accountNum, Long amount) {
        WithdrawResponse response = new WithdrawResponse();
        response.setRspCode("A0000");
        response.setTranAmt(amount);
        return response;
    }

    /**
     * 계좌실명조회 (예금주 확인)
     * @param bankCodeStd 개설기관 표준코드 (예: "004")
     * @param accountNum 계좌번호
     * @param accountHolderInfo 예금주 인증정보 (생년월일 등)
     */
//    public RealNameInquiryResponse inquireRealName(
//            String bankCodeStd, String accountNum, String accountHolderInfo) {
//
//        String accessToken = getAccessToken();
//        String url = baseUrl + "/v2.0/inquiry/real_name";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(accessToken);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        Map<String, Object> body = new HashMap<>();
//        body.put("bank_tran_id", generateBankTranId());
//        body.put("bank_code_std", bankCodeStd);
//        body.put("account_num", accountNum);
//        body.put("account_seq", "001");
//        body.put("account_holder_info_type", " ");
//        body.put("account_holder_info", accountHolderInfo);
//        body.put("tran_dtime", LocalDateTime.now().format(TRAN_DTIME_FORMAT));
//
//        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
//
//        Map<String, Object> raw = restTemplate.postForObject(url, request, Map.class);
//
//        RealNameInquiryResponse response = new RealNameInquiryResponse();
//        response.setRspCode((String) raw.get("rsp_code"));
//        response.setRspMessage((String) raw.get("rsp_message"));
//        response.setBankName((String) raw.get("bank_name"));
//        response.setAccountNum((String) raw.get("account_num"));
//        response.setAccountHolderName((String) raw.get("account_holder_name"));
//        response.setAccountType((String) raw.get("account_type"));
//
//        return response;
//    }

    /** 은행거래고유번호 생성 (이용기관코드 + 유니크값 조합이 표준이나, 테스트베드는 간단히 생성) */
    private String generateBankTranId() {
        String serialNumber = UUID.randomUUID().toString().replace("-", "").substring(0, 9);
        String bankTranId = clientUseCode + "U" + serialNumber;

        return bankTranId.toUpperCase(); // 추가: 전체를 대문자로 변환
    }


    public Map<String, Object> getRawTokenResponse() {
        String url = baseUrl + "/oauth/2.0/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("scope", "oob");
        params.add("grant_type", "client_credentials");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        return restTemplate.postForObject(url, request, Map.class);
    }

    // 송금용 확인 메서드 (TODO: 입금 이체 api 연동)
    public DepositResponse deposit(String bankCodeStd, String accountNum, Long amount) {
        DepositResponse response = new DepositResponse();
        response.setRspCode("A0000");
        response.setTranAmt(amount);
        return response;
    }
}