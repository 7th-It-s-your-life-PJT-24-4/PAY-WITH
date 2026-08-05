package com.paywith.auth.controller;

import com.paywith.auth.dto.LoginRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
            + "전화번호가 없거나 비밀번호가 일치하지 않으면 401.")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @ApiOperation(
        value = "토큰 재발급",
        notes = "refreshToken으로 accessToken/refreshToken을 재발급한다. "
            + "토큰이 유효하지 않거나 Redis에 저장된 값과 다르면 401.")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @ApiOperation(
        value = "인증번호 발송",
        notes = "회원가입(SIGNUP) 또는 비밀번호 찾기(PASSWORD_RESET) 용도로 6자리 인증번호를 SMS로 "
            + "발송한다. 5분간 유효하며, 60초 재전송 쿨다운과 일일 5회 발송 제한이 있다.")
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
}
