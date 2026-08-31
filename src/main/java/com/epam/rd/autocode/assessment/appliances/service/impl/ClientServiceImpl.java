package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRegisterRequest;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AlreadyExistsException;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional(readOnly = true)
    public ClientProfileResponse getClientById(Long id) {
        return ClientProfileResponse.fromEntity(clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client with id " + id + " not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientSummaryResponse> getAllClients(Pageable pageable) {
        return clientRepository.findAll(pageable)
                .map(ClientSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientSummaryResponse> getClientsPage(String keyword, Pageable pageable) {
        Page<Client> clients;
        if (keyword == null) {
            clients = clientRepository.findAll(pageable);
        } else {
            clients = clientRepository.searchClients(keyword, pageable);
        }

        return clients.map(ClientSummaryResponse::fromEntity);
    }

    @Override
    @Transactional
    public void saveClient(ClientUpdateRequest request) {
        Client client;
        if (request.id() != null) {
            client = clientRepository.findById(request.id())
                    .orElseThrow(() -> new NotFoundException("Client with id " + request.id() + " not found"));

            if (!client.getEmail().equals(request.email())) {
                throw new AlreadyExistsException("User with email " + request.email() + " already exists");
            }
        } else {
            if (userRepository.findByEmail(request.email()).isPresent()) {
                throw new AlreadyExistsException("User with email " + request.email() + " already exists");
            }

            client = new Client();
            client.setRole(Role.ROLE_CLIENT);

            if (request.cart() == null || request.cart().isBlank()) {
                String uniqueCart;
                do {
                    uniqueCart = generateClientCard();
                } while (clientRepository.existsByCart(uniqueCart));
                client.setCart(uniqueCart);
            }
        }

        if (request.firstName() != null) client.setFirstName(request.firstName());
        if (request.lastName() != null) client.setLastName(request.lastName());
        if (request.middleName() != null) client.setMiddleName(request.middleName());
        if (request.email() != null) client.setEmail(request.email());
        if (request.birthday() != null) client.setBirthday(request.birthday());

        client.setEnabled(request.enabled());

        if (request.cart() != null && !request.cart().isBlank()) {
            client.setCart(request.cart());
        }

        if (request.password() != null && !request.password().isBlank()) {
            client.setPassword(passwordEncoder.encode(request.password()));
        }
        clientRepository.save(client);
    }

    @Override
    @Transactional
    public void createClient(ClientRegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new AlreadyExistsException("User with email " + request.email() + " already exists");
        }

        String uniqueClientCard;

        do {
            uniqueClientCard = generateClientCard();
        } while (clientRepository.existsByCart(uniqueClientCard));

        Client client = Client.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .middleName(request.middleName())
                .email(request.email())
                .birthday(request.birthday())
                .password(passwordEncoder.encode(request.password()))
                .cart(uniqueClientCard)
                .role(Role.ROLE_CLIENT)
                .enabled(true)
                .build();

        Client savedClient = clientRepository.save(client);

        ClientProfileResponse.fromEntity(savedClient);
    }

    private String generateClientCard() {
        int rawInt = UUID.randomUUID().hashCode();

        String padded = String.format("%08d", Math.abs(rawInt % 100000000));

        return padded.substring(0, 4) + "-" + padded.substring(4, 8);
    }

}
