package com.paywith.dto;

import javax.validation.constraints.NotBlank;

public class UserUpdateRequest {

    @NotBlank
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
