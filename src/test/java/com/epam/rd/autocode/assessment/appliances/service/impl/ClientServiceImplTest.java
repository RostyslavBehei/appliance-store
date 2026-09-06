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
import com.epam.rd.autocode.assessment.appliances.service.MailService;
import com.epam.rd.autocode.assessment.appliances.service.VerificationTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client testClient;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testClient = Client.builder()
                .id(1L)
                .email("client@test.com")
                .firstName("John")
                .lastName("Doe")
                .middleName("Smith")
                .cart("1234-5678")
                .birthday(LocalDate.of(1995, 5, 15))
                .role(Role.ROLE_CLIENT)
                .enabled(true)
                .build();

        pageable = PageRequest.of(0, 10);

        ReflectionTestUtils.setField(clientService, "BASE_URL", "http://localhost:8080");

        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @Test
    @DisplayName("Get Client By Id - Should successfully return client profile")
    void getClientById_ShouldReturnClient() {
        Long id = 1L;
        when(clientRepository.findById(id)).thenReturn(Optional.of(testClient));

        ClientProfileResponse result = clientService.getClientById(id);

        assertNotNull(result);
        assertEquals("client@test.com", result.email());
        verify(clientRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Get Client By Id - Should throw NotFoundException if client does not exist")
    void getClientById_ShouldThrowNotFoundExceptionIfNotExist() {
        Long id = 99L;
        when(clientRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> clientService.getClientById(id));

        assertEquals("error.client.not.found", result.getMessage());
        verify(clientRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Get All Clients - Should return all clients page")
    void getAllClients_ShouldReturnAllClients() {
        Page<Client> clientPage = new PageImpl<>(List.of(testClient));
        when(clientRepository.findAll(pageable)).thenReturn(clientPage);

        Page<ClientSummaryResponse> result = clientService.getAllClients(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("client@test.com", result.getContent().get(0).email());
        verify(clientRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Get Clients Page - Should search by keyword when keyword is provided")
    void getClientsPage_ShouldSearchByKeyword() {
        String keyword = "John";
        Page<Client> clientPage = new PageImpl<>(List.of(testClient));

        when(clientRepository.searchClients(keyword, pageable)).thenReturn(clientPage);

        Page<ClientSummaryResponse> result = clientService.getClientsPage(keyword, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(clientRepository, times(1)).searchClients(keyword, pageable);
        verify(clientRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Get Clients Page - Should return all clients when keyword is null")
    void getClientsPage_ShouldReturnAllIfKeywordNull() {
        Page<Client> clientPage = new PageImpl<>(List.of(testClient));

        when(clientRepository.findAll(pageable)).thenReturn(clientPage);

        Page<ClientSummaryResponse> result = clientService.getClientsPage(null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(clientRepository, times(1)).findAll(pageable);
        verify(clientRepository, never()).searchClients(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Save Client - Should successfully update existing client without changing email")
    void saveClient_ShouldUpdateExistingClient_WhenEmailUnchanged() {
        ClientUpdateRequest request = new ClientUpdateRequest(
                1L,
                "UpdatedFirst",
                "UpdatedLast",
                "UpdatedMiddle",
                "client@test.com",
                "newPass",
                LocalDate.of(1996, 6, 20),
                "1234-5678",
                true
        );

        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");

        clientService.saveClient(request);

        assertEquals("UpdatedFirst", testClient.getFirstName());
        assertEquals("UpdatedLast", testClient.getLastName());
        assertEquals("encodedNewPass", testClient.getPassword());
        verify(clientRepository, times(1)).save(testClient);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Save Client - Should throw NotFoundException if client to update not found")
    void saveClient_ShouldThrowNotFoundException_WhenUpdatingNonExistingClient() {
        ClientUpdateRequest request = new ClientUpdateRequest(
                99L,
                "Alice",
                "Smith",
                null,
                "alice@test.com",
                "secret123",
                LocalDate.of(2000, 1, 1),
                null,
                true
        );

        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> clientService.saveClient(request));

        assertEquals("error.client.not.found", result.getMessage());
        verify(clientRepository, times(1)).findById(99L);
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save Client - Should throw AlreadyExistsException if new email is taken during update")
    void saveClient_ShouldThrowAlreadyExistsExceptionIfEmailTakenOnUpdate() {
        ClientUpdateRequest request = new ClientUpdateRequest(
                1L,
                "John",
                "Doe",
                null,
                "taken@test.com",
                null,
                null,
                null,
                true
        );

        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new Client()));

        AlreadyExistsException result = assertThrows(AlreadyExistsException.class, () -> clientService.saveClient(request));

        assertEquals("error.user.email.exists", result.getMessage());
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    @DisplayName("Save Client - Should throw AlreadyExistsException if custom cart number is used by another client")
    void saveClient_ShouldThrowAlreadyExistsException_WhenCartAssignedToAnotherClient() {
        Client otherClient = Client.builder().id(2L).cart("9999-8888").build();

        ClientUpdateRequest cartConflictRequest = new ClientUpdateRequest(
                1L,
                "John",
                "Doe",
                null,
                "client@test.com",
                null,
                null,
                "9999-8888",
                true
        );

        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(clientRepository.findByCart("9999-8888")).thenReturn(Optional.of(otherClient));

        AlreadyExistsException result = assertThrows(AlreadyExistsException.class, () -> clientService.saveClient(cartConflictRequest));

        assertEquals("This cart number is already in use by another client", result.getMessage());
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save Client - Should successfully create new client via admin panel")
    void saveClient_ShouldCreateNewClient() {
        ClientUpdateRequest request = new ClientUpdateRequest(
                null,
                "Alice",
                "Smith",
                null,
                "alice@test.com",
                "secret123",
                LocalDate.of(2000, 1, 1),
                null,
                true
        );

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encodedSecret");
        when(clientRepository.existsByCart(anyString())).thenReturn(false);

        clientService.saveClient(request);

        verify(clientRepository, times(1)).save(argThat(client ->
                client.getEmail().equals("alice@test.com") &&
                        client.getFirstName().equals("Alice") &&
                        client.getPassword().equals("encodedSecret") &&
                        client.getRole() == Role.ROLE_CLIENT &&
                        client.getCart() != null
        ));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Save Client - Should throw IllegalArgumentException if password is empty when creating client")
    void saveClient_ShouldThrowIllegalArgumentException_WhenPasswordEmptyOnCreate(String emptyPassword) {
        ClientUpdateRequest request = new ClientUpdateRequest(
                null,
                "Alice",
                "Smith",
                null,
                "alice@test.com",
                emptyPassword,
                LocalDate.of(2000, 1, 1),
                null,
                true
        );

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());

        IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> clientService.saveClient(request));

        assertEquals("Password is required for new client", result.getMessage());
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Save Client - Should throw AlreadyExistsException if email taken during creation")
    void saveClient_ShouldThrowAlreadyExistsExceptionIfEmailTakenOnCreate() {
        ClientUpdateRequest request = new ClientUpdateRequest(
                null,
                "Alice",
                "Smith",
                null,
                "taken@test.com",
                "secret123",
                null,
                null,
                true
        );

        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new Client()));

        AlreadyExistsException result = assertThrows(AlreadyExistsException.class, () -> clientService.saveClient(request));

        assertEquals("error.user.email.exists", result.getMessage());
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    @DisplayName("Create Client (Register) - Should register client, generate token and send welcome email")
    void createClient_ShouldRegisterClientAndSendEmail() {
        ClientRegisterRequest request = new ClientRegisterRequest(
                "John",
                "Doe",
                "Smith",
                "new@test.com",
                LocalDate.of(1998, 3, 25),
                "rawPassword"
        );

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(clientRepository.existsByCart(anyString())).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        when(clientRepository.save(any(Client.class))).thenReturn(testClient);
        when(verificationTokenService.generateToken(testClient, TokenAction.VERIFY_ACCOUNT, 60))
                .thenReturn("mockedToken");

        clientService.createClient(request);

        verify(clientRepository, times(1)).save(argThat(client ->
                client.getEmail().equals("new@test.com") &&
                        !client.getEnabled() &&
                        client.getRole() == Role.ROLE_CLIENT
        ));
        verify(verificationTokenService, times(1)).generateToken(testClient, TokenAction.VERIFY_ACCOUNT, 60);
        verify(mailService, times(1)).sendWelcomeMessage(
                eq("client@test.com"),
                eq("John"),
                eq("http://localhost:8080/verify?token=mockedToken")
        );
    }

    @Test
    @DisplayName("Create Client (Register) - Should throw AlreadyExistsException if email is taken")
    void createClient_ShouldThrowAlreadyExistsExceptionIfEmailTaken() {
        ClientRegisterRequest request = new ClientRegisterRequest(
                "John",
                "Doe",
                null,
                "taken@test.com",
                null,
                "password"
        );

        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new Client()));

        AlreadyExistsException result = assertThrows(AlreadyExistsException.class, () -> clientService.createClient(request));

        assertEquals("error.user.email.exists", result.getMessage());
        verify(clientRepository, never()).save(any(Client.class));
        verify(mailService, never()).sendWelcomeMessage(anyString(), anyString(), anyString());
    }
}