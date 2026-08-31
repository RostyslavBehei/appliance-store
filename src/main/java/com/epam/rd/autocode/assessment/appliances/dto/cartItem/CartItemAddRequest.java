package com.epam.rd.autocode.assessment.appliances.dto.cartItem;

import jakarta.validation.constraints.*;

public record CartItemAddRequest(
        @NotBlank(message = "{card.client.email.notBlank}")
        @Email(message = "{card.client.email.email}")
        String clientEmail,

        @NotNull(message = "{card.applianceId.noNull}")
        Long applianceId,

        @Min(value = 1, message = "{card.quantity.size.min}")
        int quantity
) {
}
