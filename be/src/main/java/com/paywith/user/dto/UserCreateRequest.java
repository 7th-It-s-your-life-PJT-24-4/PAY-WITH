package com.paywith.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "회원가입 요청")
@Getter
@Setter
public class UserCreateRequest {

    @ApiModelProperty(value = "사용자 역할. WARD(피보호자) 또는 GUARD(보호자), 대소문자 구분. 그 외 값은 400(code 없음), "
        + "누락·공백은 400 REQUEST_001. WARD 면 지갑이 함께 생성된다",
        required = true, allowableValues = "WARD,GUARD", example = "WARD")
    @NotBlank
    private String role;

    @ApiModelProperty(value = "가입 전화번호. 비숫자를 제거한 뒤 저장하며 형식 검증은 없다(서버 검증 없음 — 인증번호 발송 시 "
        + "검증된 번호에만 verificationToken 이 발급된다). verificationToken 이 발급된 번호와 다르면 400 AUTH_003, "
        + "이미 가입된 번호면 409(code 없음). 누락·공백은 400 REQUEST_001", required = true, example = "01012345678")
    @NotBlank
    private String phone;

    @ApiModelProperty(value = "로그인 비밀번호. 8자 미만·누락·공백은 400 REQUEST_001. 상한·문자 구성 검증은 없다(BCrypt 로 저장)",
        required = true, example = "password1234")
    @NotBlank
    @Size(min = 8, message = "비밀번호는 8자리 이상이어야 합니다.")
    private String password;

    @ApiModelProperty(value = "이름. 누락·공백은 400 REQUEST_001. 길이 검증은 없어(서버 검증 없음) 50자를 넘으면 DB 제약으로 500",
        required = true, example = "김시니어")
    @NotBlank
    private String name;

    @ApiModelProperty(value = "생년월일 YYYYMMDD(숫자 8자리). 숫자 8자리가 아니면 400 REQUEST_001, 달력상 존재하지 않는 "
        + "날짜(예: 20260231)는 400(code 없음)", required = true, example = "19550315")
    @NotBlank
    @Pattern(regexp = "\\d{8}", message = "생년월일은 YYYYMMDD 8자리여야 합니다.")
    private String birthDate;

    @ApiModelProperty(value = "성별. 남 또는 여 한 글자만 허용하며 그 외·누락·공백은 400 REQUEST_001",
        required = true, allowableValues = "남,여", example = "남")
    @NotBlank
    @Pattern(regexp = "[남여]", message = "gender는 남 또는 여여야 합니다.")
    private String gender;

    @ApiModelProperty(value = "프리셋 아바타 번호(1~6). 생략(null) 시 1. 1~6 밖이면 400 REQUEST_001", example = "1")
    @Min(value = 1, message = "avatarId는 1~6 사이여야 합니다.")
    @Max(value = 6, message = "avatarId는 1~6 사이여야 합니다.")
    private Integer avatarId;

    @ApiModelProperty(value = "결제 비밀번호(숫자 6자리). 로그인 password 와 별개로 users.pin 에 BCrypt 저장되며 QR 결제·송금·"
        + "보호자 대리충전 시 대조한다. 숫자 6자리가 아니거나 누락·공백이면 400 REQUEST_001", required = true, example = "123456")
    @NotBlank(message = "결제 비밀번호는 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "결제 비밀번호는 숫자 6자리여야 합니다.")
    private String paymentPassword;

    @ApiModelProperty(value = "인증번호 확인(POST /api/auth/phone/verify)으로 받은 verificationToken(하이픈 없는 UUID 32자 hex, "
        + "5분 유효). phone 으로 발급된 것이어야 하며 없거나 만료됐거나 다른 번호로 발급된 것이면 400 AUTH_003"
        + "(중복 가입 검사보다 먼저). 가입 성공 시 소멸한다. 누락·공백은 400 REQUEST_001", required = true, example = "3f2b8c1d9e4a4f6b8c2d1e0f9a8b7c6d")
    @NotBlank(message = "휴대폰 인증이 필요합니다.")
    private String verificationToken;
}
