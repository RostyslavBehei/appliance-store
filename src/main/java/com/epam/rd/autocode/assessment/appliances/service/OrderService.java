package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface OrderService {
    Page<OrderSummaryResponse> getAllOrdersSummary(String keyword, Pageable pageable);
    Page<OrderResponse> getClientOrders(String clientEmail, Pageable pageable);
    OrderResponse getOrderById(Long orderId);
    void approveOrder(Long orderId);
    void checkout(String clientEmail);
}
