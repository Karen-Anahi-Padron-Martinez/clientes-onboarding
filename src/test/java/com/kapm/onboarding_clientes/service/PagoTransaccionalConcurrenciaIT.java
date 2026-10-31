package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedMensaje;
import com.kapm.onboarding_clientes.client.puntored.model.PuntoRedTxResponse;
import com.kapm.onboarding_clientes.entity.Cuenta;
import com.kapm.onboarding_clientes.entity.EstatusCuenta;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.Saldo;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CuentaRepository;
import com.kapm.onboarding_clientes.repository.PagoServicioRepository;
import com.kapm.onboarding_clientes.repository.SaldoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración contra BD real (H2) de la reserva y liquidación de saldo con bloqueo pesimista.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:concurrencia;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;LOCK_TIMEOUT=10000")
@ActiveProfiles("test")
@DisplayName("Integración - Concurrencia y consistencia de saldo en pagos")
class PagoTransaccionalConcurrenciaIT {

    private static final String CUENTA = "9900000001";

    @Autowired
    private PagoTransaccionalService pagoTransaccionalService;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private SaldoRepository saldoRepository;
    @Autowired
    private PagoServicioRepository pagoServicioRepository;

    @BeforeEach
    void crearCuentaConSaldo() {
        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(CUENTA)
                .saldo(new BigDecimal("500.00"))
                .estatus(EstatusCuenta.ACTIVA)
                .fechaCreacion(LocalDateTime.now())
                .build();
        Saldo saldo = Saldo.builder()
                .cuenta(cuenta)
                .saldoDisponible(new BigDecimal("500.00"))
                .saldoContable(new BigDecimal("500.00"))
                .build();
        cuenta.setSaldoDetalle(saldo);
        cuentaRepository.save(cuenta);
    }

    @AfterEach
    void limpiar() {
        pagoServicioRepository.deleteAll();
        cuentaRepository.deleteAll();
    }

    @Test
    @DisplayName("10 reservas simultáneas de $100 sobre $500: exactamente 5 pasan y el saldo nunca queda negativo")
    void reservasConcurrentes_NoSobregiranLaCuenta() throws Exception {
        List<Future<Boolean>> resultados = ejecutarEnParalelo(10, i -> () -> {
            try {
                reservar("KEY-CONC-" + i, "100.00");
                return true;
            } catch (ReglaNegocioException e) {
                return false;
            }
        });

        long exitosas = contarExitosas(resultados);
        Saldo saldo = saldoRepository.findByCuentaNumeroCuenta(CUENTA).orElseThrow();

        assertThat(exitosas).isEqualTo(5);
        assertThat(saldo.getSaldoDisponible()).isEqualByComparingTo("0.00");
        assertThat(pagoServicioRepository.findByNumeroCuenta(CUENTA)).hasSize(5);
    }

    @Test
    @DisplayName("Misma idempotencyKey enviada 5 veces en paralelo: un solo pago y un solo débito")
    void mismaLlaveEnParalelo_UnSoloCargo() throws Exception {
        List<Future<Boolean>> resultados = ejecutarEnParalelo(5, i -> () -> {
            try {
                reservar("KEY-DUPLICADA", "100.00");
                return true;
            } catch (DataIntegrityViolationException e) {
                return false;
            }
        });

        Saldo saldo = saldoRepository.findByCuentaNumeroCuenta(CUENTA).orElseThrow();

        assertThat(contarExitosas(resultados)).isEqualTo(1);
        assertThat(saldo.getSaldoDisponible()).isEqualByComparingTo("400.00");
        assertThat(pagoServicioRepository.findByNumeroCuenta(CUENTA)).hasSize(1);
    }

    @Test
    @DisplayName("Un pago ya liquidado no puede liquidarse ni revertirse otra vez (sin doble débito ni doble devolución)")
    void pagoCerrado_NoAdmiteSegundaLiquidacion() {
        PagoServicio pago = reservar("KEY-CIERRE", "100.00");
        PuntoRedTxResponse aprobada = PuntoRedTxResponse.builder()
                .numAutorizacion("AUT-1").mensaje(new PuntoRedMensaje("01", "APROBADA")).build();

        pagoTransaccionalService.aplicarExito(pago.getId(), CUENTA, pago.getMonto(), aprobada);

        assertThatThrownBy(() -> pagoTransaccionalService.aplicarExito(pago.getId(), CUENTA, pago.getMonto(), aprobada))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> pagoTransaccionalService.aplicarRechazo(pago.getId(), CUENTA, pago.getMonto(), "99", "X"))
                .isInstanceOf(ReglaNegocioException.class);

        Saldo saldo = saldoRepository.findByCuentaNumeroCuenta(CUENTA).orElseThrow();
        assertThat(saldo.getSaldoDisponible()).isEqualByComparingTo("400.00");
        assertThat(saldo.getSaldoContable()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Conciliación: un pago EN_CONCILIACION conserva la reserva y puede liberarse una sola vez")
    void pagoEnConciliacion_SeLiberaUnaSolaVez() {
        PagoServicio pago = reservar("KEY-CONCILIA", "100.00");
        pagoTransaccionalService.registrarTimeout(pago.getId(), "TIMEOUT_PROVEEDOR", null);
        pagoTransaccionalService.marcarEnConciliacion(pago.getId());

        assertThat(pagoServicioRepository.findById(pago.getId()).orElseThrow().getEstatus()).isEqualTo(EstatusPago.EN_CONCILIACION);
        assertThat(saldoRepository.findByCuentaNumeroCuenta(CUENTA).orElseThrow().getSaldoDisponible()).isEqualByComparingTo("400.00");

        pagoTransaccionalService.aplicarRechazo(pago.getId(), CUENTA, pago.getMonto(), "CONCILIACION", "NO_PROCESADA");

        assertThatThrownBy(() -> pagoTransaccionalService.aplicarRechazo(pago.getId(), CUENTA, pago.getMonto(), "CONCILIACION", "NO_PROCESADA"))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(saldoRepository.findByCuentaNumeroCuenta(CUENTA).orElseThrow().getSaldoDisponible()).isEqualByComparingTo("500.00");
    }

    private PagoServicio reservar(String idempotencyKey, String monto) {
        return pagoTransaccionalService.reservarSaldoYCrearPago(
                CUENTA, "MEHC920824HDFRMN09", null, 55, 2, "5512345678", null,
                new BigDecimal(monto), idempotencyKey, "20261028120000", "SUCURSAL_TEST");
    }

    private List<Future<Boolean>> ejecutarEnParalelo(int hilos, java.util.function.IntFunction<Callable<Boolean>> tarea) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        try {
            for (int i = 0; i < hilos; i++) {
                Callable<Boolean> c = tarea.apply(i);
                resultados.add(executor.submit(() -> {
                    salida.await();
                    return c.call();
                }));
            }
            salida.countDown();
            for (Future<Boolean> f : resultados) {
                f.get();
            }
        } finally {
            executor.shutdown();
        }
        return resultados;
    }

    private long contarExitosas(List<Future<Boolean>> resultados) throws Exception {
        long n = 0;
        for (Future<Boolean> f : resultados) {
            if (f.get()) {
                n++;
            }
        }
        return n;
    }
}
