package com.lesieurcristal.b2bportal.order;

import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import com.lesieurcristal.b2bportal.entity.erpmock.OrderStatus;
import com.lesieurcristal.b2bportal.notification.service.PortalNotificationService;
import com.lesieurcristal.b2bportal.order.connector.ErpOrderConnector;
import com.lesieurcristal.b2bportal.order.connector.MockErpOrderConnector;
import com.lesieurcristal.b2bportal.order.dto.OrderDetailDto;
import com.lesieurcristal.b2bportal.order.dto.OrderResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;
import com.lesieurcristal.b2bportal.order.dto.UpdateOrderStatusDto;
import com.lesieurcristal.b2bportal.order.service.OrderService;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.OrderRepository;
import com.lesieurcristal.b2bportal.repository.OrderStatusRepository;
import com.lesieurcristal.b2bportal.repository.ProductRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.sample.service.SampleService;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SampleService sampleService;

    @Mock
    private PortalNotificationService portalNotificationService;

    private ErpOrderConnector erpOrderConnector;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        erpOrderConnector = new MockErpOrderConnector(orderRepository, orderStatusRepository);
        orderService = new OrderService(
                erpOrderConnector,
                orderRepository,
                orderStatusRepository,
                productRepository,
                customerRepository,
                userRepository,
                sampleService,
                portalNotificationService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsUser(String customerNumber, UserRole role) {
        Customer customer = customerNumber != null ? Customer.builder().customerNumber(customerNumber).build() : null;
        User user = User.builder()
                .id(1L)
                .login("testclient")
                .passwordHash("hashed")
                .role(role)
                .customer(customer)
                .isActive(true)
                .build();

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    @Test
    void getOrdersForCurrentUser_returnsOrdersWithInvoice() {
        authenticateAsUser("CUST0001", UserRole.CLIENT);

        Customer customer = Customer.builder().customerNumber("CUST0001").companyName("Client 1").build();
        Invoice invoice = Invoice.builder()
                .invoiceNumber("900010001")
                .invoiceDate(LocalDate.of(2026, 5, 13))
                .invoiceStatus("paid")
                .totalAmount(new BigDecimal("79200.00"))
                .dueDate(LocalDate.of(2026, 6, 12))
                .build();

        Order order = Order.builder()
                .orderNumber("4500010001")
                .orderDate(LocalDate.of(2026, 5, 5))
                .customer(customer)
                .customerOrderReference("BC-AA-0512")
                .productCode("HTO-001")
                .productLabel("Huile de tournesol Lesieur 1L")
                .netAmount(new BigDecimal("72000.00"))
                .currency("MAD")
                .invoice(invoice)
                .build();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(orderRepository.findFilteredOrders("CUST0001", null, null, null, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(order)));

        org.springframework.data.domain.Page<OrderResponseDto> result = orderService.getOrdersForCurrentUser(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        OrderResponseDto dto = result.getContent().get(0);
        assertThat(dto.orderNumber()).isEqualTo("4500010001");
        assertThat(dto.invoiceNumber()).isEqualTo("900010001");
        assertThat(dto.invoiceStatus()).isEqualTo("paid");
    }

    @Test
    void getOrderStatus_returnsLiveStatusForOwnedOrder() {
        authenticateAsUser("CUST0001", UserRole.CLIENT);

        Customer customer = Customer.builder().customerNumber("CUST0001").build();
        Order order = Order.builder().orderNumber("4500010001").customer(customer).build();
        OrderStatus orderStatus = OrderStatus.builder()
                .orderNumber("4500010001")
                .currentStatus("delivered")
                .statusUpdatedAt(OffsetDateTime.now())
                .carrierName("CTM Fret Maroc")
                .build();

        when(orderRepository.findByOrderNumberWithInvoiceAndStatus("4500010001")).thenReturn(Optional.of(order));
        when(orderStatusRepository.findByOrderNumber("4500010001")).thenReturn(Optional.of(orderStatus));

        OrderStatusResponseDto status = orderService.getOrderStatus("4500010001");

        assertThat(status.orderNumber()).isEqualTo("4500010001");
        assertThat(status.currentStatus()).isEqualTo("delivered");
        assertThat(status.carrierName()).isEqualTo("CTM Fret Maroc");
    }

    @Test
    void getOrderStatus_throwsExceptionWhenOrderBelongsToAnotherCustomer() {
        authenticateAsUser("CUST0001", UserRole.CLIENT);

        Customer otherCustomer = Customer.builder().customerNumber("CUST0002").build();
        Order order = Order.builder().orderNumber("4500010002").customer(otherCustomer).build();

        when(orderRepository.findByOrderNumberWithInvoiceAndStatus("4500010002")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderStatus("4500010002"))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("Commande introuvable");
    }

    @Test
    void getOrderDetail_returnsLinesInvoiceAndStatus() {
        authenticateAsUser("CUST0001", UserRole.CLIENT);

        Customer customer = Customer.builder().customerNumber("CUST0001").companyName("Client 1").build();
        Invoice invoice = Invoice.builder()
                .invoiceNumber("900010001")
                .invoiceDate(LocalDate.of(2026, 5, 13))
                .invoiceStatus("paid")
                .netAmount(new BigDecimal("72000.00"))
                .vatAmount(new BigDecimal("7200.00"))
                .totalAmount(new BigDecimal("79200.00"))
                .currency("MAD")
                .dueDate(LocalDate.of(2026, 6, 12))
                .paymentDate(LocalDate.of(2026, 6, 5))
                .build();
        OrderStatus status = OrderStatus.builder()
                .orderNumber("4500010001")
                .currentStatus("delivered")
                .statusUpdatedAt(OffsetDateTime.parse("2026-05-11T18:00:00+01:00"))
                .expectedDeliveryDate(LocalDate.of(2026, 5, 12))
                .carrierName("CTM Fret Maroc")
                .carrierReference("CTM-2026-88213")
                .build();
        Order order = Order.builder()
                .orderNumber("4500010001")
                .orderDate(LocalDate.of(2026, 5, 5))
                .customer(customer)
                .customerOrderReference("BC-AA-0512")
                .productCode("HTO-001")
                .productLabel("Huile de tournesol Lesieur 1L")
                .quantityOrdered(new BigDecimal("500"))
                .quantityShipped(new BigDecimal("500"))
                .salesUnit("CAR")
                .netAmount(new BigDecimal("72000.00"))
                .currency("MAD")
                .shipToCity("Casablanca")
                .shipToCountry("Maroc")
                .invoice(invoice)
                .orderStatus(status)
                .build();

        when(orderRepository.findByOrderNumberWithInvoiceAndStatus("4500010001")).thenReturn(Optional.of(order));
        when(orderRepository.findByCustomerNumberAndCustomerOrderReference("CUST0001", "BC-AA-0512"))
                .thenReturn(List.of(order));

        OrderDetailDto detail = orderService.getOrderDetail("4500010001");

        assertThat(detail.orderNumber()).isEqualTo("4500010001");
        assertThat(detail.lines()).hasSize(1);
        assertThat(detail.lines().get(0).productCode()).isEqualTo("HTO-001");
        assertThat(detail.invoice()).isNotNull();
        assertThat(detail.invoice().invoiceNumber()).isEqualTo("900010001");
        assertThat(detail.invoice().invoiceStatus()).isEqualTo("paid");
        assertThat(detail.status()).isNotNull();
        assertThat(detail.status().currentStatus()).isEqualTo("delivered");
        assertThat(detail.status().carrierName()).isEqualTo("CTM Fret Maroc");
        assertThat(detail.totalNetAmount()).isEqualByComparingTo("72000.00");
    }

    @Test
    void getOrderDetail_hidesOtherCustomersOrders() {
        authenticateAsUser("CUST0001", UserRole.CLIENT);

        Customer other = Customer.builder().customerNumber("CUST0002").build();
        Order order = Order.builder().orderNumber("4500010003").customer(other).build();
        when(orderRepository.findByOrderNumberWithInvoiceAndStatus("4500010003")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderDetail("4500010003"))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("Commande introuvable");
    }

    @Test
    void updateOrderStatus_updatesErpMockAndNotifiesClient() {
        authenticateAsUser(null, UserRole.ADMIN);

        Customer customer = Customer.builder().customerNumber("CUST0001").companyName("Client 1").build();
        Order order = Order.builder().orderNumber("4500010005").customer(customer).build();
        OrderStatus existing = OrderStatus.builder()
                .orderNumber("4500010005")
                .order(order)
                .currentStatus("confirmed")
                .statusUpdatedAt(OffsetDateTime.now().minusDays(1))
                .build();
        User clientUser = User.builder()
                .id(10L)
                .login("client1")
                .passwordHash("x")
                .role(UserRole.CLIENT)
                .customer(customer)
                .isActive(true)
                .build();

        when(orderRepository.findByOrderNumberWithInvoiceAndStatus("4500010005")).thenReturn(Optional.of(order));
        when(orderStatusRepository.findByOrderNumber("4500010005")).thenReturn(Optional.of(existing));
        when(orderStatusRepository.save(any(OrderStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByCustomer_CustomerNumber("CUST0001")).thenReturn(List.of(clientUser));

        UpdateOrderStatusDto dto = new UpdateOrderStatusDto(
                "in_preparation",
                LocalDate.of(2026, 7, 5),
                "CTM Fret Maroc",
                "CTM-NEW-001"
        );

        OrderStatusResponseDto result = orderService.updateOrderStatus("4500010005", dto);

        assertThat(result.currentStatus()).isEqualTo("in_preparation");
        assertThat(result.carrierName()).isEqualTo("CTM Fret Maroc");
        assertThat(result.carrierReference()).isEqualTo("CTM-NEW-001");
        assertThat(result.expectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 7, 5));

        ArgumentCaptor<OrderStatus> statusCaptor = ArgumentCaptor.forClass(OrderStatus.class);
        verify(orderStatusRepository).save(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getCurrentStatus()).isEqualTo("in_preparation");

        verify(portalNotificationService).createNotificationForUser(
                eq(clientUser),
                eq("Mise à jour de votre commande"),
                eq("Votre commande #4500010005 est passée de « Confirmée » à « En préparation »."),
                eq("ORDER_STATUS_CHANGED"),
                eq("ORDER"),
                eq("4500010005"),
                eq("/client/orders/4500010005")
        );
    }
}
