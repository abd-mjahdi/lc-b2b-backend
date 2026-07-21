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
                .orElseThrow(() -> new AuthException(
                        HttpStatus.BAD_REQUEST,
                        AuthErrorCodes.CUSTOMER_NOT_FOUND,
                        "No ERP customer found for customer number: " + request.customerNumber()
                ));
        if (userRepository.existsByLogin(request.login())) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    AuthErrorCodes.LOGIN_ALREADY_EXISTS,
                    "Login is already in use"
            );
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    AuthErrorCodes.EMAIL_ALREADY_EXISTS,
                    "Email is already in use"
            );
        }

        User user = User.builder()
                .customer(customer)
                .lastName(request.lastName())
                .firstName(request.firstName())
                .email(request.email())
                .phone(request.phone())
                .login(request.login())
                .role(UserRole.CLIENT)
                .language(request.language() != null && !request.language().isBlank()
                        ? request.language()
                        : "fr")
                .isActive(false)
                .passwordHash(passwordHasher.hash(activationTokenGenerator.generateRawToken()))
                .build();

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
}
