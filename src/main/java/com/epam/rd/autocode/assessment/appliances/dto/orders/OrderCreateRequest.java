package com.epam.rd.autocode.assessment.appliances.dto.orders;

import com.epam.rd.autocode.assessment.appliances.dto.orderRow.OrderRowRequest;

import java.util.Set;

public record OrderCreateRequest(
        Set<OrderRowRequest> items
) {
}
