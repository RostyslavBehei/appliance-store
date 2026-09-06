package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.cart.CartResponse;
import com.epam.rd.autocode.assessment.appliances.dto.cartItem.CartItemAddRequest;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.*;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.CartItemRepository;
import com.epam.rd.autocode.assessment.appliances.repository.CartRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ApplianceRepository applianceRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private CartServiceImpl cartService;

    private Client testClient;
    private Appliance testAppliance;
    private Cart testCart;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testClient = Client.builder()
                .id(1L)
                .email("test@test.com")
                .role(Role.ROLE_CLIENT)
                .build();

        testAppliance = Appliance.builder()
                .id(1L)
                .name("Microwave")
                .category(Category.BIG)
                .build();

        testCart = Cart.builder()
                .id(1L)
                .client(testClient)
                .items(new ArrayList<>())
                .build();

        testCartItem = CartItem.builder()
                .id(1L)
                .cart(testCart)
                .appliance(testAppliance)
                .quantity(1)
                .build();

        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @Test
    @DisplayName("Add To Cart - Should successfully add new item to cart")
    void addToCart_ShouldSuccessfullyAddToCart() {
        String clientEmail = "test@test.com";
        Long applianceId = 1L;
        int quantity = 2;

        CartItemAddRequest request = new CartItemAddRequest(clientEmail, applianceId, quantity);

        when(userRepository.findByEmail(clientEmail)).thenReturn(Optional.of(testClient));
        when(cartRepository.findByClient(testClient)).thenReturn(Optional.of(testCart));
        when(applianceRepository.findById(applianceId)).thenReturn(Optional.of(testAppliance));

        cartService.addToCart(request);

        assertEquals(1, testCart.getItems().size());
        assertEquals("Microwave", testCart.getItems().get(0).getAppliance().getName());
        assertEquals(quantity, testCart.getItems().get(0).getQuantity());

        verify(userRepository, times(1)).findByEmail(clientEmail);
        verify(cartRepository, times(1)).findByClient(testClient);
        verify(applianceRepository, times(1)).findById(applianceId);
        verify(cartRepository, times(1)).save(testCart);
    }

    @Test
    @DisplayName("Add To Cart - Should increase quantity if item already in cart")
    void addToCart_ShouldIncreaseQuantity_WhenItemAlreadyInCart() {
        String clientEmail = "test@test.com";
        Long applianceId = 1L;
        int existingQuantity = 2;
        int addedQuantity = 3;

        testCartItem.setQuantity(existingQuantity);
        testCart.getItems().add(testCartItem);

        CartItemAddRequest request = new CartItemAddRequest(clientEmail, applianceId, addedQuantity);

        when(userRepository.findByEmail(clientEmail)).thenReturn(Optional.of(testClient));
        when(cartRepository.findByClient(testClient)).thenReturn(Optional.of(testCart));
        when(applianceRepository.findById(applianceId)).thenReturn(Optional.of(testAppliance));

        cartService.addToCart(request);

        assertEquals(1, testCart.getItems().size());
        assertEquals(existingQuantity + addedQuantity, testCart.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(testCart);
    }

    @Test
    @DisplayName("Add To Cart - Should create new cart if client does not have one")
    void addToCart_ShouldCreateCart_WhenClientHasNoCart() {
        String clientEmail = "test@test.com";
        Long applianceId = 1L;

        CartItemAddRequest request = new CartItemAddRequest(clientEmail, applianceId, 1);

        when(userRepository.findByEmail(clientEmail)).thenReturn(Optional.of(testClient));
        when(cartRepository.findByClient(testClient)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(applianceRepository.findById(applianceId)).thenReturn(Optional.of(testAppliance));

        cartService.addToCart(request);

        verify(cartRepository, times(2)).save(any(Cart.class)); // 1 раз створення нового кошика, 2 раз збереження з товаром
        assertEquals(1, testCart.getItems().size());
    }

    @Test
    @DisplayName("Add To Cart - Should throw NotFoundException if client not found")
    void addToCart_ShouldThrowNotFoundExceptionIfClientIsNotExist() {
        String clientEmail = "notExistEmail@test.com";
        CartItemAddRequest request = new CartItemAddRequest(clientEmail, 1L, 1);

        when(userRepository.findByEmail(clientEmail)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> cartService.addToCart(request));

        assertEquals("error.user.not.found", result.getMessage());
        verify(userRepository, times(1)).findByEmail(clientEmail);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ROLE_ADMIN", "ROLE_EMPLOYEE"})
    @DisplayName("Add To Cart - Should throw IllegalArgumentException if user is not a client")
    void addToCart_ShouldThrowIllegalArgumentExceptionIfUserIsNotClient(Role role) {
        String staffEmail = "staff@test.com";
        User staff = User.builder()
                .email(staffEmail)
                .role(role)
                .build();

        CartItemAddRequest request = new CartItemAddRequest(staffEmail, 1L, 1);

        when(userRepository.findByEmail(staffEmail)).thenReturn(Optional.of(staff));

        IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> cartService.addToCart(request));

        assertEquals("error.only.client.cart", result.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Add To Cart - Should throw NotFoundException if appliance not exist")
    void addToCart_ShouldThrowNotFoundExceptionIfApplianceIsNotExist() {
        String clientEmail = "test@test.com";
        Long applianceId = 99L;

        CartItemAddRequest request = new CartItemAddRequest(clientEmail, applianceId, 1);

        when(userRepository.findByEmail(clientEmail)).thenReturn(Optional.of(testClient));
        when(cartRepository.findByClient(testClient)).thenReturn(Optional.of(testCart));
        when(applianceRepository.findById(applianceId)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> cartService.addToCart(request));

        assertEquals("error.appliance.not.found", result.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Get All Carts - Should return all carts")
    void getAllCarts_ShouldReturnAllCarts() {
        String clientEmail = "test@test.com";

        when(userRepository.findByEmail(clientEmail)).thenReturn(Optional.of(testClient));
        when(cartRepository.findByClient(testClient)).thenReturn(Optional.of(testCart));

        List<CartResponse> result = cartService.getAllCarts(clientEmail);

        assertEquals(1, result.size());
        verify(userRepository, times(1)).findByEmail(clientEmail);
        verify(cartRepository, times(1)).findByClient(testClient);
    }

    @Test
    @DisplayName("Get All Carts - Should return empty list if user is not client")
    void getAllCarts_ShouldReturnEmptyList_WhenUserNotClient() {
        String staffEmail = "admin@test.com";
        User admin = User.builder().email(staffEmail).role(Role.ROLE_ADMIN).build();

        when(userRepository.findByEmail(staffEmail)).thenReturn(Optional.of(admin));

        List<CartResponse> result = cartService.getAllCarts(staffEmail);

        assertTrue(result.isEmpty());
        verify(cartRepository, never()).findByClient(any());
    }

    @Test
    @DisplayName("Get All Carts - Should throw NotFoundException if user does not exist")
    void getAllCarts_ShouldThrowNotFoundExceptionIfClientIsNotExist() {
        String fantomEmail = "fantom@test.com";

        when(userRepository.findByEmail(fantomEmail)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> cartService.getAllCarts(fantomEmail));

        assertEquals("error.user.not.found", result.getMessage());
        verify(cartRepository, never()).findByClient(any());
    }

    @Test
    @DisplayName("Get Cart Item Count - Should successfully return total items count")
    void getCartItemCount_ShouldReturnCartItemCount() {
        String clientEmail = "test@test.com";
        testCartItem.setQuantity(3);
        testCart.setItems(List.of(testCartItem));

        when(userRepository.findByEmail(clientEmail)).thenReturn(Optional.of(testClient));
        when(cartRepository.findByClient(testClient)).thenReturn(Optional.of(testCart));

        int result = cartService.getCartItemCount(clientEmail);

        assertEquals(3, result);
        verify(userRepository, times(1)).findByEmail(clientEmail);
        verify(cartRepository, times(1)).findByClient(testClient);
    }

    @Test
    @DisplayName("Get Cart Item Count - Should return 0 if cart is not present")
    void getCartItemCount_ShouldReturnZero_WhenCartNotPresent() {
        String clientEmail = "test@test.com";

        when(userRepository.findByEmail(clientEmail)).thenReturn(Optional.of(testClient));
        when(cartRepository.findByClient(testClient)).thenReturn(Optional.empty());

        int result = cartService.getCartItemCount(clientEmail);

        assertEquals(0, result);
        verify(userRepository, times(1)).findByEmail(clientEmail);
        verify(cartRepository, times(1)).findByClient(testClient);
    }

    @Test
    @DisplayName("Get Cart Item Count - Should return 0 if user does not exist")
    void getCartItemCount_ShouldReturnZero_WhenUserNotExist() {
        String clientEmail = "fantom@test.com";

        when(userRepository.findByEmail(clientEmail)).thenReturn(Optional.empty());

        int result = cartService.getCartItemCount(clientEmail);

        assertEquals(0, result);
        verify(cartRepository, never()).findByClient(any());
    }

    @Test
    @DisplayName("Update Cart Item Quantity - Should successfully update quantity")
    void updateCartItemQuantity_ShouldUpdateCartItemQuantity() {
        Long cartItemId = 1L;
        int newQuantity = 5;

        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(testCartItem));

        cartService.updateCartItemQuantity(cartItemId, newQuantity);

        assertEquals(newQuantity, testCartItem.getQuantity());
        verify(cartItemRepository, times(1)).findById(cartItemId);
        verify(cartItemRepository, times(1)).save(testCartItem);
    }

    @Test
    @DisplayName("Update Cart Item Quantity - Should enforce minimum quantity of 1 if negative or 0 passed")
    void updateCartItemQuantity_ShouldEnforceMinimumOne_WhenNegativeProvided() {
        Long cartItemId = 1L;

        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(testCartItem));

        cartService.updateCartItemQuantity(cartItemId, -3);

        assertEquals(1, testCartItem.getQuantity());
        verify(cartItemRepository, times(1)).save(testCartItem);
    }

    @Test
    @DisplayName("Update Cart Item Quantity - Should throw NotFoundException if cart item not exist")
    void updateCartItemQuantity_ShouldThrowNotFoundExceptionIfCartItemNotExist() {
        Long cartItemId = 1L;

        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> cartService.updateCartItemQuantity(cartItemId, 1));

        assertEquals("error.cart.item.not.found", result.getMessage());
        verify(cartItemRepository, times(1)).findById(cartItemId);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete Cart Item - Should successfully delete cart item")
    void deleteCartItem_ShouldDeleteCartItem() {
        Long cartItemId = 1L;

        when(cartItemRepository.existsById(cartItemId)).thenReturn(true);

        cartService.deleteCartItem(cartItemId);

        verify(cartItemRepository, times(1)).existsById(cartItemId);
        verify(cartItemRepository, times(1)).forceDeleteById(cartItemId);
    }

    @Test
    @DisplayName("Delete Cart Item - Should throw NotFoundException if cart item not exist")
    void deleteCartItem_ShouldThrowNotFoundExceptionIfCartItemIsNotExist() {
        Long cartItemId = 1L;

        when(cartItemRepository.existsById(cartItemId)).thenReturn(false);

        NotFoundException result = assertThrows(NotFoundException.class, () -> cartService.deleteCartItem(cartItemId));

        assertEquals("error.cart.item.not.found", result.getMessage());
        verify(cartItemRepository, times(1)).existsById(cartItemId);
        verify(cartItemRepository, never()).forceDeleteById(anyLong());
    }
}