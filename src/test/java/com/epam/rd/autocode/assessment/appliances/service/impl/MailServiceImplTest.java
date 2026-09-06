package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orderRow.OrderRowResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.shippingDetails.ShippingDetailsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private MailServiceImpl mailService;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    private final String fromEmail = "store@test.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "fromEmail", fromEmail);

        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @Test
    @DisplayName("Send Welcome Message - Should create and send correct email")
    void sendWelcomeMessage_ShouldCreateAndSendEmail() {
        String toEmail = "client@test.com";
        String firstName = "John";
        String confirmationUrl = "http://localhost/verify";

        when(messageSource.getMessage(eq("mail.welcome.subject"), isNull(), any(Locale.class)))
                .thenReturn("Welcome Subject");
        when(messageSource.getMessage(eq("mail.welcome.text"), any(Object[].class), any(Locale.class)))
                .thenReturn("Welcome Text John http://localhost/verify");

        mailService.sendWelcomeMessage(toEmail, firstName, confirmationUrl);

        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(fromEmail, capturedMessage.getFrom());
        assertEquals(toEmail, Objects.requireNonNull(capturedMessage.getTo())[0]);
        assertEquals("Welcome Subject", capturedMessage.getSubject());
        assertEquals("Welcome Text John http://localhost/verify", capturedMessage.getText());
    }

    @Test
    @DisplayName("Send Welcome Message - Should catch MailException and not rethrow")
    void sendWelcomeMessage_ShouldHandleMailExceptionGracefully() {
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> mailService.sendWelcomeMessage("client@test.com", "John", "http://url"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Send Password Reset Message - Should create and send correct email")
    void sendPasswordResetMessage_ShouldCreateAndSendEmail() {
        String toEmail = "user@test.com";
        String resetUrl = "http://localhost/reset";

        when(messageSource.getMessage(eq("mail.reset.subject"), isNull(), any(Locale.class)))
                .thenReturn("Reset Subject");
        when(messageSource.getMessage(eq("mail.reset.text"), any(Object[].class), any(Locale.class)))
                .thenReturn("Reset Text http://localhost/reset");

        mailService.sendPasswordResetMessage(toEmail, resetUrl);

        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(fromEmail, capturedMessage.getFrom());
        assertEquals(toEmail, Objects.requireNonNull(capturedMessage.getTo())[0]);
        assertEquals("Reset Subject", capturedMessage.getSubject());
        assertEquals("Reset Text http://localhost/reset", capturedMessage.getText());
    }

    @Test
    @DisplayName("Send Password Reset Message - Should catch MailException and not rethrow")
    void sendPasswordResetMessage_ShouldHandleMailExceptionGracefully() {
        doThrow(new MailSendException("Connection timeout")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> mailService.sendPasswordResetMessage("user@test.com", "http://reset"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Send Order Message - Should format and send complete order email")
    void sendOrderMessage_ShouldFormatAndSendCompleteEmail() {
        String toEmail = "client@test.com";

        ShippingDetailsResponse shipping = new ShippingDetailsResponse(
                "John",
                "Doe",
                "client@test.com",
                "+380980000000",
                "Ukraine",
                "Lviv",
                "Volodymyra Velykoho",
                "79000",
                "CASH"
        );

        ApplianceResponse appliance = new ApplianceResponse(
                1L,
                "Kettle",
                "SMALL",
                "TWK-7800",
                "Bosch",
                "CORDED",
                "2200W",
                "Electric kettle",
                2200,
                BigDecimal.valueOf(50.00)
        );

        OrderRowResponse row = new OrderRowResponse(
                appliance,
                2L,
                BigDecimal.valueOf(100.00)
        );

        OrderResponse orderResponse = new OrderResponse(
                10L,
                Set.of(row),
                BigDecimal.valueOf(100.00),
                false,
                shipping,
                LocalDateTime.of(2026, 9, 5, 14, 30),
                LocalDateTime.of(2026, 9, 5, 14, 30)
        );

        when(messageSource.getMessage(eq("mail.order.subject"), any(Object[].class), any(Locale.class)))
                .thenReturn("Order #10 Confirmation");

        mailService.sendOrderMessage(toEmail, orderResponse);

        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(fromEmail, capturedMessage.getFrom());
        assertEquals(toEmail, Objects.requireNonNull(capturedMessage.getTo())[0]);
        assertEquals("Order #10 Confirmation", capturedMessage.getSubject());
        assertNotNull(capturedMessage.getText());
        assertTrue(capturedMessage.getText().contains("mail.order.header"));
        assertTrue(capturedMessage.getText().contains("mail.order.item.format"));
        assertTrue(capturedMessage.getText().contains("mail.order.shipping"));
    }

    @Test
    @DisplayName("Send Order Message - Should catch MailException and not rethrow")
    void sendOrderMessage_ShouldHandleMailExceptionGracefully() {
        OrderResponse orderResponse = new OrderResponse(
                10L,
                Collections.emptySet(),
                BigDecimal.TEN,
                false,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        doThrow(new MailSendException("SMTP unreachable")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> mailService.sendOrderMessage("client@test.com", orderResponse));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}