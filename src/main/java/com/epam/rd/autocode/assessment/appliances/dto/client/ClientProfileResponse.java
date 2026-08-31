package com.epam.rd.autocode.assessment.appliances.dto.client;

import com.epam.rd.autocode.assessment.appliances.dto.user.UserProfileResponse;
import com.epam.rd.autocode.assessment.appliances.model.Client;

import java.time.LocalDate;

public record ClientProfileResponse (
        Long id,
        String firstName,
        String lastName,
        String middleName,
        String email,
        LocalDate birthday,
        String role,
        String cart,
        Boolean enabled
) implements UserProfileResponse {

    public static ClientProfileResponse fromEntity(Client client) {
        return new ClientProfileResponse (
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getMiddleName(),
                client.getEmail(),
                client.getBirthday(),
                client.getRole().name(),
                client.getCart(),
                client.getEnabled()
        );
    }

    public static ClientProfileResponse empty() {
        return new ClientProfileResponse(null, null, null, null, null, null, null, null, false);
    }
}
