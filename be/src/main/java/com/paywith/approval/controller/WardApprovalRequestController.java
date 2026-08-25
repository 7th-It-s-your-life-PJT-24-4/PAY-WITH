package com.paywith.approval.controller;

import com.paywith.approval.dto.WardApprovalDetailResponse;
import com.paywith.approval.service.WardApprovalRequestService;
import com.paywith.common.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * 피보호자용 승인요청 조회. 목록은 홈 화면({@code /api/ward/home})이 이미 담고 있어 상세만 둔다.
 *
 * <p>보호자용 {@link ApprovalRequestController} 와 경로를 나눈 이유는, 같은 승인요청이라도 두
 * 역할이 보는 내용과 접근 권한 판정이 다르기 때문이다. 한 엔드포인트에서 역할로 분기하면 응답
 * 스키마가 역할에 따라 달라져 문서와 FE 스키마가 둘 다 애매해진다.
 */
@Api(tags = "피보호자 승인요청")
@RestController
@RequestMapping("/api/ward/approval-requests")
@RequiredArgsConstructor
public class WardApprovalRequestController {

    private final WardApprovalRequestService wardApprovalRequestService;

    @ApiOperation(
        value = "본인 승인 대기 건 상세",
        notes = "홈 화면 목록에서 고른 승인 대기 건의 상세를 반환한다. 대상을 파라미터로 받지 않고 "
            + "인증 주체로만 정하므로 다른 사람의 건은 조회할 수 없다. 존재하지 않는 ID·본인 건이 아닌 것·이미 "
            + "처리·만료된 건은 모두 404 \"승인요청을 찾을 수 없습니다.\"(응답에 code 없음)로 합쳐 응답한다. "
            + "approvalId 가 비숫자면 500(code 없음). 보류 사유(발동한 FDS 룰)는 보호자용 상세에만 담기며 여기서는 "
            + "내보내지 않는다.")
    @GetMapping("/{approvalId}")
    public ApiResponse<WardApprovalDetailResponse> findDetailByWard(
        @ApiIgnore @AuthenticationPrincipal Long userId,
        @ApiParam(value = "승인요청 ID", required = true, example = "1")
        @PathVariable Long approvalId
    ) {
        return ApiResponse.success(wardApprovalRequestService.findDetailByWard(approvalId, userId));
    }
}
