package com.lesieurcristal.b2bportal.auth.service;

import com.lesieurcristal.b2bportal.auth.AuthErrorCodes;
import com.lesieurcristal.b2bportal.auth.AuthException;
import com.lesieurcristal.b2bportal.auth.dto.AdminRegisterClientRequest;
import com.lesieurcristal.b2bportal.auth.dto.SendActivationEmailRequest;
import com.lesieurcristal.b2bportal.auth.dto.SendActivationEmailResponse;
import com.lesieurcristal.b2bportal.config.ActivationProperties;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.notification.AccountActivationEmail;
import com.lesieurcristal.b2bportal.notification.NotificationService;
import com.lesieurcristal.b2bportal.repository.AccountActivationTokenRepository;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.ActivationTokenGenerator;
import com.lesieurcristal.b2bportal.security.PasswordHasher;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AccountActivationTokenRepository activationTokenRepository;
    @Mock
    private ActivationTokenGenerator activationTokenGenerator;
    @Mock
    private ActivationProperties activationProperties;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AdminUserRegistrationService service;

    private AdminRegisterClientRequest request() {
        return new AdminRegisterClientRequest(
                "CUST0001",
                "Épicerie Al Amal SARL",
                "Alami",
                "Ahmed",
                "admin.portal@lesieur.ma",
                "+212600000000",
                "admin.portal",
                "fr"
        );
    }

    @Test
    void registerClient_rejectsExistingLogin() {
        when(userRepository.existsByLogin("admin.portal")).thenReturn(true);

        assertThatThrownBy(() -> service.registerClient(request()))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException auth = (AuthException) ex;
                    org.assertj.core.api.Assertions.assertThat(auth.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    org.assertj.core.api.Assertions.assertThat(auth.getCode()).isEqualTo(AuthErrorCodes.LOGIN_ALREADY_EXISTS);
                });

        verify(userRepository, never()).save(any());
        verify(customerRepository, never()).findById(any());
    }

    @Test
    void registerClient_rejectsExistingEmail() {
        when(userRepository.existsByLogin("admin.portal")).thenReturn(false);
        when(userRepository.existsByEmail("admin.portal@lesieur.ma")).thenReturn(true);

        assertThatThrownBy(() -> service.registerClient(request()))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException auth = (AuthException) ex;
                    org.assertj.core.api.Assertions.assertThat(auth.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    org.assertj.core.api.Assertions.assertThat(auth.getCode()).isEqualTo(AuthErrorCodes.EMAIL_ALREADY_EXISTS);
                });

        verify(userRepository, never()).save(any());
    }

    @Test
    void sendActivationEmail_rejectsArbitraryRecipient() {
        User user = User.builder()
                .id(10L)
                .login("aalami")
                .email("contact@alamal.ma")
                .firstName("Ahmed")
                .role(UserRole.CLIENT)
                .isActive(false)
                .isDeactivated(false)
                .passwordHash("x")
                .build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.sendActivationEmail(
                10L,
                new SendActivationEmailRequest("attacker@evil.com")
        ))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException auth = (AuthException) ex;
                    assertThat(auth.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(auth.getCode()).isEqualTo(AuthErrorCodes.INVALID_RECIPIENT_EMAIL);
                });

        verify(notificationService, never()).sendAccountActivationEmail(any());
    }

    @Test
    void sendActivationEmail_sendsToAccountEmailWithoutReturningUrl() {
        User user = User.builder()
                .id(10L)
                .login("aalami")
                .email("contact@alamal.ma")
                .firstName("Ahmed")
                .role(UserRole.CLIENT)
                .isActive(false)
                .isDeactivated(false)
                .passwordHash("x")
                .build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(activationProperties.tokenExpirationMs()).thenReturn(3_600_000L);
        when(activationTokenGenerator.generateRawToken()).thenReturn("raw-secret-token");
        when(activationTokenGenerator.hashToken("raw-secret-token")).thenReturn("hashed");
        when(activationProperties.buildActivationUrl("raw-secret-token"))
                .thenReturn("https://portal.example/activate?token=raw-secret-token");
        when(activationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SendActivationEmailResponse response = service.sendActivationEmail(10L, null);

        assertThat(response.recipientEmail()).isEqualTo("contact@alamal.ma");
        assertThat(response.message()).doesNotContain("raw-secret-token");
        assertThat(response.message()).doesNotContain("https://portal.example");

        ArgumentCaptor<AccountActivationEmail> emailCaptor = ArgumentCaptor.forClass(AccountActivationEmail.class);
        verify(notificationService).sendAccountActivationEmail(emailCaptor.capture());
        assertThat(emailCaptor.getValue().recipientEmail()).isEqualTo("contact@alamal.ma");
        assertThat(emailCaptor.getValue().activationUrl()).contains("raw-secret-token");
    }
}
