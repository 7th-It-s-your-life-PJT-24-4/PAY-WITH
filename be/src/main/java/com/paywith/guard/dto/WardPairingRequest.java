package com.paywith.guard.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WardPairingRequest {

    @NotBlank
    private String pairingCode;
}