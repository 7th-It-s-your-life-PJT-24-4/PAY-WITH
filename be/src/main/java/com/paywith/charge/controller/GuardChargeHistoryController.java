package com.paywith.charge.controller;

import com.paywith.charge.dto.ChargeDetailResponse;
import com.paywith.charge.dto.ChargeHistoryListResponse;
import com.paywith.charge.service.ChargeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/guard/charges")
@RequiredArgsConstructor
public class GuardChargeHistoryController {

    private final ChargeService chargeService;

    @GetMapping
    public ResponseEntity<ChargeHistoryListResponse> getChargeHistories(
            @AuthenticationPrincipal Long guardId
    ){
        ChargeHistoryListResponse response = chargeService.getChargeHistories(guardId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChargeDetailResponse> getChargeDetail(
            @AuthenticationPrincipal Long guardId, @PathVariable Long id
    ){
        ChargeDetailResponse response = chargeService.getChargeDetail(guardId, id);
        return ResponseEntity.ok(response);
    }
}
