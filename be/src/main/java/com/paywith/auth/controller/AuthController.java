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
        notes = "전화번호와 비밀번호로 로그인해 accessToken/refreshToken을 발급한다. "
            + "전화번호가 없거나, 탈퇴한 계정이거나, 비밀번호가 일치하지 않으면 401(세 경우 모두 동일 메시지).")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @ApiOperation(
        value = "토큰 재발급",
        notes = "refreshToken으로 accessToken/refreshToken을 재발급한다. "
            + "토큰이 유효하지 않거나, Redis에 저장된 값과 다르거나, 사용자가 없거나 탈퇴한 경우 401.")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @ApiOperation(
        value = "인증번호 발송",
        notes = "회원가입(SIGNUP) 또는 비밀번호 찾기(PASSWORD_RESET) 용도로 6자리 인증번호를 발송한다"
            + "(실 SMS 연동 전까지는 서버 로그로만 출력). SIGNUP인데 이미 가입된 번호면 409, "
            + "PASSWORD_RESET인데 미가입 번호면 404. 인증번호는 5분간 유효하며, "
            + "60초 재전송 쿨다운·일일 5회 발송 제한을 넘으면 429.")
    @PostMapping("/phone/code")
    public ApiResponse<PhoneCodeResponse> sendPhoneCode(@Valid @RequestBody PhoneCodeRequest request) {
        return ApiResponse.success(phoneVerificationService.sendCode(request));
    }

    @ApiOperation(
        value = "인증번호 확인",
        notes = "발송된 인증번호를 확인하고, 이후 회원가입·비밀번호 찾기에 사용할 verificationToken(5분 "
            + "유효)을 발급한다. 5회 이상 틀리면 429.")
    @PostMapping("/phone/verify")
    public ApiResponse<PhoneVerifyResponse> verifyPhoneCode(@Valid @RequestBody PhoneVerifyRequest request) {
        return ApiResponse.success(phoneVerificationService.verifyCode(request));
    }

    @ApiOperation(
        value = "비밀번호 재설정",
        notes = "verificationToken으로 본인 확인 후 비밀번호를 재설정한다. 성공 시 204(본문 없음)이며 "
            + "기존 refreshToken을 무효화해 전 기기에서 재로그인이 필요하다. 전화번호 형식 오류·8자 미만 "
            + "비밀번호·토큰 무효는 400, 미가입 번호면 404.")
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
    }
}
