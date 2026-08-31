package com.epam.rd.autocode.assessment.appliances.dto.manufacturer;

import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;

import java.time.LocalDateTime;

public record ManufacturerResponse (
        Long id,
        String name,
        LocalDateTime createdAt
) {

    public static ManufacturerResponse fromEntity(Manufacturer manufacturer) {
        return new ManufacturerResponse(
                manufacturer.getId(),
                manufacturer.getName(),
                manufacturer.getCreatedAt()
        );
    }

    public static ManufacturerResponse empty() {
        return new ManufacturerResponse(null, null, null);
    }
}
