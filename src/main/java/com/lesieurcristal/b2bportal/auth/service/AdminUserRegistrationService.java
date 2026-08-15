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
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public AdminRegisterClientResponse registerClient(AdminRegisterClientRequest request) {
        String login = request.login().trim();
        String email = request.email().trim();

        if (userRepository.existsByLogin(login)) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    AuthErrorCodes.LOGIN_ALREADY_EXISTS,
                    "Un compte avec l'identifiant « " + login + " » existe déjà."
            );
        }
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    AuthErrorCodes.EMAIL_ALREADY_EXISTS,
                    "Un compte avec l'email « " + email + " » existe déjà."
            );
        }

        Customer customer = customerRepository.findById(request.customerNumber())
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .customerNumber(request.customerNumber())
                            .companyName(request.companyName())
                            .city("Casablanca")
                            .country("Maroc")
                            .build();
                    return customerRepository.save(newCustomer);
                });

        User user = User.builder()
                .customer(customer)
                .lastName(request.lastName())
                .firstName(request.firstName())
                .email(email)
                .phone(request.phone())
                .login(login)
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

        if (Boolean.TRUE.equals(user.getIsDeactivated())) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    AuthErrorCodes.ACCOUNT_DEACTIVATED,
                    "Cannot send activation email to a deactivated user."
            );
        }

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
        String targetEmail = resolveActivationRecipient(user, request);

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
                now,
                "Activation link successfully sent to " + targetEmail
        );
    }

    /**
     * Activation links may only go to the account's registered email.
     * An explicit recipient is accepted only when it matches that address.
     */
    private static String resolveActivationRecipient(
            User user,
            com.lesieurcristal.b2bportal.auth.dto.SendActivationEmailRequest request
    ) {
        String accountEmail = user.getEmail();
        if (accountEmail == null || accountEmail.isBlank()) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    AuthErrorCodes.INVALID_RECIPIENT_EMAIL,
                    "Le compte n'a pas d'email enregistré pour l'activation."
            );
        }
        if (request == null || request.recipientEmail() == null || request.recipientEmail().isBlank()) {
            return accountEmail.trim();
        }
        String requested = request.recipientEmail().trim();
        if (!accountEmail.trim().equalsIgnoreCase(requested)) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    AuthErrorCodes.INVALID_RECIPIENT_EMAIL,
                    "Le lien d'activation ne peut être envoyé qu'à l'email du compte (« "
                            + accountEmail + " »)."
            );
        }
        return accountEmail.trim();
    }

    @Transactional(readOnly = true)
    public java.util.List<com.lesieurcristal.b2bportal.auth.dto.AuthUserResponse> getUsers(String customerNumber) {
        java.util.List<User> users;
        if (customerNumber != null && !customerNumber.isBlank()) {
            users = userRepository.findByCustomer_CustomerNumber(customerNumber);
        } else {
            users = userRepository.findAll();
        }
        return users.stream()
                .map(com.lesieurcristal.b2bportal.auth.AuthMapper::toUserResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public com.lesieurcristal.b2bportal.auth.dto.AuthUserResponse deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND,
                        AuthErrorCodes.USER_NOT_FOUND,
                        "User not found with ID: " + userId
                ));
        user.setIsDeactivated(true);
        user = userRepository.save(user);
        return com.lesieurcristal.b2bportal.auth.AuthMapper.toUserResponse(user);
    }

    @Transactional
    public com.lesieurcristal.b2bportal.auth.dto.AuthUserResponse activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND,
                        AuthErrorCodes.USER_NOT_FOUND,
                        "User not found with ID: " + userId
                ));
        user.setIsDeactivated(false);
        user = userRepository.save(user);
        return com.lesieurcristal.b2bportal.auth.AuthMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public java.util.List<com.lesieurcristal.b2bportal.auth.dto.CustomerAdminResponse> getAllCustomersWithStats() {
        String sql = "SELECT " +
                "    c.customer_number, " +
                "    c.company_name, " +
                "    c.postal_address, " +
                "    c.city, " +
                "    c.country, " +
                "    c.phone, " +
                "    c.email, " +
                "    c.vat_id, " +
                "    (SELECT COUNT(*) FROM erp_mock.orders o WHERE o.customer_number = c.customer_number) as order_count, " +
                "    (SELECT COUNT(*) FROM erp_mock.invoices i WHERE i.customer_number = c.customer_number) as invoice_count, " +
                "    ((SELECT COUNT(*) FROM app.reclamations r WHERE r.customer_number = c.customer_number AND r.status NOT IN ('resolved', 'rejected')) + " +
                "     (SELECT COUNT(*) FROM app.sample_requests s WHERE s.customer_number = c.customer_number AND s.status IN ('new', 'processing'))) as open_req_count, " +
                "    COALESCE((SELECT SUM(i.total_amount) FROM erp_mock.invoices i WHERE i.customer_number = c.customer_number), 0) as total_revenue " +
                "FROM erp_mock.customers c";

        java.util.List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        java.util.List<com.lesieurcristal.b2bportal.auth.dto.CustomerAdminResponse> list = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            list.add(com.lesieurcristal.b2bportal.auth.dto.CustomerAdminResponse.builder()
                    .customerNumber((String) row[0])
                    .companyName((String) row[1])
                    .postalAddress((String) row[2])
                    .city((String) row[3])
                    .country((String) row[4])
                    .phone((String) row[5])
                    .email((String) row[6])
                    .vatId((String) row[7])
                    .orderCount(((Number) row[8]).longValue())
                    .invoiceCount(((Number) row[9]).longValue())
                    .openReqCount(((Number) row[10]).longValue())
                    .totalRevenue(((Number) row[11]).doubleValue())
                    .build());
        }
        return list;
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(String customerNumber) {
        return customerRepository.findById(customerNumber)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND,
                        AuthErrorCodes.CUSTOMER_NOT_FOUND,
                        "Customer not found with number: " + customerNumber
                ));
    }
}
