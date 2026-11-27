package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.DetallePagoRequest;
import com.kapm.onboarding_clientes.dto.request.HistorialPagosRequest;
import com.kapm.onboarding_clientes.dto.request.ValidarReferenciaRequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.PageResponse;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.dto.response.ProductoPagoResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionReferenciaResponse;
import com.kapm.onboarding_clientes.service.PagoConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consultas del cliente autenticado para el frontend de pagos. Todo lo que depende del titular se resuelve
 * con el subject del JWT; el body nunca decide de quién son los datos.
 */
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos de Servicios y Recargas - Consultas", description = "Catálogo vendible, validación previa de referencia, historial y detalle de operaciones propias (Request Body, Cero PathVariable)")
@SecurityRequirement(name = "bearerAuth")
public class PagoConsultaController {

    private final PagoConsultaService pagoConsultaService;

    @GetMapping("/catalogo")
    @Operation(summary = "Catálogo de productos y servicios disponibles", description = "Productos activos del catálogo local de PuntoRed (solo recargas y servicios), con categoría, logotipo y límites de monto.")
    public ResponseEntity<ApiResponse<List<ProductoPagoResponse>>> catalogo() {
        return ResponseEntity.ok(ApiResponse.exito("Catálogo consultado exitosamente", pagoConsultaService.listarCatalogo()));
    }

    @PostMapping("/referencia/validar")
    @Operation(summary = "Validar referencia o teléfono antes de pagar", description = "Aplica las mismas reglas de formato que el pago (longitud y expresión regular del producto). Responde 200 con valida=false si el formato no es correcto; 400 si el producto no está permitido.")
    public ResponseEntity<ApiResponse<ValidacionReferenciaResponse>> validarReferencia(
            @Valid @RequestBody ValidarReferenciaRequest request) {
        ValidacionReferenciaResponse response = pagoConsultaService.validarReferencia(request);
        return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
    }

    @PostMapping("/historial")
    @Operation(summary = "Historial de operaciones del titular", description = "Pagos y recargas del titular del token, del más reciente al más antiguo, con filtros opcionales por estatus y rango de fechas.")
    public ResponseEntity<ApiResponse<PageResponse<PagoResponse>>> historial(
            @Valid @RequestBody HistorialPagosRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.exito("Historial consultado exitosamente",
                pagoConsultaService.historial(request, jwt.getSubject())));
    }

    @PostMapping("/detalle")
    @Operation(summary = "Detalle de una operación propia por folio (UPC)", description = "Devuelve el estatus vigente; sirve para consultar después un pago PENDIENTE. Un folio de otro titular responde 404.")
    public ResponseEntity<ApiResponse<PagoResponse>> detalle(
            @Valid @RequestBody DetallePagoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.exito("Detalle de la operación",
                pagoConsultaService.detalle(request.getUpc(), jwt.getSubject())));
    }
}
