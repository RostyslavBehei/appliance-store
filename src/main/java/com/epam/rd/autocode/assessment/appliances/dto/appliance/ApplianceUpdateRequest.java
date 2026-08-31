package com.epam.rd.autocode.assessment.appliances.dto.appliance;

import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.model.enums.PowerType;

import java.math.BigDecimal;

public record ApplianceUpdateRequest(
        Long id,
        String name,
        Category category,
        String model,
        Long manufacturerId,
        PowerType powerType,
        String characteristic,
        String description,
        Integer power,
        BigDecimal price
) {
}