package com.paywith.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class UserCreateRequest {

    @NotBlank
    private String role;

    @NotBlank
    private String phone;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "생년월일은 YYMMDD 6자리여야 합니다.")
    private String birthDate;

    @NotBlank
    @Pattern(regexp = "[0-9]", message = "gender는 숫자 1자리여야 합니다.")
    private String gender;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
