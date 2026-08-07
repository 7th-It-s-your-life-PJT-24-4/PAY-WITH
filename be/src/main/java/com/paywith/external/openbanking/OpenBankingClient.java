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

import javax.annotation.PostConstruct;
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

    @Value("${kftc.api.base-url:https://testapi.openbanking.or.kr}")
    private String baseUrl;

    @Value("${kftc.client-id:mock-client-id}")
    private String clientId;

    @Value("${kftc.client-secret:mock-client-secret}")
    private String clientSecret;

    // 시연용 예금주 이름 생성 매핑 테이블 (application.properties에서 주입, 재배포 없이 값 조정 가능)
    // 프로퍼티가 누락돼도 기동이 실패하지 않도록 기본값을 둔다
    @Value("${mock.holder-name.surnames:이,김,박,최,정,강,조,윤,장,임}")
    private String surnamesRaw;

    @Value("${mock.holder-name.given-first:서,민,지,수,윤,예,준,아,현,하}")
    private String givenFirstRaw;
    @Value("${mock.holder-name.given-second:아,민,준,호,영,진,희,우,빈,서}")
    private String givenSecondRaw;
    private String[] surnames;
    private String[] givenFirst;
    private String[] givenSecond;

    @PostConstruct
    private void initHolderNameTables() {
        surnames = surnamesRaw.split(",");
        givenFirst = givenFirstRaw.split(",");
        givenSecond = givenSecondRaw.split(",");
    }

    /**
     * 접근토큰(Access Token) 발급 - 2-legged
     */
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
        // 시연용: 계좌번호의 첫 자리/5번째 자리/마지막 자리를 뽑아 성+이름을 조합해 생성.
        // 등록된 몇 개 번호만 이름이 뜨던 방식 대신, 어떤 계좌번호를 입력해도 이름이 나오게 함.
        if (accountNum == null || accountNum.length() < 5) {
            return "홍길동"; // 비정상적으로 짧은 계좌번호는 기본값
        }

        String surname = pickByDigit(accountNum.charAt(0), surnames);
        String first = pickByDigit(accountNum.charAt(4), givenFirst);
        String second = pickByDigit(accountNum.charAt(accountNum.length() - 1), givenSecond);

        return surname + first + second;
    }

    // 숫자 문자 하나(digit)를 배열 인덱스로 변환해서 값을 꺼낸다. 숫자가 아니면 0번째로 고정.
    private String pickByDigit(char digit, String[] table) {
        if (!Character.isDigit(digit)) {
            return table[0];
        }
        return table[digit - '0'];
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

/**
 * 은행거래고유번호 생성 (이용기관코드 + 유니크값 조합이 표준이나, 테스트베드는 간단히 생성)
 */
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