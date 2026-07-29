package com.paywith.charge.controller;

import com.paywith.charge.dto.ChargeRequest;
import com.paywith.charge.dto.ChargeResponse;
import com.paywith.charge.service.ChargeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/guard/wards/{wardId}/charges")
@RequiredArgsConstructor
public class GuardChargeController {

    private final ChargeService chargeService;

    @PostMapping
    public ResponseEntity<ChargeResponse> chargeByGuard(
            @AuthenticationPrincipal Long guardId,
            @PathVariable Long wardId,
            @RequestBody ChargeRequest request
    ){
        ChargeResponse response = chargeService.chargeByGuard(guardId, wardId, request);
        return  ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
