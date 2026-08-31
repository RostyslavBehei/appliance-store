package com.epam.rd.autocode.assessment.appliances.dto.orderRow;

import java.math.BigDecimal;

public record OrderRowRequest(
        Long applianceId,
        BigDecimal amound
) {
}
