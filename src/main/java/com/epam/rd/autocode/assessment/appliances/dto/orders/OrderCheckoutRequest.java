package com.epam.rd.autocode.assessment.appliances.dto.orders;

import jakarta.validation.constraints.NotBlank;

public record OrderCheckoutRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String email,
        @NotBlank String phone,
        @NotBlank String country,
        @NotBlank String city,
        @NotBlank String street,
        @NotBlank String zipCode,
        @NotBlank String paymentMethod
) {
}