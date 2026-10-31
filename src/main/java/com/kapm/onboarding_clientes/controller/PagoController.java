package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.AplicarPagoRequest;
import com.kapm.onboarding_clientes.dto.request.SolicitarCodigoPagoRequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.dto.response.SolicitarCodigoPagoResponse;
import com.kapm.onboarding_clientes.service.PagoServicioService;
import com.kapm.onboarding_clientes.util.DatosSensibles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el flujo de pago de servicios y recargas de tiempo aire (PuntoRed / GestoPago).
 * Implementa el flujo en dos fases protegidas:
 * 1. Solicitud de código 2FA enviado al correo registrado en BD mediante Brevo.
 * 2. Aplicación y liquidación del pago con validación 2FA, reserva pesimista de saldo e idempotencia.
 * <p>
 * Reglas arquitecturales:
 * - 100% Request Body, Cero ID de BD en endpoints, Cero PathVariables.
 * - Identificación por CURP o RFC + numeroCuenta.
 * - Requiere JWT de cliente; el servicio verifica que CURP/cuenta pertenezcan al titular del token.
 */
@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos de Servicios y Recargas", description = "Endpoints seguros para la emisión de códigos 2FA y liquidación de pagos de servicios y recargas TAE (100% Request Body, Cero PathVariable)")
@SecurityRequirement(name = "bearerAuth")
public class PagoController {

    private final PagoServicioService pagoServicioService;

    // =========================================================================
    // PASO 1: SOLICITAR CÓDIGO 2FA POR CORREO
    // =========================================================================

    @PostMapping("/solicitar-codigo")
    @Operation(
            summary = "Paso 1: Solicitar código 2FA para pago/recarga (Request Body)",
            description = "Valida el producto en catálogo PuntoRed (solo recargas TAE y servicios permitidos), " +
                    "cliente activo, cuenta bancaria asociada y saldo disponible suficiente. " +
                    "Genera y envía un código numérico 2FA al correo registrado en la entidad Cliente a través de Brevo. " +
                    "Responde con correo enmascarado y tiempo de expiración."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Código de verificación emitido y enviado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos, saldo insuficiente o regla de negocio no cumplida"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado en el sistema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente o cuenta no encontrada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno o falla en el servicio de correo Brevo")
    })
    public ResponseEntity<ApiResponse<SolicitarCodigoPagoResponse>> solicitarCodigo(
            @Valid @RequestBody SolicitarCodigoPagoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Recibida solicitud de código 2FA para pago/recarga [cuenta: {}, idProducto: {}]",
                DatosSensibles.enmascarar(request.getNumeroCuenta()), request.getIdProducto());

        SolicitarCodigoPagoResponse response = pagoServicioService.solicitarCodigo(request, jwt.getSubject());

        return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
    }

    // =========================================================================
    // PASO 2: APLICAR PAGO CON CÓDIGO 2FA E IDEMPOTENCY KEY
    // =========================================================================

    @PostMapping("/aplicar")
    @Operation(
            summary = "Paso 2: Aplicar pago/recarga con código 2FA e IdempotencyKey (Request Body)",
            description = "Valida idempotencia (si ya se procesó, devuelve el resultado sin cobrar de nuevo), " +
                    "re-valida datos del cliente, valida y consume el código 2FA. " +
                    "En una transacción corta con @Lock pesimista reserva el saldo disponible y crea PagoServicio en PENDIENTE. " +
                    "Fuera de transacción HTTP se comunica con PuntoRed. " +
                    "Posteriormente actualiza en una transacción corta el saldo contable (éxito) o restituye disponible (rechazo). " +
                    "En caso de timeout de red, mantiene PENDIENTE con saldo reservado para resolución por scheduler."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pago procesado o transacción idempotente previamente registrada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Código 2FA incorrecto/expirado, saldo insuficiente o datos inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado en el sistema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente o cuenta no encontrada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno no controlado en el servidor")
    })
    public ResponseEntity<ApiResponse<PagoResponse>> aplicarPago(
            @Valid @RequestBody AplicarPagoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Recibida solicitud de aplicación de pago [cuenta: {}, idProducto: {}]",
                DatosSensibles.enmascarar(request.getNumeroCuenta()), request.getIdProducto());

        PagoResponse response = pagoServicioService.aplicarPago(request, jwt.getSubject());

        return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
    }
}
