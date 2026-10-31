package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.config.JwtConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Emite el JWT de sesión del cliente. Solo lleva el username (sub) y el id de sesión (jti):
 * ningún dato personal (CURP, cuenta, saldo) viaja en el token.
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final long expiracionMinutos;

    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${app.security.jwt.expiracion-minutos:15}") long expiracionMinutos) {
        this.jwtEncoder = jwtEncoder;
        this.expiracionMinutos = expiracionMinutos;
    }

    public TokenEmitido emitir(String username, String sesionId) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(expiracionMinutos, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .subject(username)
                .id(sesionId)
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim(JwtConfig.CLAIM_ROLES, List.of("CLIENTE"))
                .build();

        String valor = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenEmitido(valor, expira);
    }

    public record TokenEmitido(String valor, Instant expiraEn) {
    }
}
