package com.epam.rd.autocode.assessment.appliances.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingDetails {

    private String contactFirstName;
    private String contactLastName;
    private String contactPhone;
    private String contactEmail;

    private String country;
    private String city;
    private String street;
    private String zipCode;

    private String paymentMethod;
}
