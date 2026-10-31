package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.entity.ConsultaProveedorDiaria;
import com.kapm.onboarding_clientes.repository.ConsultaProveedorDiariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuotaConsultaProveedorServiceImpl implements CuotaConsultaProveedorService {

    private final ConsultaProveedorDiariaRepository repository;
    private final PuntoRedProperties properties;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reservarConsulta(String operacion, int limiteDiario) {
        LocalDate hoy = LocalDate.now(zonaProveedor());
        // Si dos instancias crean la fila del día a la vez, el índice único deja pasar solo una;
        // la otra falla con DataIntegrityViolationException y el llamador omite la consulta
        ConsultaProveedorDiaria registro = repository.findByOperacionAndFechaWithLock(operacion, hoy)
                .orElseGet(() -> ConsultaProveedorDiaria.builder().operacion(operacion).fecha(hoy).consultas(0).build());

        if (registro.getConsultas() >= limiteDiario) {
            log.warn("Cupo diario agotado para {} [{}/{}]", operacion, registro.getConsultas(), limiteDiario);
            return false;
        }
        registro.setConsultas(registro.getConsultas() + 1);
        repository.saveAndFlush(registro);
        log.info("Cupo de {} consumido [{}/{}]", operacion, registro.getConsultas(), limiteDiario);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExito(String operacion) {
        LocalDate hoy = LocalDate.now(zonaProveedor());
        repository.findByOperacionAndFechaWithLock(operacion, hoy).ifPresent(registro -> {
            registro.setUltimaExitosa(LocalDateTime.now());
            repository.save(registro);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> ultimaConsultaExitosa(String operacion) {
        return repository.findTopByOperacionAndUltimaExitosaIsNotNullOrderByUltimaExitosaDesc(operacion)
                .map(ConsultaProveedorDiaria::getUltimaExitosa);
    }

    private ZoneId zonaProveedor() {
        try {
            return ZoneId.of(properties.getZonaHoraria());
        } catch (Exception e) {
            return ZoneId.of("America/Mexico_City");
        }
    }
}
