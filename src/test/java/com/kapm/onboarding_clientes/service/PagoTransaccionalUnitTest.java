package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedMensaje;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.entity.Cuenta;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.Saldo;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import com.kapm.onboarding_clientes.repository.SaldoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoTransaccionalServiceImpl (unitarias): reservas, liquidación, reversión y conciliación")
class PagoTransaccionalUnitTest {

    private static final String CUENTA = "1234567890";
    private static final BigDecimal CIEN = new BigDecimal("100.00");

    @Mock SaldoRepository saldoRepository;
    @Mock PagoServicioRepository pagoServicioRepository;
    @Mock UpcGenerator upcGenerator;
    @InjectMocks PagoTransaccionalServiceImpl servicio;

    private PagoServicio reservar() {
        return servicio.reservarSaldoYCrearPago(CUENTA, "C", "R", 1, 2, "5512345678", null, CIEN, "K", "20260101000000", "UNI");
    }

    private void conSaldo(Saldo saldo) {
        when(saldoRepository.findByCuentaNumeroCuentaWithLock(CUENTA)).thenReturn(Optional.ofNullable(saldo));
    }

    private void conPago(PagoServicio pago) {
        when(pagoServicioRepository.findById(1L)).thenReturn(Optional.ofNullable(pago));
    }

    private static PagoServicio pago(EstatusPago estatus) {
        return PagoServicio.builder().id(1L).upc("UPC").estatus(estatus).build();
    }

    @Test
    @DisplayName("Reserva: cuenta inexistente, saldo nulo o insuficiente")
    void reservaInvalida() {
        conSaldo(null);
        assertThatThrownBy(this::reservar).isInstanceOf(RecursoNoEncontradoException.class);
        conSaldo(Saldo.builder().build());
        assertThatThrownBy(this::reservar).isInstanceOf(ReglaNegocioException.class);
        conSaldo(Saldo.builder().saldoDisponible(BigDecimal.ONE).build());
        assertThatThrownBy(this::reservar).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Reserva: reintenta UPC repetidos y aborta tras 10 intentos")
    void upcRepetido() {
        conSaldo(Saldo.builder().saldoDisponible(new BigDecimal("500")).build());
        when(upcGenerator.generar()).thenReturn("UPC1");
        when(pagoServicioRepository.existsByUpc("UPC1")).thenReturn(true);
        assertThatThrownBy(this::reservar).isInstanceOf(PuntoRedIntegracionException.class).hasMessageContaining("10 intentos");
    }

    @Test
    @DisplayName("Reserva: rechaza un UPC de más de 15 caracteres")
    void upcLargo() {
        conSaldo(Saldo.builder().saldoDisponible(new BigDecimal("500")).build());
        when(upcGenerator.generar()).thenReturn("U".repeat(16));
        assertThatThrownBy(this::reservar).isInstanceOf(PuntoRedIntegracionException.class).hasMessageContaining("15 caracteres");
    }

    @Test
    @DisplayName("Reserva exitosa descuenta el disponible y registra el pago PENDIENTE")
    void reservaExitosa() {
        Saldo saldo = Saldo.builder().saldoDisponible(new BigDecimal("500.00")).build();
        conSaldo(saldo);
        when(upcGenerator.generar()).thenReturn("UPC1", "UPC2");
        when(pagoServicioRepository.existsByUpc("UPC1")).thenReturn(true);
        when(pagoServicioRepository.save(any(PagoServicio.class))).then(returnsFirstArg());

        PagoServicio pago = reservar();

        assertThat(pago.getUpc()).isEqualTo("UPC2");
        assertThat(pago.getEstatus()).isEqualTo(EstatusPago.PENDIENTE);
        assertThat(saldo.getSaldoDisponible()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Éxito: sin cuenta asociada, sin ID_TX ni MENSAJE usa valores por defecto")
    void exitoSinDatosOpcionales() {
        Saldo saldo = Saldo.builder().saldoContable(new BigDecimal("500")).build();
        conSaldo(saldo);
        conPago(pago(EstatusPago.EN_CONCILIACION));
        when(pagoServicioRepository.save(any(PagoServicio.class))).then(returnsFirstArg());

        PagoServicio resultado = servicio.aplicarExito(1L, CUENTA, CIEN, PuntoRedTxResponse.builder().numAutorizacion("A").build());

        assertThat(resultado.getEstatus()).isEqualTo(EstatusPago.EXITOSO);
        assertThat(resultado.getCodigoRespuesta()).isEqualTo("01");
        assertThat(resultado.getTextoRespuesta()).isEqualTo("APROBADA");
        assertThat(resultado.getIdTxProveedor()).isNull();
        assertThat(saldo.getSaldoContable()).isEqualByComparingTo("400");
    }

    @Test
    @DisplayName("Éxito: con cuenta, ID_TX y MENSAJE del proveedor")
    void exitoCompleto() {
        Cuenta cuenta = Cuenta.builder().saldo(new BigDecimal("500")).build();
        conSaldo(Saldo.builder().saldoContable(new BigDecimal("500")).cuenta(cuenta).build());
        conPago(pago(EstatusPago.PENDIENTE));
        when(pagoServicioRepository.save(any(PagoServicio.class))).then(returnsFirstArg());

        PuntoRedTxResponse tx = PuntoRedTxResponse.builder().idTx("TX").mensaje(new PuntoRedMensaje("01", "OK")).build();
        PagoServicio resultado = servicio.aplicarExito(1L, CUENTA, CIEN, tx);

        assertThat(resultado.getIdTxProveedor()).isEqualTo("TX");
        assertThat(resultado.getTextoRespuesta()).isEqualTo("OK");
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("400");
    }

    @Test
    @DisplayName("Liquidar o revertir falla con cuenta/pago inexistente o pago ya cerrado")
    void liquidacionInvalida() {
        conSaldo(null);
        assertThatThrownBy(() -> servicio.aplicarExito(1L, CUENTA, CIEN, new PuntoRedTxResponse())).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.aplicarRechazo(1L, CUENTA, CIEN, "06", "t")).isInstanceOf(RecursoNoEncontradoException.class);

        conSaldo(Saldo.builder().saldoDisponible(BigDecimal.ZERO).build());
        conPago(null);
        assertThatThrownBy(() -> servicio.aplicarRechazo(1L, CUENTA, CIEN, "06", "t")).isInstanceOf(RecursoNoEncontradoException.class);
        conPago(pago(EstatusPago.EXITOSO));
        assertThatThrownBy(() -> servicio.aplicarRechazo(1L, CUENTA, CIEN, "06", "t")).isInstanceOf(ReglaNegocioException.class);
        verify(saldoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechazo devuelve el disponible reservado")
    void rechazo() {
        Saldo saldo = Saldo.builder().saldoDisponible(new BigDecimal("400")).build();
        conSaldo(saldo);
        conPago(pago(EstatusPago.PENDIENTE));
        when(pagoServicioRepository.save(any(PagoServicio.class))).then(returnsFirstArg());

        assertThat(servicio.aplicarRechazo(1L, CUENTA, CIEN, "06", "Rechazada").getEstatus()).isEqualTo(EstatusPago.RECHAZADO);
        assertThat(saldo.getSaldoDisponible()).isEqualByComparingTo("500");
    }

    @Test
    @DisplayName("Timeout trunca el motivo a 250 caracteres y conserva el ID_TX informado")
    void timeout() {
        PagoServicio pago = pago(EstatusPago.PENDIENTE);
        conPago(pago);
        when(pagoServicioRepository.save(any(PagoServicio.class))).then(returnsFirstArg());

        assertThat(servicio.registrarTimeout(1L, "x".repeat(300), " TX1 ").getTextoRespuesta()).hasSize(250);
        assertThat(pago.getIdTxProveedor()).isEqualTo("TX1");
        assertThat(servicio.registrarTimeout(1L, null, " ").getTextoRespuesta()).isNull();
        assertThat(servicio.registrarTimeout(1L, "corto", null).getTextoRespuesta()).isEqualTo("corto");
        assertThat(pago.getIdTxProveedor()).isEqualTo("TX1");

        conPago(null);
        assertThatThrownBy(() -> servicio.registrarTimeout(1L, "m", null)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Intentos de verificación parten de cero si no hay registro previo")
    void intentosVerificacion() {
        PagoServicio pago = pago(EstatusPago.PENDIENTE);
        conPago(pago);
        assertThat(servicio.registrarIntentoVerificacion(1L)).isEqualTo(1);
        assertThat(servicio.registrarIntentoVerificacion(1L)).isEqualTo(2);
        conPago(null);
        assertThatThrownBy(() -> servicio.registrarIntentoVerificacion(1L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Solo un pago PENDIENTE se escala a conciliación")
    void conciliacion() {
        PagoServicio cerrado = pago(EstatusPago.EXITOSO);
        conPago(cerrado);
        servicio.marcarEnConciliacion(1L);
        assertThat(cerrado.getEstatus()).isEqualTo(EstatusPago.EXITOSO);

        PagoServicio pendiente = pago(EstatusPago.PENDIENTE);
        conPago(pendiente);
        servicio.marcarEnConciliacion(1L);
        assertThat(pendiente.getEstatus()).isEqualTo(EstatusPago.EN_CONCILIACION);

        conPago(null);
        assertThatThrownBy(() -> servicio.marcarEnConciliacion(1L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(pagoServicioRepository, never()).existsByUpc(anyString());
    }
}
