package com.paywith.guard.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class GuardHomeResponse {

    private final List<WardTabResponse> wards;
    private final SelectedWardResponse selectedWard;

    public GuardHomeResponse(List<WardTabResponse> wards, SelectedWardResponse selectedWard) {
        this.wards = wards;
        this.selectedWard = selectedWard;
    }
}