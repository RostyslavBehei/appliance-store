package com.epam.rd.autocode.assessment.appliances.dto.shippingDetails;

public record ShippingDetailsResponse(
        String contactFirstName,
        String contactLastName,
        String contactEmail,
        String contactPhone,
        String country,
        String city,
        String street,
        String zipCode,
        String paymentMethod
) {
}
