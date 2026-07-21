package com.example.portail_b2b.repository;

import com.example.portail_b2b.entity.AccountActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, Long> {

    Optional<AccountActivationToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    void deleteByUser_IdAndUsedAtIsNull(Long userId);
}
