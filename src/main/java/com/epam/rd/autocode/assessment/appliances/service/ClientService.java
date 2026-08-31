package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRegisterRequest;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {
    ClientProfileResponse getClientById(Long id);
    Page<ClientSummaryResponse> getAllClients(Pageable pageable);
    Page<ClientSummaryResponse> getClientsPage(String keyword, Pageable pageable);

    void saveClient(ClientUpdateRequest request);
    void createClient(ClientRegisterRequest request);
}
