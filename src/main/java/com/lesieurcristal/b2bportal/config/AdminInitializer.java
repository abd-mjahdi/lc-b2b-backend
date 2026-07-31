package com.lesieurcristal.b2bportal.config;

import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordHasher passwordHasher;

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = "rachad.khattabi@gmail.com";
        String adminLogin = "rachad.khattabi@gmail.com";
        String rawPassword = "rachad123";

        // Seed mock customer for demo
        String demoCustomerNumber = "CU-2026-89A";
        if (!customerRepository.existsById(demoCustomerNumber)) {
            log.info("[SEED] Creating mock customer {}", demoCustomerNumber);
            Customer customer = Customer.builder()
                    .customerNumber(demoCustomerNumber)
                    .companyName("Agro-Distribution SARL")
                    .city("Casablanca")
                    .country("Maroc")
                    .build();
            customerRepository.save(customer);
        }

        // Check by login or email
        User user = userRepository.findByEmail(adminEmail)
                .orElseGet(() -> userRepository.findByLogin(adminLogin).orElse(null));

        if (user == null) {
            log.info("[SEED] Creating Admin account for {}", adminEmail);
            user = User.builder()
                    .customer(null)
                    .firstName("Rachad")
                    .lastName("Khattabi")
                    .email(adminEmail)
                    .login(adminLogin)
                    .phone("+212600000000")
                    .role(UserRole.ADMIN)
                    .language("fr")
                    .isActive(true)
                    .passwordHash(passwordHasher.hash(rawPassword))
                    .build();
            userRepository.save(user);
            log.info("[SEED] Admin account {} successfully created!", adminEmail);
        } else {
            // Ensure password and role are up to date
            user.setRole(UserRole.ADMIN);
            user.setIsActive(true);
            user.setPasswordHash(passwordHasher.hash(rawPassword));
            userRepository.save(user);
            log.info("[SEED] Admin account {} successfully updated with target credentials!", adminEmail);
        }
    }
}
