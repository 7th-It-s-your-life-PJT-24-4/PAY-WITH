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
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "사용자")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @ApiOperation(
        value = "전체 사용자 조회",
        notes = "인증만 필요하며 역할·본인 여부 검사는 없다. 탈퇴(WITHDRAWN) 사용자를 포함한 전체 목록을 "
            + "user_id 내림차순으로 반환한다(phone 포함).")
    @ApiIgnore // 소유권 검사 미구현 — 공개 Swagger 에서 제외 (코드 수정 후보 C16)
    @GetMapping
    public ApiResponse<List<UserResponse>> findAll() {
        return ApiResponse.success(userService.findAll());
    }

    @ApiOperation(
        value = "사용자 단건 조회",
        notes = "인증만 필요하며 본인 여부 검사는 없다(타인 id 조회 가능). 존재하지 않는 id면 404(code 없음). "
            + "탈퇴(WITHDRAWN) 사용자도 조회된다. 숫자가 아닌 id는 500.")
    @ApiIgnore // 소유권 검사 미구현 — 공개 Swagger 에서 제외 (코드 수정 후보 C16)
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(userService.findById(id));
    }

    @ApiOperation(
        value = "회원가입",
        notes = "인증번호 확인으로 받은 verificationToken(32자 hex)이 유효해야 가입할 수 있다. 성공 시 201. "
            + "전화번호는 비숫자를 제거한 뒤 처리하며 형식 검증은 없다(인증번호 발송 시 이미 검증된 번호에만 "
            + "토큰이 발급된다). avatarId 생략 시 1. role이 WARD면 지갑도 함께 생성된다. "
            + "검사 순서: 필수값 누락·공백, password 8자 미만, birthDate가 숫자 8자리가 아님, gender가 남/여가 "
            + "아님, avatarId가 1~6 밖, paymentPassword가 숫자 6자리가 아님은 400 REQUEST_001 → "
            + "verificationToken이 없거나 만료됐거나 다른 전화번호로 발급된 것이면 400 AUTH_003 → "
            + "이미 가입된 전화번호면 409(code 없음) → role이 WARD·GUARD가 아니면 400(code 없음) → "
            + "달력상 존재하지 않는 생년월일(예: 20260231)은 400(code 없음). "
            + "name은 서버 길이 검증이 없어 50자를 넘으면 DB 제약으로 500. JSON 파싱 실패는 500.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @ApiOperation(
        value = "사용자 정보 수정",
        notes = "name·avatarId만 수정한다. 인증만 필요하며 본인 여부 검사는 없다(타인 id 수정 가능). "
            + "name 누락·공백, avatarId가 1~6 밖이면 400 REQUEST_001. avatarId 생략 시 기존 값 유지. "
            + "존재하지 않는 id면 404(code 없음). 숫자가 아닌 id·JSON 파싱 실패는 500.")
    @ApiIgnore // 소유권 검사 미구현 — 공개 Swagger 에서 제외 (코드 수정 후보 C16)
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.success(userService.update(id, request));
    }

    @ApiOperation(
        value = "사용자 삭제",
        notes = "탈퇴 처리(soft delete): status를 WITHDRAWN으로 바꾸고 FCM 토큰을 지운다. 성공 시 200, data null. "
            + "인증만 필요하며 본인 여부 검사는 없다(타인 id 탈퇴 가능). 존재하지 않거나 이미 탈퇴한 id면 "
            + "404(code 없음). 이미 발급된 JWT는 만료까지 유효하다. 숫자가 아닌 id는 500.")
    @ApiIgnore // 소유권 검사 미구현 — 공개 Swagger 에서 제외 (코드 수정 후보 C16)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success(null);
    }

    @ApiOperation(
        value = "FCM 토큰 등록",
        notes = "로그인한 사용자 본인의 토큰만 갱신한다(사용자당 토큰 1개, 덮어쓰기). 기기에서 토큰이 "
            + "재발급될 때마다 호출한다. 같은 토큰이 다른 사용자에게 등록돼 있으면 그쪽 등록을 먼저 해제한다. "
            + "성공 시 200, data null. fcmToken 누락·공백 또는 255자 초과는 400 REQUEST_001. "
            + "탈퇴한 계정이면 403(code 없음). JSON 파싱 실패는 500.")
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
            + "아무것도 지우지 않고 성공으로 응답한다 — 지금 쓰는 기기의 알림이 끊기면 안 된다. "
            + "성공 시 200, data null(멱등). fcmToken 누락·공백 또는 255자 초과는 400 REQUEST_001. "
            + "탈퇴 여부는 검사하지 않는다(403 없음). JSON 파싱 실패는 500.")
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
