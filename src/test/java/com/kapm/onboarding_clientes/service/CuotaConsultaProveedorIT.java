package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.repository.ConsultaProveedorDiariaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integración contra BD real (H2) del cupo diario de getProductList.do (3 llamadas por día).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:cuota;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;LOCK_TIMEOUT=10000")
@ActiveProfiles("test")
@DisplayName("Integración - Cupo diario de consultas al proveedor")
class CuotaConsultaProveedorIT {

    private static final String OPERACION = "getProductList";

    @Autowired
    private CuotaConsultaProveedorService cuotaService;
    @Autowired
    private ConsultaProveedorDiariaRepository repository;

    @AfterEach
    void limpiar() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Permite exactamente el límite diario y rechaza el siguiente")
    void respetaLimiteDiario() {
        assertThat(cuotaService.reservarConsulta(OPERACION, 3)).isTrue();
        assertThat(cuotaService.reservarConsulta(OPERACION, 3)).isTrue();
        assertThat(cuotaService.reservarConsulta(OPERACION, 3)).isTrue();
        assertThat(cuotaService.reservarConsulta(OPERACION, 3)).isFalse();
    }

    @Test
    @DisplayName("Registra la última consulta exitosa (insumo para no gastar cupo en reinicios)")
    void registraUltimaExitosa() {
        assertThat(cuotaService.ultimaConsultaExitosa(OPERACION)).isEmpty();

        cuotaService.reservarConsulta(OPERACION, 3);
        cuotaService.registrarExito(OPERACION);

        assertThat(cuotaService.ultimaConsultaExitosa(OPERACION)).isPresent();
    }

    @Test
    @DisplayName("10 solicitudes simultáneas: nunca se conceden más de 3")
    void concurrenciaNoExcedeLimite() throws Exception {
        cuotaService.reservarConsulta(OPERACION, 3); // la fila del día ya existe: todas compiten por el mismo lock

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Callable<Boolean> tarea = () -> {
                salida.await();
                return cuotaService.reservarConsulta(OPERACION, 3);
            };
            resultados.add(pool.submit(tarea));
        }
        salida.countDown();

        int concedidas = 0;
        for (Future<Boolean> r : resultados) {
            if (r.get()) {
                concedidas++;
            }
        }
        pool.shutdown();

        assertThat(concedidas).isEqualTo(2);
        assertThat(repository.findAll().get(0).getConsultas()).isEqualTo(3);
    }
}
