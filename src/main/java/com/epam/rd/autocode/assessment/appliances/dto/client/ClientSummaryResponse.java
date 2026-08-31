package com.epam.rd.autocode.assessment.appliances.dto.client;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClientSummaryResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        LocalDate birthday,
        Role role,
        boolean enabled,
        String cart,
        LocalDateTime createdAt
) {

    public static ClientSummaryResponse fromEntity(Client client) {
        return new ClientSummaryResponse(
                client.getId(),
                client.getEmail(),
                client.getFirstName(),
                client.getLastName(),
                client.getBirthday(),
                client.getRole(),
                client.getEnabled(),
                client.getCart(),
                client.getCreatedAt()
        );
    }
}
