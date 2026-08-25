package com.lesieurcristal.b2bportal.config;

import com.lesieurcristal.b2bportal.storage.S3Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, ActivationProperties.class, S3Properties.class})
public class ApplicationConfig {

    private static final List<String> LOCAL_ORIGIN_PATTERNS = List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://[::1]:*",
            "http://192.168.*.*:*",
            "http://10.*.*.*:*",
            "http://172.16.*.*:*",
            "http://172.17.*.*:*",
            "http://172.18.*.*:*",
            "http://172.19.*.*:*",
            "http://172.20.*.*:*",
            "http://172.21.*.*:*",
            "http://172.22.*.*:*",
            "http://172.23.*.*:*",
            "http://172.24.*.*:*",
            "http://172.25.*.*:*",
            "http://172.26.*.*:*",
            "http://172.27.*.*:*",
            "http://172.28.*.*:*",
            "http://172.29.*.*:*",
            "http://172.30.*.*:*",
            "http://172.31.*.*:*"
    );

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> patterns = new ArrayList<>(LOCAL_ORIGIN_PATTERNS);
        if (corsProperties.allowedOrigins() != null) {
            corsProperties.allowedOrigins().stream()
                    .filter(origin -> origin != null && !origin.isBlank())
                    .forEach(patterns::add);
        }
        configuration.setAllowedOriginPatterns(patterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
