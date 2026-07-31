package com.lesieurcristal.b2bportal.auth.service;

import com.lesieurcristal.b2bportal.auth.AuthErrorCodes;
import com.lesieurcristal.b2bportal.auth.AuthException;
import com.lesieurcristal.b2bportal.auth.AuthMapper;
import com.lesieurcristal.b2bportal.auth.UserAuthSupport;
import com.lesieurcristal.b2bportal.auth.dto.ActivateAccountRequest;
import com.lesieurcristal.b2bportal.auth.dto.ActivateAccountResponse;
import com.lesieurcristal.b2bportal.auth.dto.LoginRequest;
import com.lesieurcristal.b2bportal.auth.dto.LoginResponse;
import com.lesieurcristal.b2bportal.config.JwtProperties;
import com.lesieurcristal.b2bportal.entity.app.AccountActivationToken;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.repository.AccountActivationTokenRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.ActivationTokenGenerator;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.JwtService;
import com.lesieurcristal.b2bportal.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

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
            if (!UserAuthSupport.isActive(user)) {
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

        OffsetDateTime now = OffsetDateTime.now();
        if (activationToken.isExpired(now)) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    AuthErrorCodes.ACTIVATION_TOKEN_EXPIRED,
                    "Activation link has expired. Contact your administrator for a new invitation."
            );
        }

        User user = activationToken.getUser();
        if (UserAuthSupport.isActive(user)) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    AuthErrorCodes.ACCOUNT_ALREADY_ACTIVE,
                    "Account is already active. You can log in."
            );
        }

        user.setPasswordHash(passwordHasher.hash(request.password()));
        user.setIsActive(true);
        activationToken.setUsedAt(now);

        userRepository.save(user);
        activationTokenRepository.save(activationToken);

        AuthenticatedUser principal = new AuthenticatedUser(user);
        String accessToken = jwtService.generateToken(principal);

        return new ActivateAccountResponse(
                "Account activated successfully. You can now log in.",
                accessToken,
                "Bearer",
                AuthMapper.toUserResponse(user)
        );
    }
}
