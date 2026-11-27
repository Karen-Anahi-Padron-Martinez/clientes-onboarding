package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.HistorialPagosRequest;
import com.kapm.onboarding_clientes.dto.request.ValidarReferenciaRequest;
import com.kapm.onboarding_clientes.dto.response.PageResponse;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.dto.response.ProductoPagoResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionReferenciaResponse;

import java.util.List;

/**
 * Consultas de solo lectura del cliente autenticado: catálogo vendible, validación previa de referencia,
 * historial y detalle de sus propias operaciones.
 */
public interface PagoConsultaService {

    List<ProductoPagoResponse> listarCatalogo();

    ValidacionReferenciaResponse validarReferencia(ValidarReferenciaRequest request);

    /**
     * @param username subject del JWT; el historial se filtra por el titular del token
     */
    PageResponse<PagoResponse> historial(HistorialPagosRequest request, String username);

    /**
     * @param username subject del JWT; un folio de otro titular responde igual que uno inexistente (404)
     */
    PagoResponse detalle(String upc, String username);
}
