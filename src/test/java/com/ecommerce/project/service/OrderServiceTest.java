package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderItemDTO;
import com.ecommerce.project.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartService cartService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Cart cart;
    private Address address;
    private Product product;
    private CartItem cartItem;
    private Order order;
    private OrderDTO orderDTO;
    private Payment payment;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setProductId(1L);
        product.setProductName("iPhone 15");
        product.setQuantity(100);
        product.setPrice(999.99);
        product.setSpecialPrice(899.99);

        cartItem = new CartItem();
        cartItem.setCartItemId(1L);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItem.setProductPrice(899.99);
        cartItem.setDiscount(10.0);

        cart = new Cart();
        cart.setCartId(1L);
        cart.setTotalPrice(1799.98);
        cart.setCartItems(new ArrayList<>(List.of(cartItem)));

        address = new Address();
        address.setAddressId(1L);
        address.setStreet("123 Main St");
        address.setCity("New York");
        address.setState("NY");
        address.setCountry("USA");
        address.setPincode("10001");

        order = new Order();
        order.setOrderId(1L);
        order.setEmail("test@example.com");
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(1799.98);
        order.setOrderStatus("Order Accepted !");
        order.setAddress(address);

        payment = new Payment();
        payment.setPaymentId(1L);
        payment.setPaymentMethod("Card");
        payment.setPgPaymentId("pay_123");
        payment.setPgStatus("Success");
        payment.setPgName("Stripe");

        orderDTO = new OrderDTO();
        orderDTO.setOrderId(1L);
        orderDTO.setEmail("test@example.com");
        orderDTO.setTotalAmount(1799.98);
        orderDTO.setOrderStatus("Order Accepted !");
        orderDTO.setOrderItems(new ArrayList<>());
    }

    @Nested
    @DisplayName("placeOrder() Tests")
    class PlaceOrderTests {

        @Test
        @DisplayName("Should place order successfully with valid cart and address")
        void placeOrder_Success() {
            when(cartRepository.findCartByEmail("test@example.com")).thenReturn(cart);
            when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
            when(productRepository.save(any(Product.class))).thenReturn(product);
            when(cartService.deleteProductFromCart(anyLong(), anyLong())).thenReturn("Deleted");
            when(modelMapper.map(any(Order.class), eq(OrderDTO.class))).thenReturn(orderDTO);
            when(modelMapper.map(any(OrderItem.class), eq(OrderItemDTO.class)))
                .thenReturn(new OrderItemDTO());

            OrderDTO result = orderService.placeOrder(
                "test@example.com",
                1L,
                "Card",
                "Stripe",
                "pay_123",
                "Success",
                "Payment successful"
            );

            assertNotNull(result);
            assertEquals("test@example.com", result.getEmail());
            assertEquals(1799.98, result.getTotalAmount());

            verify(cartRepository).findCartByEmail("test@example.com");
            verify(addressRepository).findById(1L);
            verify(paymentRepository).save(any(Payment.class));
            verify(orderRepository).save(any(Order.class));
            verify(orderItemRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when cart not found")
        void placeOrder_CartNotFound_ThrowsException() {
            when(cartRepository.findCartByEmail("nonexistent@example.com")).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.placeOrder(
                    "nonexistent@example.com",
                    1L,
                    "Card",
                    "Stripe",
                    "pay_123",
                    "Success",
                    "Payment successful"
                )
            );

            assertTrue(exception.getMessage().contains("Cart"));
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when address not found")
        void placeOrder_AddressNotFound_ThrowsException() {
            when(cartRepository.findCartByEmail("test@example.com")).thenReturn(cart);
            when(addressRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> orderService.placeOrder(
                    "test@example.com",
                    999L,
                    "Card",
                    "Stripe",
                    "pay_123",
                    "Success",
                    "Payment successful"
                )
            );

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw APIException when cart is empty")
        void placeOrder_EmptyCart_ThrowsException() {
            Cart emptyCart = new Cart();
            emptyCart.setCartId(2L);
            emptyCart.setCartItems(new ArrayList<>());

            when(cartRepository.findCartByEmail("test@example.com")).thenReturn(emptyCart);
            when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            APIException exception = assertThrows(APIException.class,
                () -> orderService.placeOrder(
                    "test@example.com",
                    1L,
                    "Card",
                    "Stripe",
                    "pay_123",
                    "Success",
                    "Payment successful"
                )
            );

            assertEquals("Cart is empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should reduce product stock quantity after order")
        void placeOrder_ReducesStockQuantity() {
            int initialQuantity = product.getQuantity();
            int orderedQuantity = cartItem.getQuantity();

            when(cartRepository.findCartByEmail("test@example.com")).thenReturn(cart);
            when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderItemRepository.saveAll(anyList())).thenReturn(new ArrayList<>());
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product savedProduct = invocation.getArgument(0);
                assertEquals(initialQuantity - orderedQuantity, savedProduct.getQuantity());
                return savedProduct;
            });
            when(cartService.deleteProductFromCart(anyLong(), anyLong())).thenReturn("Deleted");
            when(modelMapper.map(any(Order.class), eq(OrderDTO.class))).thenReturn(orderDTO);

            orderService.placeOrder(
                "test@example.com",
                1L,
                "Card",
                "Stripe",
                "pay_123",
                "Success",
                "Payment successful"
            );

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should clear cart items after placing order")
        void placeOrder_ClearsCartItems() {
            when(cartRepository.findCartByEmail("test@example.com")).thenReturn(cart);
            when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderItemRepository.saveAll(anyList())).thenReturn(new ArrayList<>());
            when(productRepository.save(any(Product.class))).thenReturn(product);
            when(cartService.deleteProductFromCart(anyLong(), anyLong())).thenReturn("Deleted");
            when(modelMapper.map(any(Order.class), eq(OrderDTO.class))).thenReturn(orderDTO);

            orderService.placeOrder(
                "test@example.com",
                1L,
                "Card",
                "Stripe",
                "pay_123",
                "Success",
                "Payment successful"
            );

            verify(cartService).deleteProductFromCart(cart.getCartId(), product.getProductId());
        }

        @Test
        @DisplayName("Should save payment details correctly")
        void placeOrder_SavesPaymentDetails() {
            when(cartRepository.findCartByEmail("test@example.com")).thenReturn(cart);
            when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment savedPayment = invocation.getArgument(0);
                assertEquals("Card", savedPayment.getPaymentMethod());
                assertEquals("Stripe", savedPayment.getPgName());
                assertEquals("pay_123", savedPayment.getPgPaymentId());
                assertEquals("Success", savedPayment.getPgStatus());
                return payment;
            });
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderItemRepository.saveAll(anyList())).thenReturn(new ArrayList<>());
            when(productRepository.save(any(Product.class))).thenReturn(product);
            when(cartService.deleteProductFromCart(anyLong(), anyLong())).thenReturn("Deleted");
            when(modelMapper.map(any(Order.class), eq(OrderDTO.class))).thenReturn(orderDTO);

            orderService.placeOrder(
                "test@example.com",
                1L,
                "Card",
                "Stripe",
                "pay_123",
                "Success",
                "Payment successful"
            );

            verify(paymentRepository).save(any(Payment.class));
        }
    }
}
