package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRegisterRequest;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AlreadyExistsException;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import com.epam.rd.autocode.assessment.appliances.model.enums.TokenAction;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.MailService;
import com.epam.rd.autocode.assessment.appliances.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final PasswordEncoder passwordEncoder;

    private final MailService mailService;
    private final VerificationTokenService verificationTokenService;

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    private final MessageSource messageSource;

    @Value("${app.base-url}")
    private String BASE_URL;

    @Override
    @Transactional(readOnly = true)
    public ClientProfileResponse getClientById(Long id) {
        log.debug("Fetching client profile by id: '{}'", id);
        return ClientProfileResponse.fromEntity(clientRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Client not found with id: '{}'", id);
                    return new NotFoundException(messageSource.getMessage("error.client.not.found", new Object[]{id}, getLocale()));
                }));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientSummaryResponse> getAllClients(Pageable pageable) {
        log.debug("Fetching all clients with pagination: pageNumber='{}', pageSize={}", pageable.getPageNumber(), pageable.getPageSize());
        return clientRepository.findAll(pageable)
                .map(ClientSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientSummaryResponse> getClientsPage(String keyword, Pageable pageable) {
        log.debug("Searching client page: keyword='{}', pageNumber={}", keyword, pageable.getPageNumber());
        Page<Client> clients;

        if (keyword == null || keyword.trim().isBlank()) {
            clients = clientRepository.findAll(pageable);
        } else {
            clients = clientRepository.searchClients(keyword.trim(), pageable);
        }

        return clients.map(ClientSummaryResponse::fromEntity);
    }

    @Override
    @Transactional
    public void saveClient(ClientUpdateRequest request) {
        boolean isNew = request.id() == null;
        Client client;

        if (!isNew) {
            client = clientRepository.findById(request.id())
                    .orElseThrow(() -> {
                        log.warn("Client not found with id: '{}'", request.id());
                        return new NotFoundException(messageSource.getMessage("error.client.not.found", new Object[]{request.id()}, getLocale()));
                    });

            if (!client.getEmail().equals(request.email()) && userRepository.findByEmail(request.email()).isPresent()) {
                log.warn("Failed to update client id='{}': Email {} is already in use", request.id(), request.email());
                throw new AlreadyExistsException(
                        messageSource.getMessage("error.user.email.exists", new Object[]{request.email()}, getLocale())
                );
            }
        } else {
            log.info("Creating client vie admin panel with email: '{}'", request.email());

            if (userRepository.findByEmail(request.email()).isPresent()) {
                log.warn("Failed to create client with email='{}': Email already exists", request.email());
                throw new AlreadyExistsException(
                        messageSource.getMessage("error.user.email.exists", new Object[]{request.email()}, getLocale())
                );
            }

            if (request.password() == null || request.password().isBlank()) {
                log.warn("Failed to create client: Password is empty for email '{}'", request.email());
                throw new IllegalArgumentException("Password is required for new client");
            }

            client = new Client();
            client.setRole(Role.ROLE_CLIENT);
        }

        if (request.firstName() != null) client.setFirstName(request.firstName());
        if (request.lastName() != null) client.setLastName(request.lastName());
        if (request.middleName() != null) client.setMiddleName(request.middleName());
        if (request.email() != null) client.setEmail(request.email());
        if (request.birthday() != null) client.setBirthday(request.birthday());
        client.setEnabled(request.enabled());

        if (request.password() != null && !request.password().isBlank()) {
            client.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.cart() != null && !request.cart().isBlank()) {
            Client existingClientWithCart = clientRepository.findByCart(request.cart()).orElse(null);
            if (existingClientWithCart != null && !existingClientWithCart.getId().equals(client.getId())) {
                log.warn("Client cart conflict: Number '{}' is already assigned to client id='{}'", client.getId(), existingClientWithCart.getId());
                throw new AlreadyExistsException("This cart number is already in use by another client");
            }
            client.setCart(request.cart());
        } else if (client.getId() == null) {
            String uniqueCart;
            do {
                uniqueCart = generateClientCard();
            } while (clientRepository.existsByCart(uniqueCart));
            client.setCart(uniqueCart);
        }

        clientRepository.save(client);
        log.info("Client successfully {} with id: '{}', email: '{}', enabled: {}", isNew, client.getId(), client.getEmail(), client.getEnabled());
    }

    @Override
    @Transactional
    public void createClient(ClientRegisterRequest request) {
        log.info("Initiating client registration for email: '{}'", request.email());

        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("Registration rejected: User with email '{}' already exists", request.email());
            throw new AlreadyExistsException(
                    messageSource.getMessage("error.user.email.exists", new Object[]{request.email()}, getLocale())
            );
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
                .enabled(false)
                .build();

        Client savedClient = clientRepository.save(client);
        log.info("Client account registered (pending verification) with id: {}, card: '{}'",
                savedClient.getId(), uniqueClientCard);

        String token = verificationTokenService.generateToken(savedClient, TokenAction.VERIFY_ACCOUNT, 60);
        String verifyUrl = BASE_URL + "/verify?token=" + token;

        mailService.sendWelcomeMessage(savedClient.getEmail(), savedClient.getFirstName(), verifyUrl);
        log.info("Verification email sent to: '{}'", savedClient.getEmail());
    }

    private String generateClientCard() {
        int rawInt = UUID.randomUUID().hashCode();

        String padded = String.format("%08d", Math.abs(rawInt % 100000000));

        return padded.substring(0, 4) + "-" + padded.substring(4, 8);
    }

    private Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}