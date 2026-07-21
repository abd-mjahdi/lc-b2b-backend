package com.example.portail_b2b.auth.service;

import com.example.portail_b2b.auth.AuthErrorCodes;
import com.example.portail_b2b.auth.AuthException;
import com.example.portail_b2b.auth.AuthMapper;
import com.example.portail_b2b.auth.dto.ActivateAccountRequest;
import com.example.portail_b2b.auth.dto.ActivateAccountResponse;
import com.example.portail_b2b.auth.dto.LoginRequest;
import com.example.portail_b2b.auth.dto.LoginResponse;
import com.example.portail_b2b.config.JwtProperties;
import com.example.portail_b2b.entity.AccountActivationToken;
import com.example.portail_b2b.entity.User;
import com.example.portail_b2b.repository.AccountActivationTokenRepository;
import com.example.portail_b2b.repository.UserRepository;
import com.example.portail_b2b.security.ActivationTokenGenerator;
import com.example.portail_b2b.security.AuthenticatedUser;
import com.example.portail_b2b.security.JwtService;
import com.example.portail_b2b.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordHasher passwordHasher;
    private final AccountActivationTokenRepository activationTokenRepository;
    private final UserRepository userRepository;
    private final ActivationTokenGenerator activationTokenGenerator;

    public LoginResponse login(LoginRequest request) {
        userRepository.findByLogin(request.login()).ifPresent(user -> {
            if (!user.isActive()) {
                throw new AuthException(
                        HttpStatus.FORBIDDEN,
                        AuthErrorCodes.ACCOUNT_NOT_ACTIVATED,
                        "Account is not activated yet. Check your email for the activation link."
                );
            }
        });

        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.login(), request.password())
            );
            AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
            String accessToken = jwtService.generateToken(principal);
            User user = userRepository.findByLogin(principal.getLogin())
                    .orElseThrow(() -> new AuthException(
                            HttpStatus.UNAUTHORIZED,
                            AuthErrorCodes.INVALID_CREDENTIALS,
                            "Invalid login or password"
                    ));

            return new LoginResponse(
                    accessToken,
                    "Bearer",
                    jwtProperties.expirationMs(),
                    AuthMapper.toUserResponse(user)
            );
        } catch (BadCredentialsException ex) {
            throw new AuthException(
                    HttpStatus.UNAUTHORIZED,
                    AuthErrorCodes.INVALID_CREDENTIALS,
                    "Invalid login or password"
            );
        } catch (DisabledException ex) {
            throw new AuthException(
                    HttpStatus.FORBIDDEN,
                    AuthErrorCodes.ACCOUNT_NOT_ACTIVATED,
                    "Account is not activated yet. Check your email for the activation link."
            );
        }
    }

    @Transactional
    public ActivateAccountResponse activateAccount(ActivateAccountRequest request) {
        String tokenHash = activationTokenGenerator.hashToken(request.token());
        AccountActivationToken activationToken = activationTokenRepository
                .findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.BAD_REQUEST,
                        AuthErrorCodes.INVALID_ACTIVATION_TOKEN,
                        "Invalid or already used activation link"
                ));

        Instant now = Instant.now();
        if (activationToken.isExpired(now)) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    AuthErrorCodes.ACTIVATION_TOKEN_EXPIRED,
                    "Activation link has expired. Contact your administrator for a new invitation."
            );
        }

        User user = activationToken.getUser();
        if (user.isActive()) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    AuthErrorCodes.ACCOUNT_ALREADY_ACTIVE,
                    "Account is already active. You can log in."
            );
        }

        user.setPasswordHash(passwordHasher.hash(request.password()));
        user.setActive(true);
        activationToken.setUsedAt(now);

        userRepository.save(user);
        activationTokenRepository.save(activationToken);

        return new ActivateAccountResponse(
                "Account activated successfully. You can now log in.",
                AuthMapper.toUserResponse(user)
        );
    }
}
