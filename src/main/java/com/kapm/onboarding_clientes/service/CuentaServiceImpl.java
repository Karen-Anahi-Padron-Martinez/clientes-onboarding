package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
import com.kapm.onboarding_clientes.exception.CuentaNoEncontradaException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.model.Cliente;
import com.kapm.onboarding_clientes.model.Cuenta;
import com.kapm.onboarding_clientes.model.EstatusCuenta;
import com.kapm.onboarding_clientes.model.Saldo;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.CuentaRepository;
import com.kapm.onboarding_clientes.repository.SaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final SaldoRepository saldoRepository;
    private static final BigDecimal SALDO_INICIAL_DEFAULT = new BigDecimal("1000.00");
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public Cuenta crearCuentaInicial(BigDecimal saldoInicial) {
        BigDecimal saldo = (saldoInicial != null) ? saldoInicial : SALDO_INICIAL_DEFAULT;

        if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El saldo inicial de la cuenta no puede ser negativo");
        }

        String numeroCuenta = generarNumeroCuentaUnico();

        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(numeroCuenta)
                .saldo(saldo)
                .estatus(EstatusCuenta.ACTIVA)
                .build();

        Saldo saldoRegistro = Saldo.builder()
                .cuenta(cuenta)
                .saldoDisponible(saldo)
                .saldoContable(saldo)
                .fechaUltimaActualizacion(LocalDateTime.now())
                .build();

        cuenta.setSaldoDetalle(saldoRegistro);

        return cuentaRepository.save(cuenta);
    }

    @Override
    public String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            long numero = 1000000000L + (long) (random.nextDouble() * 8999999999L);
            numeroCuenta = String.valueOf(numero);
        } while (cuentaRepository.existsByNumeroCuenta(numeroCuenta));

        return numeroCuenta;
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaResponse obtenerCuentaPorNumero(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró la cuenta con el número: " + numeroCuenta));

        return mapearACuentaResponse(cuenta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> obtenerCuentasActivas() {
        return cuentaRepository.findByEstatus(EstatusCuenta.ACTIVA).stream()
                .map(this::mapearACuentaResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SaldoResponse obtenerSaldoCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró la cuenta con el número: " + numeroCuenta));

        Optional<Cliente> clienteOpt = clienteRepository.findByCuentaNumeroCuenta(numeroCuenta);

        String titularNombre = clienteOpt.map(c -> c.getNombre() + " " + c.getApellidoPaterno() + " " + c.getApellidoMaterno()).orElse("No asignado");
        String titularCurp = clienteOpt.map(Cliente::getCurp).orElse("N/A");

        boolean loggeado = false;
        long segundosInactividad = 0;

        if (clienteOpt.isPresent() && clienteOpt.get().getDatosSeguridad() != null) {
            var seguridad = clienteOpt.get().getDatosSeguridad();
            if (seguridad.getUltimaActividad() != null) {
                segundosInactividad = Duration.between(seguridad.getUltimaActividad(), LocalDateTime.now()).getSeconds();
                // 5-second inactivity check
                if (Boolean.TRUE.equals(seguridad.getLoggeado()) && segundosInactividad <= 5) {
                    loggeado = true;
                    seguridad.setUltimaActividad(LocalDateTime.now());
                } else {
                    seguridad.setLoggeado(false);
                    loggeado = false;
                }
            } else {
                loggeado = Boolean.TRUE.equals(seguridad.getLoggeado());
            }
        }

        Saldo saldoDetalle = saldoRepository.findByCuentaNumeroCuenta(numeroCuenta)
                .orElseGet(() -> Saldo.builder()
                        .cuenta(cuenta)
                        .saldoDisponible(cuenta.getSaldo())
                        .saldoContable(cuenta.getSaldo())
                        .fechaUltimaActualizacion(LocalDateTime.now())
                        .build());

        return SaldoResponse.builder()
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldoDisponible(saldoDetalle.getSaldoDisponible())
                .saldoContable(saldoDetalle.getSaldoContable())
                .estatus(cuenta.getEstatus())
                .titularNombreCompleto(titularNombre)
                .titularCurp(titularCurp)
                .usuarioLoggeado(loggeado)
                .segundosInactividad(segundosInactividad)
                .fechaConsulta(LocalDateTime.now())
                .build();
    }

    private CuentaResponse mapearACuentaResponse(Cuenta cuenta) {
        Optional<Cliente> clienteOpt = clienteRepository.findByCuentaNumeroCuenta(cuenta.getNumeroCuenta());

        String titularNombre = clienteOpt.map(c -> c.getNombre() + " " + c.getApellidoPaterno() + " " + c.getApellidoMaterno()).orElse("Sin Titular");
        String titularCurp = clienteOpt.map(Cliente::getCurp).orElse("N/A");

        return CuentaResponse.builder()
                .id(cuenta.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo())
                .estatus(cuenta.getEstatus())
                .fechaCreacion(cuenta.getFechaCreacion())
                .clienteNombreCompleto(titularNombre)
                .clienteCurp(titularCurp)
                .build();
    }
}
