package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.ConfigurarGoogle2FAResponse;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.DatosSeguridadBiometria;
import com.kapm.onboarding_clientes.exception.AccesoDenegadoException;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de autenticación de dos factores (2FA) con Google Authenticator (TOTP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Google2FAService {

    public static final String METODO_GOOGLE = "GOOGLE_AUTHENTICATOR";
    public static final String METODO_CORREO = "CORREO";

    private final DatosSeguridadBiometriaRepository seguridadRepository;
    private final ClienteRepository clienteRepository;
    private final SeguridadBiometriaService seguridadBiometriaService;
    private final TotpService totpService;

    @Transactional(readOnly = true)
    public boolean estaHabilitado(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return seguridadRepository.findByUsername(username.trim())
                .map(s -> Boolean.TRUE.equals(s.getTotpHabilitado())
                        && s.getTotpSecret() != null && !s.getTotpSecret().isBlank())
                .orElse(false);
    }

    @Transactional
    public ConfigurarGoogle2FAResponse iniciarConfiguracion(String username) {
        return generarRespuestaConfiguracion(buscarSeguridad(username), true);
    }

    /**
     * Reutiliza el secreto pendiente si el usuario aún no completa la vinculación (evita invalidar un QR recién escaneado).
     */
    @Transactional
    public ConfigurarGoogle2FAResponse iniciarOContinuarConfiguracion(String username) {
        return generarRespuestaConfiguracion(buscarSeguridad(username), false);
    }

    private ConfigurarGoogle2FAResponse generarRespuestaConfiguracion(DatosSeguridadBiometria seguridad, boolean regenerar) {
        String cuenta = clienteRepository.findByDatosSeguridadUsername(seguridad.getUsername())
                .map(Cliente::getCorreo)
                .filter(c -> c != null && !c.isBlank())
                .orElse(seguridad.getUsername());

        String secreto = (!regenerar && seguridad.getTotpSecretPendiente() != null && !seguridad.getTotpSecretPendiente().isBlank())
                ? seguridad.getTotpSecretPendiente()
                : totpService.generarSecretoBase32();
        if (!secreto.equals(seguridad.getTotpSecretPendiente())) {
            seguridad.setTotpSecretPendiente(secreto);
            seguridadRepository.save(seguridad);
        }

        String uri = totpService.construirOtpAuthUri(TotpService.EMISOR_DEFAULT, cuenta, secreto);
        String qr = totpService.generarQrDataUrl(uri);

        log.info("Configuración de Google Authenticator lista para usuario");
        return ConfigurarGoogle2FAResponse.builder()
                .habilitado(Boolean.TRUE.equals(seguridad.getTotpHabilitado()))
                .secreto(secreto)
                .otpauthUri(uri)
                .qrCodeDataUrl(qr)
                .emisor(TotpService.EMISOR_DEFAULT)
                .cuenta(cuenta)
                .build();
    }

    /**
     * Si el usuario ya vinculó Google Authenticator valida su código activo; si es su primera vez, activa la vinculación.
     */
    @Transactional
    public void validarOActivar(String username, String codigo) {
        if (estaHabilitado(username)) {
            validarCodigoActivo(username, codigo);
        } else {
            activar(username, codigo);
        }
    }

    @Transactional
    public void activar(String username, String codigo) {
        DatosSeguridadBiometria seguridad = buscarSeguridad(username);
        String secretoPendiente = seguridad.getTotpSecretPendiente();
        if (secretoPendiente == null || secretoPendiente.isBlank()) {
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_NO_ENCONTRADO,
                    "Primero genera el código QR de Google Authenticator para vincular tu cuenta");
        }
        Long paso = totpService.validarCodigo(secretoPendiente, codigo == null ? null : codigo.trim(), null)
                .orElseThrow(() -> new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INVALIDO,
                        "El código de Google Authenticator es incorrecto. Verifica la hora de tu dispositivo e intenta de nuevo"));

        seguridad.setTotpSecret(secretoPendiente);
        seguridad.setTotpSecretPendiente(null);
        seguridad.setTotpHabilitado(true);
        seguridad.setTotpUltimoPaso(paso);
        seguridadRepository.save(seguridad);
        log.info("Google Authenticator activado exitosamente");
    }

    @Transactional
    public void desactivar(String username, String passwordActual, String codigo) {
        DatosSeguridadBiometria seguridad = buscarSeguridad(username);
        if (!Boolean.TRUE.equals(seguridad.getTotpHabilitado()) || seguridad.getTotpSecret() == null) {
            throw new ReglaNegocioException(ErrorMessage.REGLA_NEGOCIO,
                    "Google Authenticator no está activado en tu cuenta");
        }
        if (!seguridadBiometriaService.validarPassword(passwordActual, seguridad.getPasswordHash())) {
            throw new ReglaNegocioException(ErrorMessage.AUTH_PASSWORD_ACTUAL_INCORRECTA);
        }
        totpService.validarCodigo(seguridad.getTotpSecret(), codigo == null ? null : codigo.trim(), seguridad.getTotpUltimoPaso())
                .orElseThrow(() -> new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INVALIDO,
                        "El código de Google Authenticator es incorrecto"));

        seguridad.setTotpHabilitado(false);
        seguridad.setTotpSecret(null);
        seguridad.setTotpSecretPendiente(null);
        seguridad.setTotpUltimoPaso(null);
        seguridadRepository.save(seguridad);
        log.info("Google Authenticator desactivado exitosamente");
    }

    @Transactional
    public void validarCodigoActivo(String username, String codigo) {
        DatosSeguridadBiometria seguridad = buscarSeguridad(username);
        if (!Boolean.TRUE.equals(seguridad.getTotpHabilitado()) || seguridad.getTotpSecret() == null) {
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_NO_ENCONTRADO);
        }
        Long paso = totpService.validarCodigo(seguridad.getTotpSecret(), codigo == null ? null : codigo.trim(), seguridad.getTotpUltimoPaso())
                .orElseThrow(() -> new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INVALIDO,
                        "El código de Google Authenticator es incorrecto o ya fue utilizado"));

        seguridad.setTotpUltimoPaso(paso);
        seguridadRepository.save(seguridad);
    }

    private DatosSeguridadBiometria buscarSeguridad(String username) {
        if (username == null || username.isBlank()) {
            throw new AccesoDenegadoException();
        }
        return seguridadRepository.findByUsername(username.trim())
                .orElseThrow(AccesoDenegadoException::new);
    }
}
