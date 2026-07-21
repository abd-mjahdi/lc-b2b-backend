package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.AccountActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, Long> {

    Optional<AccountActivationToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    void deleteByUser_IdAndUsedAtIsNull(Long userId);
}
