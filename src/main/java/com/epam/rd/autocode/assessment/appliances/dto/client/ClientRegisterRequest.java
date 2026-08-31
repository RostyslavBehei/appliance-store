package com.epam.rd.autocode.assessment.appliances.dto.client;

import java.time.LocalDate;

public record ClientRegisterRequest(
        String firstName,
        String lastName,
        String middleName,
        String email,
        LocalDate birthday,
        String password
) {

    public static ClientRegisterRequest empty() {
        return new ClientRegisterRequest(null, null, null, null, null, null);
    }
}
