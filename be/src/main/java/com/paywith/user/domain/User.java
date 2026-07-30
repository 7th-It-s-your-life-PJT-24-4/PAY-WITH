package com.paywith.user.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {

    private Long id;
    private Role role;
    private String phone;
    private String password;
    private String name;
    private LocalDate birthDate;
    private String gender;
    private String pin;
    private String fcmToken;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
