package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Async
    @Override
    public void sendWelcomeMessage(String toEmail, String firstName, String confirmationUrl) {
        Locale locale = getLocale();
        log.info("Sending welcome message to: '{}' (locale: {})", toEmail, locale);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(messageSource.getMessage("mail.welcome.subject", null, locale));
            message.setText(messageSource.getMessage("mail.welcome.text", new Object[]{firstName, confirmationUrl}, locale));

            mailSender.send(message);
            log.info("Welcome message successfully sent to: '{}'", toEmail);
        } catch (MailException e) {
            log.error("Failed to send welcome message to '{}': {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendPasswordResetMessage(String toEmail, String resetUrl) {
        Locale locale = getLocale();
        log.info("Sending password reset message to: '{}' (locale: {})", toEmail, locale);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(messageSource.getMessage("mail.reset.subject", null, locale));
            message.setText(messageSource.getMessage("mail.reset.text", new Object[]{resetUrl}, locale));

            mailSender.send(message);
            log.info("Password reset message successfully sent to: '{}'", toEmail);
        } catch (MailException e) {
            log.error("Failed to send password reset message to '{}': {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendOrderMessage(String toEmail, OrderResponse orderResponse) {
        Locale locale = getLocale();
        log.info("Preparing order confirmation message for order id: {} to: '{}'", orderResponse.id(), toEmail);

        try {
            var shipping = orderResponse.shippingDetails();
            String firstName = (shipping != null && shipping.contactFirstName() != null)
                    ? shipping.contactFirstName()
                    : "";

            String dateStr = (orderResponse.createdAt() != null)
                    ? orderResponse.createdAt().format(DATE_FORMATTER)
                    : "-";
            String paymentMethod = (shipping != null && shipping.paymentMethod() != null)
                    ? shipping.paymentMethod()
                    : "-";

            String subject = messageSource.getMessage(
                    "mail.order.subject",
                    new Object[]{orderResponse.id()},
                    locale
            );

            StringBuilder text = new StringBuilder();

            text.append(messageSource.getMessage(
                    "mail.order.header",
                    new Object[]{firstName, orderResponse.id(), dateStr, paymentMethod},
                    locale
            )).append("\n");

            if (orderResponse.orderRowResponses() != null) {
                for (var row : orderResponse.orderRowResponses()) {
                    var appliance = row.appliance();
                    text.append(messageSource.getMessage(
                            "mail.order.item.format",
                            new Object[]{
                                    appliance.name(),
                                    appliance.manufacturerName(),
                                    appliance.model(),
                                    row.number(),
                                    appliance.price(),
                                    row.amount()
                            },
                            locale
                    )).append("\n");
                }
            }

            text.append(messageSource.getMessage(
                    "mail.order.total",
                    new Object[]{orderResponse.totalPrice()},
                    locale
            ));

            if (shipping != null) {
                text.append(messageSource.getMessage(
                        "mail.order.shipping",
                        new Object[]{
                                shipping.contactFirstName(),
                                shipping.contactLastName(),
                                shipping.contactPhone(),
                                shipping.country(),
                                shipping.city(),
                                shipping.street(),
                                shipping.zipCode()
                        },
                        locale
                ));
            }

            text.append(messageSource.getMessage("mail.order.footer", null, locale));

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text.toString());

            mailSender.send(message);
            log.info("Order confirmation message for order id: {} successfully sent to: '{}'", orderResponse.id(), toEmail);
        } catch (MailException e) {
            log.error("Failed to send order confirmation email for order id: {} to '{}': {}",
                    orderResponse.id(), toEmail, e.getMessage());
        }
    }

    private Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}