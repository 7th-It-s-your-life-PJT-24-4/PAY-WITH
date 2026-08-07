package com.paywith.external.openbanking;

import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.recipient.mapper.BankMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenBankingClientTest {

    @Mock
    private BankMapper bankMapper;

    private OpenBankingClient client;

    @BeforeEach
    void setUp() {
        given(bankMapper.findBankName("004")).willReturn("KB국민은행");

        client = new OpenBankingClient(bankMapper);
        // application-local/prod.properties의 mock.holder-name.* 값과 동일하게 맞춘다
        ReflectionTestUtils.setField(client, "surnamesRaw", "이,김,박,최,정,강,조,윤,장,임");
        ReflectionTestUtils.setField(client, "givenFirstRaw", "서,민,지,수,윤,예,준,아,현,하");
        ReflectionTestUtils.setField(client, "givenSecondRaw", "아,민,준,호,영,진,희,우,빈,서");
        ReflectionTestUtils.invokeMethod(client, "initHolderNameTables");
    }

    @Test
    void 계좌번호의_1번째_5번째_마지막_자리로_이름을_조합한다() {
        // "11012300006781" -> [0]='1', [4]='2', [13]='1'
        RealNameInquiryResponse response = client.inquireRealName("004", "11012300006781", null);

        assertThat(response.getAccountHolderName()).isEqualTo("김지민");
    }

    @Test
    void 같은_자리_숫자를_가진_계좌번호는_같은_이름을_반환한다() {
        RealNameInquiryResponse first = client.inquireRealName("004", "11012300006781", null);
        RealNameInquiryResponse second = client.inquireRealName("088", "19012300006781", null);

        assertThat(first.getAccountHolderName()).isEqualTo(second.getAccountHolderName());
    }

    @Test
    void 다른_계좌번호는_다른_이름을_반환할_수_있다() {
        RealNameInquiryResponse a = client.inquireRealName("004", "11012300006781", null);
        RealNameInquiryResponse b = client.inquireRealName("004", "22011122223333", null);

        assertThat(a.getAccountHolderName()).isNotEqualTo(b.getAccountHolderName());
    }

    @Test
    void 계좌번호가_null이면_기본값_홍길동을_반환한다() {
        RealNameInquiryResponse response = client.inquireRealName("004", null, null);

        assertThat(response.getAccountHolderName()).isEqualTo("홍길동");
    }

    @Test
    void 계좌번호가_5자_미만이면_기본값_홍길동을_반환한다() {
        RealNameInquiryResponse response = client.inquireRealName("004", "1234", null);

        assertThat(response.getAccountHolderName()).isEqualTo("홍길동");
    }

    @Test
    void 숫자가_아닌_자리는_0번째_테이블값으로_대체된다() {
        // 첫 자리가 숫자가 아니므로 surnames[0]="이"로 고정, 나머지는 정상 매핑
        RealNameInquiryResponse response = client.inquireRealName("004", "A1012300006781", null);

        assertThat(response.getAccountHolderName()).isEqualTo("이지민");
    }

    @Test
    void 계좌번호가_정확히_5자면_5번째와_마지막_자리가_같은_인덱스를_가리킨다() {
        // length=5 -> charAt(4) == charAt(length-1) == 같은 문자
        RealNameInquiryResponse response = client.inquireRealName("004", "10002", null);

        // [0]='1' -> 김, [4]='2'(=마지막) -> givenFirst[2]=지, givenSecond[2]=준
        assertThat(response.getAccountHolderName()).isEqualTo("김지준");
    }

    @Test
    void 응답의_은행코드와_계좌번호는_그대로_반영된다() {
        RealNameInquiryResponse response = client.inquireRealName("004", "11012300006781", null);

        assertThat(response.getRspCode()).isEqualTo("A0000");
        assertThat(response.getBankCodeStd()).isEqualTo("004");
        assertThat(response.getBankName()).isEqualTo("KB국민은행");
        assertThat(response.getAccountNum()).isEqualTo("11012300006781");
        assertThat(response.getAccountType()).isEqualTo("1");
    }

    @Test
    void banks_테이블에_없는_은행코드면_알수없는은행을_반환한다() {
        given(bankMapper.findBankName("999")).willReturn(null);

        RealNameInquiryResponse response = client.inquireRealName("999", "11012300006781", null);

        assertThat(response.getBankName()).isEqualTo("알 수 없는 은행");
    }
}
