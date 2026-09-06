package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderCheckoutRequest;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.*;
import com.epam.rd.autocode.assessment.appliances.repository.*;
import com.epam.rd.autocode.assessment.appliances.service.MailService;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderRowRepository orderRowRepository;
    private final CartItemRepository cartItemRepository;
    private final MessageSource messageSource;
    private final MailService mailService;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrdersSummary(String keyword, Pageable pageable) {
        Page<Order> orders;
        if (keyword == null || keyword.trim().isBlank()) {
            orders = orderRepository.findAll(pageable);
        } else {
            orders = orderRepository.findAllWithFilter(keyword.trim(), pageable);
        }
        return orders.map(OrderSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getClientOrders(String clientEmail, Pageable pageable) {
        log.debug("Fetching orders for client: '{}', pageNumber={}", clientEmail, pageable.getPageNumber());

        Page<Order> ordersPage = orderRepository.findByClientEmail(clientEmail, pageable);
        log.debug("Found {} orders for client '{}'", ordersPage.getTotalElements(), clientEmail);

        return ordersPage.map(OrderResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        log.debug("Fetching order by id: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Failed to fetch: Order with id {} not found", orderId);
                    return new NotFoundException(
                            messageSource.getMessage("error.order.not.found", new Object[]{orderId}, getLocale())
                    );
                });

        return OrderResponse.fromEntity(order);
    }

    @Override
    @Transactional
    public void approveOrder(Long orderId) {
        log.info("Attempting to approve order with id: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Failed to approve: Order with id {} not found", orderId);
                    return new NotFoundException(
                            messageSource.getMessage("error.order.not.found", new Object[]{orderId}, getLocale())
                    );
                });

        if (Boolean.TRUE.equals(order.getApproved())) {
            log.warn("Order id={} is already approved", orderId);
            throw new IllegalStateException(
                    messageSource.getMessage("error.order.already.approved", null, getLocale())
            );
        }

        order.setApproved(true);
        orderRepository.save(order);
        log.info("Order id={} successfully approved", orderId);
    }

    @Override
    @Transactional
    public void checkout(String clientEmail, OrderCheckoutRequest request) {
        log.info("Starting checkout process for user: '{}'", clientEmail);

        User user = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> {
                    log.warn("Checkout rejected: User with email '{}' not found", clientEmail);
                    return new NotFoundException(
                            messageSource.getMessage("error.user.not.found", new Object[]{clientEmail}, getLocale())
                    );
                });

        if (!(user instanceof Client client)) {
            log.warn("Checkout rejected: User '{}' is not an instance of Client", clientEmail);
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.only.client.cart", null, getLocale())
            );
        }

        Cart cart = cartRepository.findByClient(client)
                .orElseThrow(() -> {
                    log.warn("Checkout rejected: Cart not found for client '{}'", clientEmail);
                    return new NotFoundException(
                            messageSource.getMessage("error.cart.not.found", null, getLocale())
                    );
                });

        List<CartItem> cartItems = cart.getItems();
        if (cartItems == null || cartItems.isEmpty()) {
            log.warn("Checkout rejected: Cart is empty for client '{}'", clientEmail);
            throw new IllegalStateException(
                    messageSource.getMessage("error.cart.empty", null, getLocale())
            );
        }

        log.debug("Calculating total price for {} cart items", cartItems.size());
        BigDecimal totalPrice = cartItems.stream()
                .map(item -> item.getAppliance().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ShippingDetails shippingDetails = ShippingDetails.builder()
                .contactFirstName(request.firstName())
                .contactLastName(request.lastName())
                .contactEmail(request.email())
                .contactPhone(request.phone())
                .country(request.country())
                .city(request.city())
                .street(request.street())
                .zipCode(request.zipCode())
                .paymentMethod(request.paymentMethod())
                .build();

        Order order = Order.builder()
                .client(client)
                .totalPrice(totalPrice)
                .approved(false)
                .shippingDetails(shippingDetails)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with id: {} for client: '{}', total: {} $",
                savedOrder.getId(), clientEmail, totalPrice);

        for (CartItem cartItem : cartItems) {
            BigDecimal rowAmount = cartItem.getAppliance().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderRow orderRow = OrderRow.builder()
                    .order(savedOrder)
                    .appliance(cartItem.getAppliance())
                    .number((long) cartItem.getQuantity())
                    .amount(rowAmount)
                    .build();

            orderRowRepository.save(orderRow);
        }

        cartItemRepository.deleteAll(cartItems);
        cartItems.clear();
        log.info("Cart cleared for client '{}' after successful checkout", clientEmail);

        try {
            mailService.sendOrderMessage(clientEmail, OrderResponse.fromEntity(savedOrder));
            log.info("Order confirmation email triggered for order id: {}", savedOrder.getId());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order id: {}: {}", savedOrder.getId(), e.getMessage());
        }
    }

    private Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}