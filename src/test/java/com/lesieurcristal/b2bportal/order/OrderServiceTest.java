package com.lesieurcristal.b2bportal.order;

import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import com.lesieurcristal.b2bportal.entity.erpmock.OrderStatus;
import com.lesieurcristal.b2bportal.order.connector.ErpOrderConnector;
import com.lesieurcristal.b2bportal.order.connector.MockErpOrderConnector;
import com.lesieurcristal.b2bportal.order.dto.OrderResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;
import com.lesieurcristal.b2bportal.order.service.OrderService;
import com.lesieurcristal.b2bportal.repository.OrderRepository;
import com.lesieurcristal.b2bportal.repository.OrderStatusRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    private ErpOrderConnector erpOrderConnector;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        erpOrderConnector = new MockErpOrderConnector(orderRepository, orderStatusRepository);
        orderService = new OrderService(erpOrderConnector, orderRepository);
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

        when(orderRepository.findByCustomerCustomerNumberOrderByOrderDateDesc("CUST0001"))
                .thenReturn(List.of(order));

        List<OrderResponseDto> result = orderService.getOrdersForCurrentUser();

        assertThat(result).hasSize(1);
        OrderResponseDto dto = result.get(0);
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

        when(orderRepository.findByOrderNumberWithInvoice("4500010001")).thenReturn(Optional.of(order));
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

        when(orderRepository.findByOrderNumberWithInvoice("4500010002")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderStatus("4500010002"))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("Commande introuvable");
    }
}
