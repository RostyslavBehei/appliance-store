package com.epam.rd.autocode.assessment.appliances.dto.appliance;

import com.epam.rd.autocode.assessment.appliances.model.Appliance;

import java.math.BigDecimal;

public record ApplianceResponse(
        Long id,
        String name,
        String category,
        String model,
        String manufacturerName,
        String powerType,
        String characteristic,
        String description,
        Integer power,
        BigDecimal price
) {

    public static ApplianceResponse fromEntity(Appliance appliance) {
        return new ApplianceResponse(
                appliance.getId(),
                appliance.getName(),
                appliance.getCategory().name(),
                appliance.getModel(),
                appliance.getManufacturer().getName(),
                appliance.getPowerType().name(),
                appliance.getCharacteristic(),
                appliance.getDescription(),
                appliance.getPower(),
                appliance.getPrice()
        );
    }

    public static ApplianceResponse empty() {
        return new ApplianceResponse(null, null, null, null, null, null, null, null, null, null);
    }
}
