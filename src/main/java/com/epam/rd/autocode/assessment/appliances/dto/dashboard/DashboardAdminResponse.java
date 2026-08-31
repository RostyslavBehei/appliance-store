package com.epam.rd.autocode.assessment.appliances.dto.dashboard;

import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderSummaryResponse;

import java.util.List;

public record DashboardAdminResponse(
        long totalRevenue,
        double totalRevenuePercents,
        long totalOrders,
        double totalOrdersPercents,
        long activeUsers,
        double activeUsersPercents,
        long totalAppliances,
        double totalAppliancesPercents,
        List<OrderSummaryResponse> recentOrders,
        long totalBigAppliances,
        long totalSmallAppliances
) {
}
