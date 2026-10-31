package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.brevo.BrevoEmailClient;
import com.kapm.onboarding_clientes.dto.request.EmitirCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.request.ValidarCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.response.CodigoVerificacionResponse;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.CodigoVerificacionPago;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.CodigoVerificacionPagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación del servicio de autenticación de dos factores (2FA) para transacciones de pago.
 * <p>
 * Seguridad y confidencialidad:
 * <ul>
 *   <li>NUNCA se registra en bitácoras el código numérico en claro.</li>
 *   <li>El código se persiste únicamente en formato hash (BCrypt).</li>
 *   <li>El destinatario se obtiene obligatoriamente de la base de datos (Cliente), nunca del request.</li>
 *   <li>El código está condicionado a la operación exacta mediante un hash SHA-256.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodigoVerificacionServiceImpl implements CodigoVerificacionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CodigoVerificacionPagoRepository codigoVerificacionRepository;
    private final ClienteRepository clienteRepository;
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
    public CodigoVerificacionResponse emitirCodigo(EmitirCodigo2FARequest request) {
        if (request == null) {
            throw new ReglaNegocioException(ErrorMessage.PARAMETROS_INVALIDOS, "La solicitud de código 2FA no puede ser nula");
        }
        if (request.getNumeroCuenta() == null || request.getNumeroCuenta().isBlank()) {
            throw new ReglaNegocioException(ErrorMessage.PARAMETROS_INVALIDOS, "El número de cuenta es obligatorio");
        }
        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException(ErrorMessage.PARAMETROS_INVALIDOS, "El monto de la transacción debe ser mayor a cero");
        }

        String numeroCuenta = request.getNumeroCuenta().trim();

        // 1. Obtener cliente de la base de datos (se usa el correo registrado, nunca uno enviado por el request)
        Cliente cliente = buscarClientePorCuentaOCurp(numeroCuenta, request.getCurp());
        String correoRegistrado = cliente.getCorreo();
        if (correoRegistrado == null || correoRegistrado.isBlank()) {
            throw new ReglaNegocioException(ErrorMessage.CLIENTE_NO_ENCONTRADO, "El cliente no tiene un correo electrónico registrado");
        }

        // 2. Control de límite de solicitudes (rate limiting: máx 3 códigos por 10 minutos por cuenta)
        LocalDateTime limiteDiezMinutos = LocalDateTime.now().minusMinutes(10);
        long solicitudesRecientes = codigoVerificacionRepository.countByNumeroCuentaAndCreadoEnAfter(numeroCuenta, limiteDiezMinutos);
        if (solicitudesRecientes >= maxSolicitudes10Min) {
            log.warn("Límite de solicitudes de código 2FA superado para la cuenta {}", enmascararCuenta(numeroCuenta));
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_LIMITE_SOLICITUDES);
        }

        // 3. Invalidar todos los códigos activos anteriores del mismo cliente/cuenta
        List<CodigoVerificacionPago> codigosAnteriores = codigoVerificacionRepository.findByNumeroCuentaAndUsadoFalse(numeroCuenta);
        if (!codigosAnteriores.isEmpty()) {
            for (CodigoVerificacionPago anterior : codigosAnteriores) {
                anterior.setUsado(true);
            }
            codigoVerificacionRepository.saveAll(codigosAnteriores);
            log.debug("Se invalidaron {} códigos 2FA anteriores para la cuenta {}", codigosAnteriores.size(), enmascararCuenta(numeroCuenta));
        }

        // 4. Generar código de 6 dígitos numéricos con SecureRandom
        String codigo6Digitos = generarCodigo6Digitos();

        // 5. Generar hash BCrypt del código (NUNCA se guarda en texto plano)
        String hashCodigo = passwordEncoder.encode(codigo6Digitos);

        // 6. Generar hashOperacion vinculando la transacción exacta
        String hashOperacion = calcularHashOperacion(
                numeroCuenta,
                request.getIdProducto(),
                request.getIdServicio(),
                request.getReferencia(),
                request.getTelefono(),
                request.getMonto()
        );

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expiraEn = ahora.plusMinutes(expiracionMinutos);

        // 7. Persistir la entidad CodigoVerificacionPago
        CodigoVerificacionPago codigoEntity = CodigoVerificacionPago.builder()
                .cliente(cliente)
                .numeroCuenta(numeroCuenta)
                .curp(cliente.getCurp())
                .hashCodigo(hashCodigo)
                .hashOperacion(hashOperacion)
                .expiraEn(expiraEn)
                .intentos(0)
                .usado(false)
                .creadoEn(ahora)
                .build();

        codigoVerificacionRepository.save(codigoEntity);

        // 8. Enviar correo transaccional con Brevo (NUNCA registrar en logs el código en claro)
        String asunto = "Código de seguridad para autorizar tu pago - Banco Digital";
        String htmlBody = construirPlantillaHtml(cliente.getNombre(), codigo6Digitos, expiracionMinutos);

        brevoEmailClient.enviarCorreo(correoRegistrado, asunto, htmlBody);
        log.info("Código 2FA generado y despachado por correo para cuenta {}", enmascararCuenta(numeroCuenta));

        return CodigoVerificacionResponse.builder()
                .enviado(true)
                .correoEnmascarado(BrevoEmailClient.enmascararCorreo(correoRegistrado))
                .expiraEn(expiraEn)
                .mensaje("Código de verificación enviado al correo registrado del cliente")
                .build();
    }

    @Override
    @Transactional
    public boolean validarCodigo(ValidarCodigo2FARequest request) {
        if (request == null) {
            throw new ReglaNegocioException(ErrorMessage.PARAMETROS_INVALIDOS, "La solicitud de validación no puede ser nula");
        }
        if (request.getNumeroCuenta() == null || request.getNumeroCuenta().isBlank()) {
            throw new ReglaNegocioException(ErrorMessage.PARAMETROS_INVALIDOS, "El número de cuenta es obligatorio");
        }
        if (request.getCodigo() == null || !request.getCodigo().matches("^\\d{6}$")) {
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INVALIDO, "El código de verificación debe constar de 6 dígitos numéricos");
        }

        String numeroCuenta = request.getNumeroCuenta().trim();

        // 1. Calcular hashOperacion para los datos de la transacción recibidos
        String hashOperacionCalculado = calcularHashOperacion(
                numeroCuenta,
                request.getIdProducto(),
                request.getIdServicio(),
                request.getReferencia(),
                request.getTelefono(),
                request.getMonto()
        );

        // 2. Obtener el código más reciente emitido para esta cuenta
        CodigoVerificacionPago codigoEntity = codigoVerificacionRepository
                .findTopByNumeroCuentaOrderByCreadoEnDesc(numeroCuenta)
                .orElseThrow(() -> new ReglaNegocioException(ErrorMessage.CODIGO_2FA_NO_ENCONTRADO));

        // 3. Regla: Código de un solo uso (ya consumido o invalidado)
        if (Boolean.TRUE.equals(codigoEntity.getUsado())) {
            log.warn("Intento de reutilizar código 2FA para cuenta {}", enmascararCuenta(numeroCuenta));
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_YA_USADO);
        }

        // 4. Regla: Máximo 3 intentos fallidos y luego se invalida
        if (codigoEntity.getIntentos() >= maxIntentos) {
            codigoEntity.setUsado(true);
            codigoVerificacionRepository.save(codigoEntity);
            log.warn("Código 2FA bloqueado por intentos agotados para cuenta {}", enmascararCuenta(numeroCuenta));
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INTENTOS_AGOTADOS);
        }

        // 5. Regla: Expiración de tiempo (por defecto 5 minutos)
        if (LocalDateTime.now().isAfter(codigoEntity.getExpiraEn())) {
            codigoEntity.setUsado(true);
            codigoVerificacionRepository.save(codigoEntity);
            log.warn("Código 2FA expirado para cuenta {}", enmascararCuenta(numeroCuenta));
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_EXPIRADO);
        }

        // 6. Regla: Operación exacta (hashOperacion)
        if (!codigoEntity.getHashOperacion().equals(hashOperacionCalculado)) {
            log.warn("Código 2FA presentado para una operación distinta en cuenta {}", enmascararCuenta(numeroCuenta));
            throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_OPERACION_DISTINTA);
        }

        // 7. Comparación del código numérico contra el hash BCrypt
        boolean coincide = passwordEncoder.matches(request.getCodigo(), codigoEntity.getHashCodigo());
        if (!coincide) {
            int nuevoIntentos = codigoEntity.getIntentos() + 1;
            codigoEntity.setIntentos(nuevoIntentos);

            if (nuevoIntentos >= maxIntentos) {
                codigoEntity.setUsado(true); // Se invalida definitivamente
                codigoVerificacionRepository.save(codigoEntity);
                log.warn("Código 2FA invalidado tras alcanzar {} intentos fallidos en cuenta {}", nuevoIntentos, enmascararCuenta(numeroCuenta));
                throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INTENTOS_AGOTADOS);
            } else {
                codigoVerificacionRepository.save(codigoEntity);
                int intentosRestantes = maxIntentos - nuevoIntentos;
                log.warn("Intento fallido de código 2FA (intento {}/{}) para cuenta {}", nuevoIntentos, maxIntentos, enmascararCuenta(numeroCuenta));
                throw new ReglaNegocioException(ErrorMessage.CODIGO_2FA_INVALIDO, "Código incorrecto. Intentos restantes: " + intentosRestantes);
            }
        }

        // 8. Código correcto: marcar como usado (consumir código)
        codigoEntity.setUsado(true);
        codigoVerificacionRepository.save(codigoEntity);
        log.info("Código 2FA consumido y validado exitosamente para cuenta {}", enmascararCuenta(numeroCuenta));

        return true;
    }

    @Override
    public String calcularHashOperacion(
            String numeroCuenta,
            Integer idProducto,
            Integer idServicio,
            String referencia,
            String telefono,
            BigDecimal monto) {

        String refOTel = (referencia != null && !referencia.trim().isEmpty())
                ? referencia.trim()
                : (telefono != null ? telefono.trim() : "");

        String cuenta = numeroCuenta != null ? numeroCuenta.trim() : "";
        int prod = idProducto != null ? idProducto : 0;
        int serv = idServicio != null ? idServicio : 0;
        String montoStr = monto != null ? monto.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";

        String raw = cuenta + "|" + prod + "|" + serv + "|" + refOTel + "|" + montoStr;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo criptográfico SHA-256 no disponible", e);
        }
    }

    // =========================================================================
    // MÉTODOS PRIVADOS AUXILIARES
    // =========================================================================

    private Cliente buscarClientePorCuentaOCurp(String numeroCuenta, String curp) {
        if (numeroCuenta != null && !numeroCuenta.isBlank()) {
            var optCliente = clienteRepository.findByCuentaNumeroCuenta(numeroCuenta.trim());
            if (optCliente.isPresent()) {
                return optCliente.get();
            }
        }
        if (curp != null && !curp.isBlank()) {
            return clienteRepository.findByCurp(curp.trim())
                    .orElseThrow(() -> new ReglaNegocioException(ErrorMessage.CLIENTE_NO_ENCONTRADO, "No se encontró el cliente con la CURP indicada"));
        }
        throw new ReglaNegocioException(ErrorMessage.CLIENTE_NO_ENCONTRADO, "No se encontró cliente asociado a la cuenta " + numeroCuenta);
    }

    private String generarCodigo6Digitos() {
        int numero = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", numero);
    }

    private String construirPlantillaHtml(String nombreCliente, String codigo6Digitos, int expiracionMin) {
        String nombre = nombreCliente != null ? nombreCliente : "Estimado cliente";
        return "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 8px; color: #333;'>"
                + "<h2 style='color: #1a365d; text-align: center; margin-top: 0;'>Código de Verificación</h2>"
                + "<p>Hola <strong>" + nombre + "</strong>,</p>"
                + "<p>Has solicitado autorizar un pago de servicio. Tu código de seguridad temporal es:</p>"
                + "<div style='background-color: #f7fafc; border: 2px dashed #cbd5e0; padding: 16px; font-size: 32px; font-weight: bold; letter-spacing: 8px; text-align: center; color: #2b6cb0; border-radius: 6px; margin: 24px 0;'>"
                + codigo6Digitos
                + "</div>"
                + "<p style='font-size: 14px; color: #4a5568;'>Este código expira en <strong>" + expiracionMin + " minutos</strong> y es de un solo uso. No lo compartas con nadie.</p>"
                + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;'/>"
                + "<p style='font-size: 12px; color: #a0aec0; text-align: center;'>Banco Digital - Si tú no solicitaste este código, comunícate con nosotros inmediatamente.</p>"
                + "</div>";
    }

    private String enmascararCuenta(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.length() < 4) {
            return "****";
        }
        return "******" + numeroCuenta.substring(numeroCuenta.length() - 4);
    }
}
