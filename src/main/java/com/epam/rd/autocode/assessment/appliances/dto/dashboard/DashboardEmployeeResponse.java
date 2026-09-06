package com.epam.rd.autocode.assessment.appliances.dto.dashboard;

import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderSummaryResponse;

import java.util.List;

public record DashboardEmployeeResponse(
    long pendingOrdersCount,
    long approvedOrdersCount,
    long totalAppliances,
    long totalManufacturers,
    List<OrderSummaryResponse> recentOrders,
    long totalBigAppliances,
    long totalSmallAppliances
) {
}
