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
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;

    public AuthController(AuthService authService, PhoneVerificationService phoneVerificationService) {
        this.authService = authService;
        this.phoneVerificationService = phoneVerificationService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/phone/code")
    public ApiResponse<PhoneCodeResponse> sendPhoneCode(@Valid @RequestBody PhoneCodeRequest request) {
        return ApiResponse.success(phoneVerificationService.sendCode(request));
    }

    @PostMapping("/phone/verify")
    public ApiResponse<PhoneVerifyResponse> verifyPhoneCode(@Valid @RequestBody PhoneVerifyRequest request) {
        return ApiResponse.success(phoneVerificationService.verifyCode(request));
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
    }
}
