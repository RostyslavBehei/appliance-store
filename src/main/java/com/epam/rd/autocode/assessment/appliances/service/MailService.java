package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;

public interface MailService {
    void sendWelcomeMessage(String toEmail, String firstName, String confirmationUrl);

    void sendPasswordResetMessage(String toEmail, String resetUrl);

    void sendOrderMessage(String toEmail, OrderResponse orderResponse);
}
