package com.kapm.onboarding_clientes.config;

import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rechaza tokens cuya sesión ya no es la vigente: tras un logout o un login posterior el token
 * deja de servir aunque su firma y expiración sigan siendo válidas.
 */
@RequiredArgsConstructor
public class SesionVigenteValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error SESION_INVALIDA =
            new OAuth2Error("invalid_token", "La sesión no está vigente", null);

    private final DatosSeguridadBiometriaRepository seguridadRepository;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getSubject() == null || jwt.getId() == null) {
            return OAuth2TokenValidatorResult.failure(SESION_INVALIDA);
        }
        boolean vigente = seguridadRepository.findByUsername(jwt.getSubject())
                .filter(s -> Boolean.TRUE.equals(s.getLoggeado()))
                .filter(s -> jwt.getId().equals(s.getSesionId()))
                .isPresent();
        return vigente ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(SESION_INVALIDA);
    }
}
