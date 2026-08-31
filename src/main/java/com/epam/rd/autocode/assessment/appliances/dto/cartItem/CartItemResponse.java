package com.epam.rd.autocode.assessment.appliances.dto.cartItem;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.model.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        ApplianceSummaryResponse appliance,
        Integer quantity,
        BigDecimal sum
) {

    public static CartItemResponse fromEntity(CartItem cartItem) {
        return new CartItemResponse(
                cartItem.getId(),
                ApplianceSummaryResponse.fromEntity(cartItem.getAppliance()),
                cartItem.getQuantity(),
                cartItem.getAppliance().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
        );
    }
}
