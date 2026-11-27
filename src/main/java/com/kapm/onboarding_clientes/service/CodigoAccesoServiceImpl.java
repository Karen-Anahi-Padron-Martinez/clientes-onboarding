package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.brevo.BrevoEmailClient;
import com.kapm.onboarding_clientes.dto.response.CodigoVerificacionResponse;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.CodigoAcceso;
import com.kapm.onboarding_clientes.entity.PropositoCodigoAcceso;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CodigoAccesoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mismas reglas que el 2FA de pagos: código de 6 dígitos con SecureRandom, solo se guarda su hash BCrypt,
 * un solo uso, expiración, máximo de intentos y límite de solicitudes por ventana de 10 minutos.
 * El destinatario siempre es el correo registrado del cliente, nunca uno enviado en el request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodigoAccesoServiceImpl implements CodigoAccesoService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CodigoAccesoRepository codigoAccesoRepository;
    private final BrevoEmailClient brevoEmailClient;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seguridad.2fa.expiracion-minutos:5}")
    private int expiracionMinutos = 5;

    @Value("${app.seguridad.2fa.max-intentos:3}")
    private int maxIntentos = 3;

    @Value("${app.seguridad.2fa.max-solicitudes-10min:3}")
    private int maxSolicitudes10Min = 3;

    @Override
    @Transactional
    public CodigoVerificacionResponse emitir(String username, Cliente cliente, PropositoCodigoAcceso proposito) {
        if (cliente == null || cliente.getCorreo() == null || cliente.getCorreo().isBlank()) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_NO_ENCONTRADO, "El cliente no tiene un correo electrónico registrado");
        }

        long recientes = codigoAccesoRepository.countByUsernameAndPropositoAndCreadoEnAfter(
                username, proposito, LocalDateTime.now().minusMinutes(10));
        if (recientes >= maxSolicitudes10Min) {
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_LIMITE_SOLICITUDES);
        }

        List<CodigoAcceso> anteriores = codigoAccesoRepository.findByUsernameAndPropositoAndUsadoFalse(username, proposito);
        anteriores.forEach(c -> c.setUsado(true));
        codigoAccesoRepository.saveAll(anteriores);

        String codigo = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        LocalDateTime expiraEn = LocalDateTime.now().plusMinutes(expiracionMinutos);

        codigoAccesoRepository.save(CodigoAcceso.builder()
                .username(username)
                .proposito(proposito)
                .hashCodigo(passwordEncoder.encode(codigo))
                .expiraEn(expiraEn)
                .build());

        // Nunca se registra el código en claro
        brevoEmailClient.enviarCorreo(cliente.getCorreo(), asunto(proposito), plantillaHtml(cliente.getNombre(), codigo, proposito));
        log.info("Código de acceso {} enviado al correo registrado", proposito);

        return CodigoVerificacionResponse.builder()
                .enviado(true)
                .correoEnmascarado(BrevoEmailClient.enmascararCorreo(cliente.getCorreo()))
                .expiraEn(expiraEn)
                .mensaje("Enviamos un código de verificación a tu correo registrado")
                .build();
    }

    /**
     * Los intentos fallidos y la invalidación deben persistir aunque se lance la excepción:
     * si el rollback los deshiciera, el límite de intentos no serviría.
     */
    @Override
    @Transactional(noRollbackFor = ReglaNegocioException.class)
    public void validar(String username, PropositoCodigoAcceso proposito, String codigo) {
        if (codigo == null || !codigo.matches("^\\d{6}$")) {
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INVALIDO, "El código debe tener 6 dígitos");
        }

        CodigoAcceso actual = codigoAccesoRepository.findTopByUsernameAndPropositoOrderByCreadoEnDesc(username, proposito)
                .orElseThrow(() -> new ReglaNegocioException(ErrorMessage.CODIGO_2FA_NO_ENCONTRADO));

        if (Boolean.TRUE.equals(actual.getUsado())) {
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_YA_USADO);
        }
        if (LocalDateTime.now().isAfter(actual.getExpiraEn())) {
            actual.setUsado(true);
            codigoAccesoRepository.save(actual);
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_EXPIRADO);
        }
        if (!passwordEncoder.matches(codigo, actual.getHashCodigo())) {
            int intentos = actual.getIntentos() + 1;
            actual.setIntentos(intentos);
            if (intentos >= maxIntentos) {
                actual.setUsado(true);
                codigoAccesoRepository.save(actual);
                throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INTENTOS_AGOTADOS);
            }
            codigoAccesoRepository.save(actual);
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INVALIDO,
                    "Código incorrecto. Intentos restantes: " + (maxIntentos - intentos));
        }

        actual.setUsado(true);
        codigoAccesoRepository.save(actual);
    }

    private static String asunto(PropositoCodigoAcceso proposito) {
        return proposito == PropositoCodigoAcceso.LOGIN
                ? "Código para iniciar sesión - Banco Digital"
                : "Código para restablecer tu contraseña - Banco Digital";
    }

    private String plantillaHtml(String nombreCliente, String codigo, PropositoCodigoAcceso proposito) {
        String nombre = nombreCliente != null ? nombreCliente : "Estimado cliente";
        String accion = proposito == PropositoCodigoAcceso.LOGIN
                ? "Recibimos un intento de inicio de sesión en tu cuenta. Para continuar, ingresa este código:"
                : "Recibimos una solicitud para restablecer tu contraseña. Tu código es:";
        return "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 8px; color: #333;'>"
                + "<h2 style='color: #1a365d; text-align: center; margin-top: 0;'>Código de Verificación</h2>"
                + "<p>Hola <strong>" + nombre + "</strong>,</p>"
                + "<p>" + accion + "</p>"
                + "<div style='background-color: #f7fafc; border: 2px dashed #cbd5e0; padding: 16px; font-size: 32px; font-weight: bold; letter-spacing: 8px; text-align: center; color: #2b6cb0; border-radius: 6px; margin: 24px 0;'>"
                + codigo
                + "</div>"
                + "<p style='font-size: 14px; color: #4a5568;'>Este código expira en <strong>" + expiracionMinutos + " minutos</strong> y es de un solo uso. No lo compartas con nadie.</p>"
                + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;'/>"
                + "<p style='font-size: 12px; color: #a0aec0; text-align: center;'>Banco Digital - Si no fuiste tú, cambia tu contraseña y comunícate con nosotros.</p>"
                + "</div>";
    }
}
