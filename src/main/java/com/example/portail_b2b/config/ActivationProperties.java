package com.example.portail_b2b.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.activation")
public record ActivationProperties(
        long tokenExpirationMs,
        String frontendBaseUrl,
        String activationPath
) {
    public String buildActivationUrl(String rawToken) {
        String base = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        String path = activationPath.startsWith("/") ? activationPath : "/" + activationPath;
        return base + path + "?token=" + rawToken;
    }
}
