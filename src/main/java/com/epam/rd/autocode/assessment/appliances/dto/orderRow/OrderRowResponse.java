package com.epam.rd.autocode.assessment.appliances.dto.orderRow;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;

import java.math.BigDecimal;

public record OrderRowResponse(
        ApplianceResponse appliance,
        Long number,
        BigDecimal amount
) {

    public static OrderRowResponse fromEntity(OrderRow orderRow) {
        return new OrderRowResponse(
                ApplianceResponse.fromEntity(orderRow.getAppliance()),
                orderRow.getNumber(),
                orderRow.getAmount()
        );
    }
}
