package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.dashboard.DashboardAdminResponse;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Order;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrderRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplianceRepository applianceRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        Client client = Client.builder()
                .id(1L)
                .email("client@test.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        testOrder = new Order();
        testOrder.setId(10L);
        testOrder.setClient(client);
        testOrder.setTotalPrice(BigDecimal.valueOf(500));
        testOrder.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Get Dashboard Admin Response - Should calculate and return valid metrics")
    void getDashboardAdminResponse_ShouldReturnValidMetrics() {
        when(orderRepository.calculateTotalRevenue(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1500L, 1500L, 1000L);

        when(orderRepository.count()).thenReturn(100L);
        when(orderRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(20L, 10L);

        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L, 0L);

        when(applianceRepository.count()).thenReturn(200L);
        when(applianceRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);

        when(orderRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(testOrder)));

        when(applianceRepository.countByCategory(Category.BIG)).thenReturn(30L);
        when(applianceRepository.countByCategory(Category.SMALL)).thenReturn(170L);

        DashboardAdminResponse response = dashboardService.getDashboardAdminResponse();

        assertNotNull(response);
        assertEquals(1500L, response.totalRevenue());
        assertEquals(50.0, response.totalRevenuePercents());

        assertEquals(100L, response.totalOrders());
        assertEquals(100.0, response.totalOrdersPercents());

        assertEquals(50L, response.activeUsers());
        assertEquals(100.0, response.activeUsersPercents());

        assertEquals(200L, response.totalAppliances());
        assertEquals(0.0, response.totalAppliancesPercents());

        assertEquals(30L, response.totalBigAppliances());
        assertEquals(170L, response.totalSmallAppliances());

        assertEquals(1, response.recentOrders().size());

        verify(orderRepository, times(3)).calculateTotalRevenue(any(), any());
        verify(orderRepository, times(2)).countByCreatedAtBetween(any(), any());
        verify(userRepository, times(2)).countByCreatedAtBetween(any(), any());
        verify(applianceRepository, times(2)).countByCreatedAtBetween(any(), any());
        verify(orderRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("Get Dashboard Admin Response - Should handle null and zero revenue correctly")
    void getDashboardAdminResponse_ShouldHandleNullRevenue() {
        when(orderRepository.calculateTotalRevenue(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(null, null, null);

        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);

        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);

        when(applianceRepository.count()).thenReturn(0L);
        when(applianceRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);

        when(orderRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        when(applianceRepository.countByCategory(Category.BIG)).thenReturn(0L);
        when(applianceRepository.countByCategory(Category.SMALL)).thenReturn(0L);

        DashboardAdminResponse response = dashboardService.getDashboardAdminResponse();

        assertNotNull(response);
        assertEquals(0L, response.totalRevenue());
        assertEquals(0.0, response.totalRevenuePercents());
        assertEquals(0L, response.totalOrders());
        assertEquals(0.0, response.totalOrdersPercents());
        assertEquals(0, response.recentOrders().size());
    }
}