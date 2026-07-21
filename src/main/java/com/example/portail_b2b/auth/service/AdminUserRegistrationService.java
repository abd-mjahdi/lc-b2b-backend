package com.example.portail_b2b.auth.service;

import com.example.portail_b2b.auth.AuthErrorCodes;
import com.example.portail_b2b.auth.AuthException;
import com.example.portail_b2b.auth.dto.AdminRegisterClientRequest;
import com.example.portail_b2b.auth.dto.AdminRegisterClientResponse;
import com.example.portail_b2b.config.ActivationProperties;
import com.example.portail_b2b.entity.AccountActivationToken;
import com.example.portail_b2b.entity.Role;
import com.example.portail_b2b.entity.User;
import com.example.portail_b2b.notification.AccountActivationEmail;
import com.example.portail_b2b.notification.NotificationService;
import com.example.portail_b2b.repository.AccountActivationTokenRepository;
import com.example.portail_b2b.repository.ErpCustomerRepository;
import com.example.portail_b2b.repository.UserRepository;
import com.example.portail_b2b.security.ActivationTokenGenerator;
import com.example.portail_b2b.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminUserRegistrationService {

    private final UserRepository userRepository;
    private final ErpCustomerRepository erpCustomerRepository;
    private final AccountActivationTokenRepository activationTokenRepository;
    private final ActivationTokenGenerator activationTokenGenerator;
    private final ActivationProperties activationProperties;
    private final PasswordHasher passwordHasher;
    private final NotificationService notificationService;

    @Transactional
    public AdminRegisterClientResponse registerClient(AdminRegisterClientRequest request) {
        if (!erpCustomerRepository.existsById(request.customerNumber())) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    AuthErrorCodes.CUSTOMER_NOT_FOUND,
                    "No ERP customer found for customer number: " + request.customerNumber()
            );
        }
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

        User user = new User();
        user.setCustomerNumber(request.customerNumber());
        user.setLastName(request.lastName());
        user.setFirstName(request.firstName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setLogin(request.login());
        user.setRole(Role.CLIENT);
        user.setLanguage(request.language() != null && !request.language().isBlank()
                ? request.language()
                : "fr");
        user.setActive(false);
        user.setPasswordHash(passwordHasher.hash(activationTokenGenerator.generateRawToken()));

        user = userRepository.save(user);

        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(activationProperties.tokenExpirationMs());
        String rawToken = activationTokenGenerator.generateRawToken();

        activationTokenRepository.deleteByUser_IdAndUsedAtIsNull(user.getId());

        AccountActivationToken activationToken = new AccountActivationToken();
        activationToken.setUser(user);
        activationToken.setTokenHash(activationTokenGenerator.hashToken(rawToken));
        activationToken.setExpiresAt(expiresAt);
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
                user.getCustomerNumber(),
                user.isActive(),
                now,
                "Client account created. An activation email has been sent."
        );
    }
}
