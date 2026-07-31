package com.lesieurcristal.b2bportal.auth.service;

import com.lesieurcristal.b2bportal.auth.AuthErrorCodes;
import com.lesieurcristal.b2bportal.auth.AuthException;
import com.lesieurcristal.b2bportal.auth.UserAuthSupport;
import com.lesieurcristal.b2bportal.auth.dto.AdminRegisterClientRequest;
import com.lesieurcristal.b2bportal.auth.dto.AdminRegisterClientResponse;
import com.lesieurcristal.b2bportal.config.ActivationProperties;
import com.lesieurcristal.b2bportal.entity.app.AccountActivationToken;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.notification.AccountActivationEmail;
import com.lesieurcristal.b2bportal.notification.NotificationService;
import com.lesieurcristal.b2bportal.repository.AccountActivationTokenRepository;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.ActivationTokenGenerator;
import com.lesieurcristal.b2bportal.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AdminUserRegistrationService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountActivationTokenRepository activationTokenRepository;
    private final ActivationTokenGenerator activationTokenGenerator;
    private final ActivationProperties activationProperties;
    private final PasswordHasher passwordHasher;
    private final NotificationService notificationService;

    @Transactional
    public AdminRegisterClientResponse registerClient(AdminRegisterClientRequest request) {
        Customer customer = customerRepository.findById(request.customerNumber())
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .customerNumber(request.customerNumber())
                            .companyName("Entreprise " + request.customerNumber())
                            .city("Casablanca")
                            .country("Maroc")
                            .build();
                    return customerRepository.save(newCustomer);
                });
        User user = userRepository.findByLogin(request.login())
                .orElseGet(() -> userRepository.findByEmail(request.email())
                        .orElseGet(() -> User.builder().build()));

        user.setCustomer(customer);
        user.setLastName(request.lastName());
        user.setFirstName(request.firstName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setLogin(request.login());
        user.setRole(UserRole.CLIENT);
        user.setLanguage(request.language() != null && !request.language().isBlank()
                ? request.language()
                : "fr");
        user.setIsActive(false);
        user.setPasswordHash(passwordHasher.hash(activationTokenGenerator.generateRawToken()));

        user = userRepository.save(user);

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(Duration.ofMillis(activationProperties.tokenExpirationMs()));
        String rawToken = activationTokenGenerator.generateRawToken();

        activationTokenRepository.deleteByUser_IdAndUsedAtIsNull(user.getId());

        AccountActivationToken activationToken = AccountActivationToken.builder()
                .user(user)
                .tokenHash(activationTokenGenerator.hashToken(rawToken))
                .expiresAt(expiresAt)
                .build();
        activationTokenRepository.save(activationToken);

        String activationUrl = activationProperties.buildActivationUrl(rawToken);
        notificationService.sendAccountActivationEmail(new AccountActivationEmail(
                user.getEmail(),
                user.getFirstName(),
                user.getLogin(),
                activationUrl,
                expiresAt
        ));

        return new AdminRegisterClientResponse(
                user.getId(),
                user.getLogin(),
                user.getEmail(),
                UserAuthSupport.customerNumber(user),
                UserAuthSupport.isActive(user),
                now,
                "Client account created. An activation email has been sent."
        );
    }

    @Transactional
    public com.lesieurcristal.b2bportal.auth.dto.SendActivationEmailResponse sendActivationEmail(
            Long userId,
            com.lesieurcristal.b2bportal.auth.dto.SendActivationEmailRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND,
                        AuthErrorCodes.USER_NOT_FOUND,
                        "User not found with ID: " + userId
                ));

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(Duration.ofMillis(activationProperties.tokenExpirationMs()));
        String rawToken = activationTokenGenerator.generateRawToken();

        activationTokenRepository.deleteByUser_IdAndUsedAtIsNull(user.getId());

        AccountActivationToken activationToken = AccountActivationToken.builder()
                .user(user)
                .tokenHash(activationTokenGenerator.hashToken(rawToken))
                .expiresAt(expiresAt)
                .build();
        activationTokenRepository.save(activationToken);

        String activationUrl = activationProperties.buildActivationUrl(rawToken);
        String targetEmail = (request != null && request.recipientEmail() != null && !request.recipientEmail().isBlank())
                ? request.recipientEmail()
                : user.getEmail();

        notificationService.sendAccountActivationEmail(new AccountActivationEmail(
                targetEmail,
                user.getFirstName(),
                user.getLogin(),
                activationUrl,
                expiresAt
        ));

        return new com.lesieurcristal.b2bportal.auth.dto.SendActivationEmailResponse(
                user.getId(),
                targetEmail,
                activationUrl,
                now,
                "Activation link successfully sent to " + targetEmail
        );
    }
}
