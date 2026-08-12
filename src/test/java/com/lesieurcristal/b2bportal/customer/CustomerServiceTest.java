package com.lesieurcristal.b2bportal.customer;

import com.lesieurcristal.b2bportal.customer.dto.CustomerProfileResponse;
import com.lesieurcristal.b2bportal.customer.dto.UpdateCustomerProfileRequest;
import com.lesieurcristal.b2bportal.customer.service.CustomerService;
import com.lesieurcristal.b2bportal.entity.app.ClientChangeLog;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.repository.ClientChangeLogRepository;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ClientChangeLogRepository clientChangeLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private User user;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerNumber("CUST0001")
                .companyName("Épicerie Al Amal SARL")
                .phone("+212522000111")
                .email("contact@alamal-epicerie.ma")
                .city("Casablanca")
                .country("Maroc")
                .build();

        user = User.builder()
                .id(1L)
                .login("aalami")
                .passwordHash("hashed")
                .role(UserRole.CLIENT)
                .customer(customer)
                .isActive(true)
                .build();

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateMyProfile_updatesPhoneAndEmailAndLogsChanges() {
        when(customerRepository.findById("CUST0001")).thenReturn(Optional.of(customer));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(clientChangeLogRepository.findByCustomer_CustomerNumberOrderByChangedAtDesc("CUST0001"))
                .thenReturn(List.of());

        UpdateCustomerProfileRequest request = new UpdateCustomerProfileRequest(
                "+212522999888",
                "nouveau@alamal-epicerie.ma"
        );

        CustomerProfileResponse response = customerService.updateMyProfile(request);

        assertThat(customer.getPhone()).isEqualTo("+212522999888");
        assertThat(customer.getEmail()).isEqualTo("nouveau@alamal-epicerie.ma");
        assertThat(response.getCustomer().getPhone()).isEqualTo("+212522999888");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClientChangeLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(clientChangeLogRepository).saveAll(captor.capture());

        List<ClientChangeLog> logs = captor.getValue();
        assertThat(logs).hasSize(2);
        assertThat(logs).extracting(ClientChangeLog::getFieldName)
                .containsExactlyInAnyOrder("phone", "email");
        assertThat(logs).allMatch(l -> l.getChangedByUser() != null
                && l.getChangedByUser().getLogin().equals("aalami"));
    }

    @Test
    void updateMyProfile_skipsUnchangedFields() {
        when(customerRepository.findById("CUST0001")).thenReturn(Optional.of(customer));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(clientChangeLogRepository.findByCustomer_CustomerNumberOrderByChangedAtDesc("CUST0001"))
                .thenReturn(List.of());

        UpdateCustomerProfileRequest request = new UpdateCustomerProfileRequest(
                "+212522000111",
                "contact@alamal-epicerie.ma"
        );

        customerService.updateMyProfile(request);

        verify(customerRepository, never()).save(customer);
        verify(clientChangeLogRepository, never()).saveAll(anyList());
    }

    @Test
    void updateMyProfile_onlyLogsProvidedFields() {
        when(customerRepository.findById("CUST0001")).thenReturn(Optional.of(customer));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(clientChangeLogRepository.findByCustomer_CustomerNumberOrderByChangedAtDesc("CUST0001"))
                .thenReturn(List.of());

        UpdateCustomerProfileRequest request = new UpdateCustomerProfileRequest("+212600000000", null);

        customerService.updateMyProfile(request);

        assertThat(customer.getPhone()).isEqualTo("+212600000000");
        assertThat(customer.getEmail()).isEqualTo("contact@alamal-epicerie.ma");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClientChangeLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(clientChangeLogRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getFieldName()).isEqualTo("phone");
    }
}
