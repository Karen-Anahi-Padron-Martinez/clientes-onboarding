package com.kapm.onboarding_clientes.config;

import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JWT de sesión de clientes firmado con HMAC-SHA256.
 * El secreto se toma de la variable de entorno JWT_SECRET (Base64, mínimo 256 bits) y nunca se registra en logs.
 */
@Slf4j
@Configuration
public class JwtConfig {

    public static final String ISSUER = "onboarding-clientes";
    public static final String CLAIM_ROLES = "roles";
    private static final int LONGITUD_MINIMA_BYTES = 32;

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.security.jwt.secret:}") String secretBase64) {
        byte[] bytes;
        if (secretBase64 == null || secretBase64.isBlank()) {
            // Fallback solo para desarrollo: llave efímera, los tokens dejan de valer al reiniciar
            log.warn("JWT_SECRET no configurado: se generó una llave efímera. Configure JWT_SECRET en cualquier entorno compartido.");
            bytes = new byte[LONGITUD_MINIMA_BYTES];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = Base64.getDecoder().decode(secretBase64.trim());
            if (bytes.length < LONGITUD_MINIMA_BYTES) {
                throw new IllegalStateException("JWT_SECRET debe tener al menos 256 bits (32 bytes) codificados en Base64");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, DatosSeguridadBiometriaRepository seguridadRepository) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                new SesionVigenteValidator(seguridadRepository)));
        return decoder;
    }
}
