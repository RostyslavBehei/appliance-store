package com.epam.rd.autocode.assessment.appliances.dto.orders;

import com.epam.rd.autocode.assessment.appliances.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        long id,
        long userId,
        boolean status,
        BigDecimal amount,
        LocalDateTime createdAt
) {

    public static OrderSummaryResponse fromEntity(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getClient().getId(),
                order.getApproved(),
                order.getTotalPrice(),
                order.getCreatedAt()
        );
    }
}
