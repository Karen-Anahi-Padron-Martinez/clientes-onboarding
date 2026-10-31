package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.EmitirCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.request.ValidarCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.response.CodigoVerificacionResponse;

import java.math.BigDecimal;

/**
 * Contrato de servicio para la autenticación de dos factores (2FA) mediante códigos numéricos
 * de un solo uso enviados por correo electrónico (Brevo) para autorizar pagos de servicios y recargas.
 */
public interface CodigoVerificacionService {

    /**
     * Genera un código de 6 dígitos numéricos con SecureRandom, guarda SOLO su hash (BCrypt),
     * vincula el hash de la operación exacta, invalida códigos anteriores de la cuenta,
     * y envía el código al correo registrado en la entidad Cliente a través de Brevo.
     *
     * @param request Datos de la operación y cuenta
     * @return CodigoVerificacionResponse con estado de envío, correo enmascarado y expiración
     */
    CodigoVerificacionResponse emitirCodigo(EmitirCodigo2FARequest request);

    /**
     * Valida y consume el código ingresado por el usuario, comprobando que pertenezca
     * a la operación exacta, no haya expirado, no haya sido usado y no haya superado
     * el límite de 3 intentos fallidos.
     *
     * @param request Datos del código y de la transacción
     * @return true si el código es correcto y fue consumido con éxito
     */
    boolean validarCodigo(ValidarCodigo2FARequest request);

    /**
     * Calcula el hash SHA-256 de la operación con la fórmula:
     * hash(numeroCuenta + idProducto + idServicio + referencia/telefono + monto).
     */
    String calcularHashOperacion(String numeroCuenta, Integer idProducto, Integer idServicio, String referencia, String telefono, BigDecimal monto);
}
