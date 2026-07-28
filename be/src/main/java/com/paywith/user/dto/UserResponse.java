package com.paywith.user.dto;

import com.paywith.user.domain.User;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class UserResponse {

    private final Long id;
    private final String phone;
    private final String name;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.phone = user.getPhone();
        this.name = user.getName();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}
