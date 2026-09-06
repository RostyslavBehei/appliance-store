package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderCheckoutRequest;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.orders.OrderSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.*;
import com.epam.rd.autocode.assessment.appliances.repository.*;
import com.epam.rd.autocode.assessment.appliances.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderRowRepository orderRowRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private MailService mailService;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Client testClient;
    private Order testOrder;
    private Pageable pageable;
    private OrderCheckoutRequest checkoutRequest;

    @BeforeEach
    void setUp() {
        testClient = Client.builder()
                .id(1L)
                .email("client@test.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        ShippingDetails shippingDetails = ShippingDetails.builder()
                .contactFirstName("John")
                .contactLastName("Doe")
                .contactEmail("client@test.com")
                .contactPhone("+380980000000")
                .country("Ukraine")
                .city("Lviv")
                .street("Volodymyra Velykoho")
                .zipCode("79000")
                .paymentMethod("CASH")
                .build();

        testOrder = Order.builder()
                .id(10L)
                .client(testClient)
                .totalPrice(BigDecimal.valueOf(300.00))
                .approved(false)
                .shippingDetails(shippingDetails)
                .build();

        pageable = PageRequest.of(0, 10);

        checkoutRequest = new OrderCheckoutRequest(
                "John",
                "Doe",
                "client@test.com",
                "+380980000000",
                "Ukraine",
                "Lviv",
                "Volodymyra Velykoho",
                "79000",
                "CASH"
        );

        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @Test
    @DisplayName("Get All Orders Summary - Should return filtered list when keyword is provided")
    void getAllOrdersSummary_ShouldReturnFilteredList_WhenKeywordProvided() {
        String keyword = "John";
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAllWithFilter(keyword, pageable)).thenReturn(orderPage);

        Page<OrderSummaryResponse> result = orderService.getAllOrdersSummary(keyword, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(orderRepository, times(1)).findAllWithFilter(keyword, pageable);
        verify(orderRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Get All Orders Summary - Should return all orders when keyword is null or blank")
    void getAllOrdersSummary_ShouldReturnAllOrders_WhenKeywordIsNullOrBlank() {
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAll(pageable)).thenReturn(orderPage);

        Page<OrderSummaryResponse> resultNull = orderService.getAllOrdersSummary(null, pageable);
        Page<OrderSummaryResponse> resultBlank = orderService.getAllOrdersSummary("   ", pageable);

        assertEquals(1, resultNull.getTotalElements());
        assertEquals(1, resultBlank.getTotalElements());
        verify(orderRepository, times(2)).findAll(pageable);
        verify(orderRepository, never()).findAllWithFilter(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Get Client Orders - Should return orders for specific client")
    void getClientOrders_ShouldReturnOrders() {
        String email = "client@test.com";
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findByClientEmail(email, pageable)).thenReturn(orderPage);

        Page<OrderResponse> result = orderService.getClientOrders(email, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).id());
        verify(orderRepository, times(1)).findByClientEmail(email, pageable);
    }

    @Test
    @DisplayName("Get Order By Id - Should return Order")
    void getOrderById_ShouldReturnOrder() {
        Long id = 10L;
        when(orderRepository.findById(id)).thenReturn(Optional.of(testOrder));

        OrderResponse result = orderService.getOrderById(id);

        assertNotNull(result);
        assertEquals(10L, result.id());
        verify(orderRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Get Order By Id - Should throw NotFoundException if order does not exist")
    void getOrderById_ShouldThrowNotFoundExceptionIfOrderNotExist() {
        Long id = 99L;
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> orderService.getOrderById(id));

        assertEquals("error.order.not.found", result.getMessage());
        verify(orderRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Approve Order - Should successfully approve order")
    void approveOrder_ShouldSuccessfullyApproveOrder() {
        Long id = 10L;
        when(orderRepository.findById(id)).thenReturn(Optional.of(testOrder));

        assertFalse(testOrder.getApproved());

        orderService.approveOrder(id);

        assertTrue(testOrder.getApproved());
        verify(orderRepository, times(1)).findById(id);
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    @DisplayName("Approve Order - Should throw IllegalStateException if already approved")
    void approveOrder_ShouldThrowIllegalStateExceptionIfAlreadyApproved() {
        Long id = 10L;
        testOrder.setApproved(true);
        when(orderRepository.findById(id)).thenReturn(Optional.of(testOrder));

        IllegalStateException result = assertThrows(IllegalStateException.class, () -> orderService.approveOrder(id));

        assertEquals("error.order.already.approved", result.getMessage());
        verify(orderRepository, times(1)).findById(id);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Approve Order - Should throw NotFoundException if order not found")
    void approveOrder_ShouldThrowNotFoundExceptionIfOrderNotFound() {
        Long id = 99L;
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> orderService.approveOrder(id));

        assertEquals("error.order.not.found", result.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Checkout - Should successfully create order and clear cart")
    void checkout_ShouldSuccessfullyCreateOrder() {
        String email = "client@test.com";

        Appliance testAppliance = Appliance.builder()
                .id(1L)
                .name("Kettle")
                .model("KT-100")
                .price(BigDecimal.valueOf(150.00))
                .build();

        CartItem cartItem = CartItem.builder()
                .id(1L)
                .appliance(testAppliance)
                .quantity(2)
                .build();

        List<CartItem> mutableCartItems = new ArrayList<>(List.of(cartItem));
        Cart testCart = Cart.builder().id(1L).client(testClient).items(mutableCartItems).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testClient));
        when(cartRepository.findByClient(testClient)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        orderService.checkout(email, checkoutRequest);

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderRowRepository, times(1)).save(any(OrderRow.class));
        verify(cartItemRepository, times(1)).deleteAll(anyList());
        verify(mailService, times(1)).sendOrderMessage(eq(email), any(OrderResponse.class));
        assertTrue(mutableCartItems.isEmpty());
    }

    @Test
    @DisplayName("Checkout - Should succeed even if MailService throws an exception")
    void checkout_ShouldCompleteOrder_EvenIfMailServiceFails() {
        String email = "client@test.com";

        Appliance testAppliance = Appliance.builder()
                .id(1L)
                .name("Iron")
                .price(BigDecimal.valueOf(100.00))
                .build();

        CartItem cartItem = CartItem.builder()
                .id(1L)
                .appliance(testAppliance)
                .quantity(1)
                .build();

        List<CartItem> mutableCartItems = new ArrayList<>(List.of(cartItem));
        Cart testCart = Cart.builder().id(1L).client(testClient).items(mutableCartItems).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testClient));
        when(cartRepository.findByClient(testClient)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doThrow(new RuntimeException("SMTP Server Down"))
                .when(mailService).sendOrderMessage(eq(email), any(OrderResponse.class));

        assertDoesNotThrow(() -> orderService.checkout(email, checkoutRequest));

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(cartItemRepository, times(1)).deleteAll(anyList());
        assertTrue(mutableCartItems.isEmpty());
    }

    @Test
    @DisplayName("Checkout - Should throw NotFoundException if user not found")
    void checkout_ShouldThrowNotFoundExceptionIfUserNotFound() {
        String email = "unknown@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class,
                () -> orderService.checkout(email, checkoutRequest));

        assertEquals("error.user.not.found", result.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartItemRepository, never()).deleteAll(anyList());
    }

    @Test
    @DisplayName("Checkout - Should throw IllegalArgumentException if user is not a Client")
    void checkout_ShouldThrowIllegalArgumentExceptionIfUserNotClient() {
        String email = "employee@test.com";
        Employee employee = Employee.builder().id(2L).email(email).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
                () -> orderService.checkout(email, checkoutRequest));

        assertEquals("error.only.client.cart", result.getMessage());
        verify(cartRepository, never()).findByClient(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Checkout - Should throw NotFoundException if cart not found")
    void checkout_ShouldThrowNotFoundExceptionIfCartNotFound() {
        String email = "client@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testClient));
        when(cartRepository.findByClient(testClient)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class,
                () -> orderService.checkout(email, checkoutRequest));

        assertEquals("error.cart.not.found", result.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Checkout - Should throw IllegalStateException if cart is empty or items null")
    void checkout_ShouldThrowIllegalStateExceptionIfCartIsEmpty() {
        String email = "client@test.com";
        Cart emptyCart = Cart.builder().id(1L).client(testClient).items(new ArrayList<>()).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testClient));
        when(cartRepository.findByClient(testClient)).thenReturn(Optional.of(emptyCart));

        IllegalStateException result = assertThrows(IllegalStateException.class,
                () -> orderService.checkout(email, checkoutRequest));

        assertEquals("error.cart.empty", result.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }
}