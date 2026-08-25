package com.lesieurcristal.b2bportal.dispute;

import com.lesieurcristal.b2bportal.dispute.dto.CreateInvoiceDisputeRequest;
import com.lesieurcristal.b2bportal.dispute.dto.InvoiceDisputeResponseDto;
import com.lesieurcristal.b2bportal.dispute.service.DisputeService;
import com.lesieurcristal.b2bportal.entity.app.InvoiceDispute;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.notification.service.PortalNotificationService;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.InvoiceDisputeRepository;
import com.lesieurcristal.b2bportal.repository.InvoiceRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.storage.ObjectStorage;
import com.lesieurcristal.b2bportal.storage.UploadValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private InvoiceDisputeRepository disputeRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PortalNotificationService portalNotificationService;
    @Mock
    private ObjectStorage objectStorage;
    @Mock
    private UploadValidator uploadValidator;

    private DisputeService disputeService;

    private Customer customer;
    private User clientUser;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        disputeService = new DisputeService(
                disputeRepository,
                invoiceRepository,
                customerRepository,
                userRepository,
                portalNotificationService,
                objectStorage,
                uploadValidator
        );

        customer = Customer.builder()
                .customerNumber("CUST0001")
                .companyName("Client 1")
                .build();
        clientUser = User.builder()
                .id(1L)
                .login("client1")
                .passwordHash("x")
                .role(UserRole.CLIENT)
                .customer(customer)
                .isActive(true)
                .build();
        invoice = Invoice.builder()
                .invoiceNumber("900010099")
                .customer(customer)
                .invoiceStatus("paid")
                .invoiceDate(LocalDate.of(2026, 5, 1))
                .paymentDate(LocalDate.of(2026, 5, 20))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsClient() {
        AuthenticatedUser auth = new AuthenticatedUser(clientUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(auth, null, auth.getAuthorities()));
    }

    private void authenticateAsAdmin() {
        User admin = User.builder()
                .id(99L)
                .login("admin")
                .passwordHash("x")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();
        AuthenticatedUser auth = new AuthenticatedUser(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(auth, null, auth.getAuthorities()));
    }

    @Test
    void createDispute_storesPreviousInvoiceStatusAndMarksDisputed() {
        authenticateAsClient();

        when(uploadValidator.validateIfPresent(null)).thenReturn(Optional.empty());
        when(invoiceRepository.findById("900010099")).thenReturn(Optional.of(invoice));
        when(customerRepository.findById("CUST0001")).thenReturn(Optional.of(customer));
        when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
        when(disputeRepository.save(any(InvoiceDispute.class))).thenAnswer(inv -> {
            InvoiceDispute d = inv.getArgument(0);
            d.setId(42L);
            return d;
        });

        CreateInvoiceDisputeRequest req = new CreateInvoiceDisputeRequest(
                InvoiceDispute.DisputeReason.PRICE_DISCREPANCY,
                "Montant incorrect"
        );

        InvoiceDisputeResponseDto result = disputeService.createDispute("900010099", req, null);

        assertThat(result.previousInvoiceStatus()).isEqualTo("paid");
        assertThat(result.status()).isEqualTo(InvoiceDispute.DisputeStatus.PENDING);
        assertThat(result.hasAttachment()).isFalse();

        ArgumentCaptor<InvoiceDispute> disputeCaptor = ArgumentCaptor.forClass(InvoiceDispute.class);
        verify(disputeRepository).save(disputeCaptor.capture());
        assertThat(disputeCaptor.getValue().getPreviousInvoiceStatus()).isEqualTo("paid");

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getInvoiceStatus()).isEqualTo("disputed");
    }

    @Test
    void approveDispute_restoresPreviousPaidStatus() {
        authenticateAsAdmin();

        Invoice disputed = Invoice.builder()
                .invoiceNumber("900010099")
                .customer(customer)
                .invoiceStatus("disputed")
                .paymentDate(LocalDate.of(2026, 5, 20))
                .build();
        InvoiceDispute pending = InvoiceDispute.builder()
                .id(7L)
                .invoiceNumber("900010099")
                .customer(customer)
                .user(clientUser)
                .reason(InvoiceDispute.DisputeReason.OTHER)
                .description("test")
                .status(InvoiceDispute.DisputeStatus.PENDING)
                .previousInvoiceStatus("paid")
                .build();

        when(disputeRepository.findById(7L)).thenReturn(Optional.of(pending));
        when(userRepository.findById(99L)).thenReturn(Optional.of(
                User.builder().id(99L).login("admin").passwordHash("x").role(UserRole.ADMIN).isActive(true).build()));
        when(disputeRepository.save(any(InvoiceDispute.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.findById("900010099")).thenReturn(Optional.of(disputed));

        InvoiceDisputeResponseDto result = disputeService.approveDispute(7L, "OK");

        assertThat(result.status()).isEqualTo(InvoiceDispute.DisputeStatus.APPROVED);
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getInvoiceStatus()).isEqualTo("paid");
    }

    @Test
    void rejectDispute_restoresPreviousPartiallyPaidStatus() {
        authenticateAsAdmin();

        Invoice disputed = Invoice.builder()
                .invoiceNumber("900010100")
                .customer(customer)
                .invoiceStatus("disputed")
                .build();
        InvoiceDispute pending = InvoiceDispute.builder()
                .id(8L)
                .invoiceNumber("900010100")
                .customer(customer)
                .user(clientUser)
                .reason(InvoiceDispute.DisputeReason.QUANTITY_DISCREPANCY)
                .description("qté")
                .status(InvoiceDispute.DisputeStatus.PENDING)
                .previousInvoiceStatus("partially_paid")
                .build();

        when(disputeRepository.findById(8L)).thenReturn(Optional.of(pending));
        when(userRepository.findById(99L)).thenReturn(Optional.of(
                User.builder().id(99L).login("admin").passwordHash("x").role(UserRole.ADMIN).isActive(true).build()));
        when(disputeRepository.save(any(InvoiceDispute.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.findById("900010100")).thenReturn(Optional.of(disputed));

        disputeService.rejectDispute(8L, "Non fondé");

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getInvoiceStatus()).isEqualTo("partially_paid");
    }

    @Test
    void approveDispute_rejectsWhenNotPending() {
        authenticateAsAdmin();

        InvoiceDispute already = InvoiceDispute.builder()
                .id(9L)
                .invoiceNumber("900010099")
                .customer(customer)
                .reason(InvoiceDispute.DisputeReason.OTHER)
                .description("x")
                .status(InvoiceDispute.DisputeStatus.APPROVED)
                .previousInvoiceStatus("paid")
                .build();

        when(disputeRepository.findById(9L)).thenReturn(Optional.of(already));

        assertThatThrownBy(() -> disputeService.approveDispute(9L, "again"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");

        verify(invoiceRepository, never()).save(any());
    }
}
