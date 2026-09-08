package com.d5wq.burger.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 4, max = 64) String password,
        @NotBlank @Size(max = 50) String name) {
}
