package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    List<User> findByCustomer_CustomerNumber(String customerNumber);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}
