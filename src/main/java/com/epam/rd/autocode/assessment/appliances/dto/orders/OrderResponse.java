package com.epam.rd.autocode.assessment.appliances.dto.orders;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orderRow.OrderRowResponse;
import com.epam.rd.autocode.assessment.appliances.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record OrderResponse(
        Long id,
        Set<OrderRowResponse> orderRowResponses,
        BigDecimal totalPrice,
        Boolean approved,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderRowSet().stream()
                        .map(orderRow -> new OrderRowResponse(
                                new ApplianceResponse(
                                        orderRow.getAppliance().getId(),
                                        orderRow.getAppliance().getName(),
                                        orderRow.getAppliance().getCategory().name(),
                                        orderRow.getAppliance().getModel(),
                                        orderRow.getAppliance().getManufacturer().getName(),
                                        orderRow.getAppliance().getPowerType().name(),
                                        orderRow.getAppliance().getCharacteristic(),
                                        orderRow.getAppliance().getDescription(),
                                        orderRow.getAppliance().getPower(),
                                        orderRow.getAppliance().getPrice()
                                ),
                                orderRow.getNumber(),
                                orderRow.getAmount()
                        )).collect(Collectors.toSet()),
                order.getTotalPrice(),
                order.getApproved(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
