package com.paywith.auth.controller;

import com.paywith.auth.dto.LoginRequest;
import com.paywith.auth.dto.PasswordResetRequest;
import com.paywith.auth.dto.PhoneCodeRequest;
import com.paywith.auth.dto.PhoneCodeResponse;
import com.paywith.auth.dto.PhoneVerifyRequest;
import com.paywith.auth.dto.PhoneVerifyResponse;
import com.paywith.auth.dto.RefreshTokenRequest;
import com.paywith.auth.dto.TokenResponse;
import com.paywith.auth.service.AuthService;
import com.paywith.auth.service.PhoneVerificationService;
import com.paywith.common.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "인증")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;

    public AuthController(AuthService authService, PhoneVerificationService phoneVerificationService) {
        this.authService = authService;
        this.phoneVerificationService = phoneVerificationService;
    }

    @ApiOperation(
        value = "로그인",
        notes = "전화번호와 비밀번호로 로그인해 accessToken/refreshToken(tokenType Bearer)을 발급한다. "
            + "전화번호는 비숫자를 제거한 뒤 조회하며 형식 검증은 없다. "
            + "phone·password 누락·공백은 400 REQUEST_001(message \"<필드>: <검증 메시지>\"). "
            + "미가입·탈퇴·비밀번호 불일치는 401(code 없음, 세 경우 모두 동일 메시지). "
            + "성공 시 refreshToken이 사용자당 1개로 저장되어 이전 refreshToken은 무효가 된다. "
            + "JSON 파싱 실패는 500.")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @ApiOperation(
        value = "토큰 재발급",
        notes = "refreshToken으로 accessToken/refreshToken을 재발급한다(회전: 새 refreshToken이 저장되어 "
            + "이전 refreshToken은 즉시 무효). refreshToken 누락·공백은 400 REQUEST_001. "
            + "서명 불일치·만료, 저장된 값과 불일치(이전 토큰 재사용 등), 사용자가 탈퇴(WITHDRAWN)했거나 "
            + "없는 경우(후자는 soft delete라 실질 미발생)는 401(code 없음, 메시지는 각각 다름). "
            + "JSON 파싱 실패는 500.")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @ApiOperation(
        value = "인증번호 발송",
        notes = "회원가입(SIGNUP) 또는 비밀번호 찾기(PASSWORD_RESET) 용도로 6자리 인증번호를 발송한다"
            + "(실 SMS 연동 전까지는 서버 로그로만 출력). 응답 expireIn은 인증번호 유효 시간(초, 300). "
            + "검사 순서: phone·purpose 누락·공백은 400 REQUEST_001 → 전화번호 형식(비숫자 제거 후 "
            + "01X + 숫자 7~8자리, 총 10~11자리) 오류는 400 PHONE_001 → purpose가 SIGNUP·PASSWORD_RESET "
            + "이외 값(대소문자 구분)이면 400(code 없음) → SIGNUP인데 이미 가입된 번호면 409 USER_001, "
            + "PASSWORD_RESET인데 미가입 번호면 404 USER_002 → 60초 재전송 쿨다운 중이면 429 PHONE_002"
            + "(data.retryAfter: 남은 초, 일일 횟수는 소모하지 않음) → 일일 5회 발송 한도(전화번호당 24시간)를 "
            + "넘으면 429 PHONE_003. 재발송 시 이전 인증번호·실패 횟수는 초기화된다. JSON 파싱 실패는 500.")
    @PostMapping("/phone/code")
    public ApiResponse<PhoneCodeResponse> sendPhoneCode(@Valid @RequestBody PhoneCodeRequest request) {
        return ApiResponse.success(phoneVerificationService.sendCode(request));
    }

    @ApiOperation(
        value = "인증번호 확인",
        notes = "발송된 인증번호를 확인하고, 이후 회원가입·비밀번호 찾기에 사용할 verificationToken"
            + "(하이픈 없는 UUID 32자 hex, 5분 유효, 발급된 전화번호에만 유효)을 발급한다. "
            + "phone·code 누락·공백은 400 REQUEST_001. 전화번호 형식(비숫자 제거 후 01X + 숫자 7~8자리) 또는 "
            + "code가 숫자 6자리가 아니면 400 PHONE_001. 인증번호가 만료·미발급이면 400 AUTH_003, "
            + "불일치면 400 AUTH_003(실패 횟수 증가). 5회 틀린 뒤의 요청은 정답이어도 429 PHONE_003이며 "
            + "인증번호가 무효화된다(재발송 필요). 성공 시 인증번호는 소멸해 재사용할 수 없고, 같은 번호로 "
            + "재검증하면 이전 verificationToken은 무효가 된다. JSON 파싱 실패는 500.")
    @PostMapping("/phone/verify")
    public ApiResponse<PhoneVerifyResponse> verifyPhoneCode(@Valid @RequestBody PhoneVerifyRequest request) {
        return ApiResponse.success(phoneVerificationService.verifyCode(request));
    }

    @ApiOperation(
        value = "비밀번호 재설정",
        notes = "verificationToken으로 본인 확인 후 비밀번호를 재설정한다. 성공 시 204(본문 없음)이며 "
            + "기존 refreshToken을 무효화해 전 기기에서 재로그인이 필요하고 verificationToken도 소멸한다. "
            + "검사 순서: phone·verificationToken·newPassword 누락·공백은 400 REQUEST_001 → 전화번호 형식"
            + "(비숫자 제거 후 01X + 숫자 7~8자리, 총 10~11자리) 오류는 400 PHONE_001 → newPassword 8자 미만은 "
            + "400 PASSWORD_001 → verificationToken이 없거나 만료됐거나 다른 전화번호로 발급된 것이면 "
            + "400 AUTH_003 → 미가입 번호면 404 USER_002. JSON 파싱 실패는 500.")
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
    }
}
