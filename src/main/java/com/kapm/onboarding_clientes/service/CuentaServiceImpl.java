package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.Cuenta;
import com.kapm.onboarding_clientes.entity.EstatusCuenta;
import com.kapm.onboarding_clientes.entity.Saldo;
import com.kapm.onboarding_clientes.exception.CuentaNoEncontradaException;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.mapper.ClienteMapper;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.CuentaRepository;
import com.kapm.onboarding_clientes.repository.SaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
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
    private final ClienteMapper clienteMapper;
    private static final BigDecimal SALDO_INICIAL_DEFAULT = new BigDecimal("1000.00");
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public Cuenta crearCuentaInicial(BigDecimal saldoInicial) {
        BigDecimal saldo = (saldoInicial != null) ? saldoInicial : SALDO_INICIAL_DEFAULT;

        if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException(ErrorMessage.SALDO_INICIAL_NEGATIVO);
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
                .orElseThrow(() -> new CuentaNoEncontradaException(ErrorMessage.CUENTA_NO_ENCONTRADA, numeroCuenta));

        Optional<Cliente> clienteOpt = clienteRepository.findByCuentaNumeroCuenta(numeroCuenta);
        return clienteMapper.mapearACuentaResponse(cuenta, clienteOpt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> obtenerCuentasActivas() {
        return cuentaRepository.findByEstatus(EstatusCuenta.ACTIVA).stream()
                .map(cuenta -> {
                    Optional<Cliente> clienteOpt = clienteRepository.findByCuentaNumeroCuenta(cuenta.getNumeroCuenta());
                    return clienteMapper.mapearACuentaResponse(cuenta, clienteOpt);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SaldoResponse obtenerSaldoCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(ErrorMessage.CUENTA_NO_ENCONTRADA, numeroCuenta));

        Optional<Cliente> clienteOpt = clienteRepository.findByCuentaNumeroCuenta(numeroCuenta);

        Saldo saldoDetalle = saldoRepository.findByCuentaNumeroCuenta(numeroCuenta)
                .orElseGet(() -> Saldo.builder()
                        .cuenta(cuenta)
                        .saldoDisponible(cuenta.getSaldo())
                        .saldoContable(cuenta.getSaldo())
                        .fechaUltimaActualizacion(LocalDateTime.now())
                        .build());

        return clienteMapper.mapearASaldoResponse(cuenta, clienteOpt, saldoDetalle);
    }
}
