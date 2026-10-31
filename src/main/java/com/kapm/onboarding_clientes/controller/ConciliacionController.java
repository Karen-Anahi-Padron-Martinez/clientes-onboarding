package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.ResolverConciliacionRequest;
import com.kapm.onboarding_clientes.dto.request.VerificarPagoRequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.service.CatalogoPuntoRedSyncService;
import com.kapm.onboarding_clientes.service.ConciliacionPagosService;
import com.kapm.onboarding_clientes.service.VerificacionPagosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Operación interna (rol ADMIN): conciliación de pagos con resultado desconocido y refresco del catálogo.
 */
@RestController
@RequestMapping("/api/admin/pagos")
@RequiredArgsConstructor
@Tag(name = "Administración de Pagos", description = "Conciliación manual de pagos y catálogo PuntoRed (requiere rol ADMIN)")
@SecurityRequirement(name = "basicAuth")
public class ConciliacionController {

    private final ConciliacionPagosService conciliacionPagosService;
    private final CatalogoPuntoRedSyncService catalogoSyncService;
    private final VerificacionPagosService verificacionPagosService;

    @PostMapping("/en-conciliacion")
    @Operation(summary = "Listar pagos con resultado desconocido pendientes de conciliar")
    public ResponseEntity<ApiResponse<List<PagoResponse>>> listarEnConciliacion() {
        List<PagoResponse> pagos = conciliacionPagosService.listarEnConciliacion();
        return ResponseEntity.ok(ApiResponse.exito("Pagos en conciliación", pagos));
    }

    @PostMapping("/verificar")
    @Operation(summary = "Verificar con PuntoRed un pago con resultado desconocido",
            description = "Consulta confirmTx si hay ID_TX o reenvía el mismo UPC dentro de la ventana de duplicados del proveedor. " +
                    "Fuera de ventana el pago se deja EN_CONCILIACION para resolución manual.")
    public ResponseEntity<ApiResponse<PagoResponse>> verificar(@Valid @RequestBody VerificarPagoRequest request) {
        PagoResponse response = verificacionPagosService.verificar(request.getUpc());
        return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
    }

    @PostMapping("/conciliar")
    @Operation(summary = "Resolver un pago en conciliación tras confirmar el resultado con PuntoRed",
            description = "procesadaPorProveedor=true liquida el saldo contable; false libera la reserva del saldo disponible. " +
                    "Solo aplica a pagos PENDIENTE o EN_CONCILIACION: un pago ya cerrado responde 400.")
    public ResponseEntity<ApiResponse<PagoResponse>> conciliar(@Valid @RequestBody ResolverConciliacionRequest request) {
        PagoResponse response = conciliacionPagosService.resolver(request);
        return ResponseEntity.ok(ApiResponse.exito(response.getMensaje(), response));
    }

    @PostMapping("/catalogo/sincronizar")
    @Operation(summary = "Forzar sincronización del catálogo local con getProductList.do",
            description = "Ignora la antigüedad mínima entre sincronizaciones, pero nunca excede el cupo de 3 consultas " +
                    "diarias del proveedor: si ya se agotó responde 400.")
    public ResponseEntity<ApiResponse<Integer>> sincronizarCatalogo() {
        int activos = catalogoSyncService.sincronizar(true);
        return ResponseEntity.ok(ApiResponse.exito("Catálogo sincronizado", activos));
    }
}
