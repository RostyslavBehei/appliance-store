package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.dashboard.DashboardAdminResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrderRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ApplianceRepository applianceRepository;

    @Transactional(readOnly = true)
    public DashboardAdminResponse getDashboardAdminResponse() {
        Long revenueObj = orderRepository.calculateTotalRevenue(
                YearMonth.now().atDay(1).atStartOfDay(),
                YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX)
        );
        long totalRevenue = revenueObj != null ? revenueObj : 0L;

        double totalRevenuePercents = calculateTotalPercents((start, end) -> {
            Long rev = orderRepository.calculateTotalRevenue(start, end);
            return rev != null ? rev : 0L;
        });

        long totalOrders = orderRepository.count();
        double totalOrdersPercents = calculateTotalPercents(orderRepository::countByCreatedAtBetween);

        long activeUsers = userRepository.count();
        double activeUsersPercents = calculateTotalPercents(userRepository::countByCreatedAtBetween);

        long totalAppliances = applianceRepository.count();
        double totalAppliancesPercents = calculateTotalPercents(applianceRepository::countByCreatedAtBetween);

        List<OrderSummaryResponse> recentOrders = orderRepository.findAll(
                PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).map(OrderSummaryResponse::fromEntity).toList();

        long totalBigAppliance = applianceRepository.countByCategory(Category.BIG);
        long totalSmallAppliance = applianceRepository.countByCategory(Category.SMALL);

        return new DashboardAdminResponse(
                totalRevenue,
                totalRevenuePercents,
                totalOrders,
                totalOrdersPercents,
                activeUsers,
                activeUsersPercents,
                totalAppliances,
                totalAppliancesPercents,
                recentOrders,
                totalBigAppliance,
                totalSmallAppliance
        );
    }

    private double calculateTotalPercents(BiFunction<LocalDateTime, LocalDateTime, Long> repositoryMethod) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = YearMonth.now().minusMonths(1);

        long currentCount = countForMonth(currentMonth, repositoryMethod);
        long previousCount = countForMonth(previousMonth, repositoryMethod);

        if (previousCount == 0) {
            return currentCount > 0 ? 100.0 : 0.0;
        }

        return ((double) (currentCount - previousCount) / previousCount) * 100.0;
    }

    private long countForMonth(YearMonth month, BiFunction<LocalDateTime, LocalDateTime, Long> repositoryMethod) {
        LocalDateTime startDate = month.atDay(1).atStartOfDay();
        LocalDateTime endDate = month.atEndOfMonth().atTime(LocalTime.MAX);
        return repositoryMethod.apply(startDate, endDate);
    }
}