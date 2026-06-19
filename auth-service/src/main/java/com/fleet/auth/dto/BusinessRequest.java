package com.fleet.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BusinessRequest(
        @NotBlank(message = "Organization name is required")
        @Size(max = 160, message = "Organization name must be at most 160 characters")
        String name,
        @Size(max = 80, message = "Registration number must be at most 80 characters")
        String registrationNumber,
        @Email(message = "Contact email must be valid")
        @Size(max = 160, message = "Contact email must be at most 160 characters")
        String contactEmail,
        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,
        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,
        Boolean active
) {
}
