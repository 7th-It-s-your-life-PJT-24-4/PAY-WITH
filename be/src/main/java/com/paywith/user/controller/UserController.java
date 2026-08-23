package com.paywith.user.controller;

import com.paywith.common.ApiResponse;
import com.paywith.user.dto.FcmTokenUpdateRequest;
import com.paywith.user.dto.UserCreateRequest;
import com.paywith.user.dto.UserResponse;
import com.paywith.user.dto.UserUpdateRequest;
import com.paywith.user.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "사용자")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @ApiOperation(value = "전체 사용자 조회")
    @GetMapping
    public ApiResponse<List<UserResponse>> findAll() {
        return ApiResponse.success(userService.findAll());
    }

    @ApiOperation(value = "사용자 단건 조회", notes = "존재하지 않는 id면 404.")
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(userService.findById(id));
    }

    @ApiOperation(
        value = "회원가입",
        notes = "verificationToken이 유효해야 가입할 수 있다. 이미 가입된 전화번호면 409. "
            + "role이 WARD면 지갑도 함께 생성된다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @ApiOperation(value = "사용자 정보 수정", notes = "존재하지 않는 id면 404.")
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.success(userService.update(id, request));
    }

    @ApiOperation(value = "사용자 삭제", notes = "존재하지 않는 id면 404.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success(null);
    }

    @ApiOperation(
        value = "FCM 토큰 등록",
        notes = "로그인한 사용자 본인의 토큰만 갱신한다. 기기에서 토큰이 재발급될 때마다 호출한다. "
            + "탈퇴한 계정이면 403.")
    @PutMapping("/me/fcm-token")
    public ApiResponse<Void> updateFcmToken(
        @Valid @RequestBody FcmTokenUpdateRequest request,
        Authentication authentication
    ) {
        userService.updateFcmToken(currentUserId(authentication), request.getFcmToken());
        return ApiResponse.success(null);
    }

    @ApiOperation(
        value = "FCM 토큰 해제",
        notes = "로그아웃 시 호출해 발송 대상에서 제외한다. 해제할 토큰을 함께 보내야 하며, "
            + "현재 등록된 토큰과 같을 때만 지운다. 다른 기기에서 이미 새 토큰이 등록된 뒤라면 "
            + "아무것도 지우지 않고 성공으로 응답한다 — 지금 쓰는 기기의 알림이 끊기면 안 된다.")
    @DeleteMapping("/me/fcm-token")
    public ApiResponse<Void> deleteFcmToken(
        @Valid @RequestBody FcmTokenUpdateRequest request,
        Authentication authentication
    ) {
        userService.deleteFcmToken(currentUserId(authentication), request.getFcmToken());
        return ApiResponse.success(null);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
