package com.example.portail_b2b.security;

import com.example.portail_b2b.config.JwtProperties;
import com.example.portail_b2b.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_CUSTOMER_NUMBER = "customer_number";

    private final JwtProperties jwtProperties;

    public String generateToken(AuthenticatedUser user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.expirationMs());

        var builder = Jwts.builder()
                .subject(user.getLogin())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(now)
                .expiration(expiry);

        if (user.getCustomerNumber() != null) {
            builder.claim(CLAIM_CUSTOMER_NUMBER, user.getCustomerNumber());
        }

        return builder.signWith(signingKey()).compact();
    }

    public String extractLogin(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, AuthenticatedUser user) {
        try {
            Claims claims = parseClaims(token);
            String subject = claims.getSubject();
            Date expiration = claims.getExpiration();
            return subject.equals(user.getLogin())
                    && expiration.after(new Date())
                    && user.isEnabled();
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Role extractRole(String token) {
        String role = parseClaims(token).get(CLAIM_ROLE, String.class);
        return Role.valueOf(role);
    }

    Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return true;
        }
    }

    private SecretKey signingKey() {
        byte[] keyBytes = decodeSecret(jwtProperties.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static byte[] decodeSecret(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
