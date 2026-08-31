package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.orderRow.OrderRowRequest;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderCreateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.*;
import com.epam.rd.autocode.assessment.appliances.repository.*;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderRowRepository orderRowRepository;
    private final CartItemRepository cartItemRepository;
    private final ApplianceRepository applianceRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrdersSummary(String keyword, Pageable pageable) {
        Page<Order> orders;

        if (keyword != null && !keyword.isBlank()) {
            orders = orderRepository.findAllWithFilter(keyword, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }

        return orders.map(OrderSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getClientOrders(String clientEmail, Pageable pageable) {
        Page<Order> ordersPage = orderRepository.findByClientEmail(clientEmail, pageable);

        return ordersPage.map(OrderResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order with id " + orderId + " not found"));
        return OrderResponse.fromEntity(order);
    }

//    @Override
//    @Transactional
//    public OrderResponse createOrder(Long clientId, OrderCreateRequest request) {
//        User user = userRepository.findById(clientId)
//                .orElseThrow(() -> new NotFoundException("User with id " + clientId + " not found"));
//
//        if (!(user instanceof Client client)) {
//            throw new IllegalArgumentException("User is not client. Only clients can create orders");
//        }
//
//        Order order = Order.builder()
//                .client(client)
//                .approved(false)
//                .build();
//
//        Set<OrderRow> orderRows = new HashSet<>();
//        BigDecimal totalPrice = BigDecimal.ZERO;
//
//        for (OrderRowRequest orderRowRequest : request.items()) {
//            Appliance appliance = applianceRepository.findById(orderRowRequest.applianceId())
//                    .orElseThrow(() -> new NotFoundException("Appliance with id " + orderRowRequest.applianceId() + " not found"));
//
//            OrderRow orderRow = OrderRow.builder()
//                    .order(order)
//                    .appliance(appliance)
//                    .amount(orderRowRequest.amound())
//                    .build();
//
//            BigDecimal rowPrice = appliance.getPrice().multiply(orderRowRequest.amound());
//            totalPrice = totalPrice.add(rowPrice);
//
//            orderRows.add(orderRow);
//        }
//
//        order.setOrderRowSet(orderRows);
//        order.setTotalPrice(totalPrice);
//
//        Order savedOrder = orderRepository.save(order);
//
//        return OrderResponse.fromEntity(savedOrder);
//    }

    @Override
    @Transactional
    public void approveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order with id " + orderId + " not found"));

        if (order.getApproved()) {
            throw new IllegalStateException("Order has already been approved");
        }

        order.setApproved(true);

        OrderResponse.fromEntity(order);
    }

    @Override
    @Transactional
    public void checkout(String clientEmail) {
        Client client = (Client) userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new NotFoundException("User with email " + clientEmail + " not found"));

        Cart cart = cartRepository.findByClient(client)
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        List<CartItem> cartItems = cart.getItems();

        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalStateException("Cart items is empty");
        }

        BigDecimal totalPrice = cartItems.stream()
                .map(item -> item.getAppliance().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .client(client)
                .totalPrice(totalPrice)
                .approved(false)
                .build();

        order = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            OrderRow orderRow = OrderRow.builder()
                    .order(order)
                    .appliance(cartItem.getAppliance())
                    .build();

            orderRow.setNumber((long) cartItem.getQuantity());

            BigDecimal rowAmound = cartItem.getAppliance().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            orderRow.setAmount(rowAmound);

            orderRowRepository.save(orderRow);
        }

        cartItemRepository.deleteAll(cartItems);
        cartItems.clear();
    }

//    @Override
//    @Transactional
//    public void deleteOrder(Long orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new NotFoundException("Order with id " + orderId + " not found"));
//
//        if (!order.getApproved()) {
//            throw new IllegalArgumentException("Cannot delete order an approved order");
//        }
//
//        orderRepository.delete(order);
//    }
}
