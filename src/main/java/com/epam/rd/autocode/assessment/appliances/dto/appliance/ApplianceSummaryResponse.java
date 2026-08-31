package com.epam.rd.autocode.assessment.appliances.dto.appliance;

import com.epam.rd.autocode.assessment.appliances.model.Appliance;

import java.math.BigDecimal;

public record ApplianceSummaryResponse (
        Long id,
        String name,
        String model,
        String category,
        String manufacturerName,
        BigDecimal price
) {

    public static ApplianceSummaryResponse fromEntity(Appliance appliance) {
        return new ApplianceSummaryResponse(
                appliance.getId(),
                appliance.getName(),
                appliance.getModel(),
                appliance.getCategory().name(),
                appliance.getManufacturer().getName(),
                appliance.getPrice()
        );
    }
}
