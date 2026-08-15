package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByCustomer_CustomerNumber(String customerNumber);

    List<User> findByRole(UserRole role);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}
