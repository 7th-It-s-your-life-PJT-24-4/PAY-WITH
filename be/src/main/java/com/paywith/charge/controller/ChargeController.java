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
@RequestMapping("/ward/charges")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;

    @PostMapping
    public ResponseEntity<ChargeResponse> charge(
            @AuthenticationPrincipal Long userId,
            @RequestBody ChargeRequest request
            ){
        ChargeResponse response = chargeService.charge(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

}
