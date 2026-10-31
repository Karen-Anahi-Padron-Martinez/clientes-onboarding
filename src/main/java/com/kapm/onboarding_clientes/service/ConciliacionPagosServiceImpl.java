package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedMensaje;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.dto.request.ResolverConciliacionRequest;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConciliacionPagosServiceImpl implements ConciliacionPagosService {

    private final PagoServicioRepository pagoServicioRepository;
    private final PagoTransaccionalService pagoTransaccionalService;

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> listarEnConciliacion() {
        return pagoServicioRepository.findByEstatus(EstatusPago.EN_CONCILIACION).stream()
                .map(p -> PagoResponse.desde(p, "Pendiente de conciliación con el proveedor"))
                .toList();
    }

    @Override
    public PagoResponse resolver(ResolverConciliacionRequest request) {
        PagoServicio pago = pagoServicioRepository.findByUpc(request.getUpc().trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el pago a conciliar"));

        // aplicarExito / aplicarRechazo validan bajo lock que el pago siga abierto (PENDIENTE o EN_CONCILIACION)
        PagoServicio resuelto;
        if (Boolean.TRUE.equals(request.getProcesadaPorProveedor())) {
            PuntoRedTxResponse confirmacion = PuntoRedTxResponse.builder()
                    .numAutorizacion(request.getNumAutorizacion().trim())
                    .idTx(request.getIdTxProveedor())
                    .mensaje(new PuntoRedMensaje("01", "CONCILIADO_MANUAL_APROBADA"))
                    .build();
            resuelto = pagoTransaccionalService.aplicarExito(pago.getId(), pago.getNumeroCuenta(), pago.getMonto(), confirmacion);
        } else {
            resuelto = pagoTransaccionalService.aplicarRechazo(pago.getId(), pago.getNumeroCuenta(), pago.getMonto(),
                    "CONCILIACION", "CONCILIADO_MANUAL_NO_PROCESADA");
        }

        log.info("Pago conciliado manualmente [upc: {}, estatus final: {}]", resuelto.getUpc(), resuelto.getEstatus());
        return PagoResponse.desde(resuelto, "Pago conciliado");
    }
}
