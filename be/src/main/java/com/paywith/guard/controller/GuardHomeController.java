package com.paywith.guard.controller;

import com.paywith.common.ApiResponse;
import com.paywith.guard.dto.GuardHomeResponse;
import com.paywith.guard.service.GuardHomeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guard")
public class GuardHomeController {

    private final GuardHomeService guardHomeService;

    public GuardHomeController(GuardHomeService guardHomeService) {
        this.guardHomeService = guardHomeService;
    }

    @GetMapping
    public ApiResponse<GuardHomeResponse> getHome(
        @AuthenticationPrincipal Long guardId,
        @RequestParam(required = false) Long wardId
    ) {
        return ApiResponse.success(guardHomeService.getHome(guardId, wardId));
    }
}