package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.Cuenta;
import com.kapm.onboarding_clientes.entity.DatosSeguridadBiometria;
import com.kapm.onboarding_clientes.entity.EstatusCuenta;
import com.kapm.onboarding_clientes.entity.Saldo;
import com.kapm.onboarding_clientes.entity.TipoBiometria;
import com.kapm.onboarding_clientes.exception.CuentaNoEncontradaException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.mapper.ClienteMapper;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.CuentaRepository;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import com.kapm.onboarding_clientes.repository.SaldoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CuentaServiceImpl y SeguridadBiometriaServiceImpl (unitarias)")
class CuentaYSeguridadUnitTest {

    private static final String NUMERO = "1234567890";

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Cuentas {
        @Mock CuentaRepository cuentaRepository;
        @Mock ClienteRepository clienteRepository;
        @Mock SaldoRepository saldoRepository;
        @Mock ClienteMapper clienteMapper;
        @InjectMocks CuentaServiceImpl servicio;

        @Test
        @DisplayName("Sin saldo inicial usa 1000.00; un saldo negativo se rechaza")
        void crearCuentaInicial() {
            when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(true, false);
            when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

            Cuenta cuenta = servicio.crearCuentaInicial(null);

            assertThat(cuenta.getSaldo()).isEqualByComparingTo("1000.00");
            assertThat(cuenta.getSaldoDetalle().getSaldoDisponible()).isEqualByComparingTo("1000.00");
            assertThat(cuenta.getNumeroCuenta()).hasSize(10);
            assertThatThrownBy(() -> servicio.crearCuentaInicial(new BigDecimal("-1")))
                    .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        @DisplayName("Consultas por número de cuenta inexistente responden 404")
        void cuentaInexistente() {
            when(cuentaRepository.findByNumeroCuenta(NUMERO)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.obtenerCuentaPorNumero(NUMERO)).isInstanceOf(CuentaNoEncontradaException.class);
            assertThatThrownBy(() -> servicio.obtenerSaldoCuenta(NUMERO)).isInstanceOf(CuentaNoEncontradaException.class);
        }

        @Test
        @DisplayName("Consulta una cuenta existente por número")
        void cuentaPorNumero() {
            Cuenta cuenta = Cuenta.builder().numeroCuenta(NUMERO).build();
            CuentaResponse respuesta = new CuentaResponse();
            when(cuentaRepository.findByNumeroCuenta(NUMERO)).thenReturn(Optional.of(cuenta));
            when(clienteRepository.findByCuentaNumeroCuenta(NUMERO)).thenReturn(Optional.empty());
            when(clienteMapper.mapearACuentaResponse(cuenta, Optional.empty())).thenReturn(respuesta);

            assertThat(servicio.obtenerCuentaPorNumero(NUMERO)).isSameAs(respuesta);
        }

        @Test
        @DisplayName("Lista las cuentas activas con su cliente")
        void cuentasActivas() {
            Cuenta cuenta = Cuenta.builder().numeroCuenta(NUMERO).saldo(BigDecimal.TEN).estatus(EstatusCuenta.ACTIVA).build();
            Optional<Cliente> cliente = Optional.of(new Cliente());
            CuentaResponse respuesta = new CuentaResponse();
            when(cuentaRepository.findByEstatus(EstatusCuenta.ACTIVA)).thenReturn(List.of(cuenta));
            when(clienteRepository.findByCuentaNumeroCuenta(NUMERO)).thenReturn(cliente);
            when(clienteMapper.mapearACuentaResponse(cuenta, cliente)).thenReturn(respuesta);

            assertThat(servicio.obtenerCuentasActivas()).containsExactly(respuesta);
        }

        @Test
        @DisplayName("El saldo usa el detalle guardado o, si falta, el saldo de la cuenta")
        void saldo() {
            Cuenta cuenta = Cuenta.builder().numeroCuenta(NUMERO).saldo(new BigDecimal("50.00")).build();
            Saldo guardado = Saldo.builder().saldoDisponible(BigDecimal.ONE).build();
            when(cuentaRepository.findByNumeroCuenta(NUMERO)).thenReturn(Optional.of(cuenta));
            when(clienteRepository.findByCuentaNumeroCuenta(NUMERO)).thenReturn(Optional.empty());
            when(saldoRepository.findByCuentaNumeroCuenta(NUMERO)).thenReturn(Optional.of(guardado), Optional.empty());
            SaldoResponse respuesta = new SaldoResponse();
            when(clienteMapper.mapearASaldoResponse(eq(cuenta), eq(Optional.empty()), any(Saldo.class))).thenReturn(respuesta);

            assertThat(servicio.obtenerSaldoCuenta(NUMERO)).isSameAs(respuesta);
            assertThat(servicio.obtenerSaldoCuenta(NUMERO)).isSameAs(respuesta);

            ArgumentCaptor<Saldo> saldos = ArgumentCaptor.forClass(Saldo.class);
            verify(clienteMapper, times(2)).mapearASaldoResponse(eq(cuenta), eq(Optional.empty()), saldos.capture());
            assertThat(saldos.getAllValues().get(0)).isSameAs(guardado);
            assertThat(saldos.getAllValues().get(1).getSaldoDisponible()).isEqualByComparingTo("50.00");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Seguridad {
        @Mock DatosSeguridadBiometriaRepository repo;

        private SeguridadBiometriaServiceImpl servicio() {
            return new SeguridadBiometriaServiceImpl(repo);
        }

        @Test
        @DisplayName("Password y biometría vacías o nulas se rechazan; las validaciones con null son falsas")
        void entradasVacias() {
            SeguridadBiometriaServiceImpl s = servicio();
            assertThatThrownBy(() -> s.encriptarPassword(null)).isInstanceOf(ReglaNegocioException.class);
            assertThatThrownBy(() -> s.encriptarPassword(" ")).isInstanceOf(ReglaNegocioException.class);
            assertThatThrownBy(() -> s.generarHashBiometrico(null)).isInstanceOf(ReglaNegocioException.class);
            assertThatThrownBy(() -> s.generarHashBiometrico(" ")).isInstanceOf(ReglaNegocioException.class);
            assertThat(s.validarPassword(null, "h")).isFalse();
            assertThat(s.validarPassword("p", null)).isFalse();
            assertThat(s.validarBiometria(null, "h")).isFalse();
            assertThat(s.validarBiometria("p", null)).isFalse();
            String hash = s.generarHashBiometrico("plantilla");
            assertThat(s.validarBiometria("plantilla", hash.toUpperCase())).isTrue();
        }

        @Test
        @DisplayName("Sin SHA-256 disponible responde error de hash")
        void sinAlgoritmo() {
            SeguridadBiometriaServiceImpl s = servicio();
            try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
                digest.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(new NoSuchAlgorithmException("x"));
                assertThatThrownBy(() -> s.generarHashBiometrico("plantilla")).isInstanceOf(ReglaNegocioException.class);
            }
        }

        @Test
        @DisplayName("Usuario duplicado se rechaza; sin tipo de biometría usa huella")
        void crearDatos() {
            when(repo.existsByUsername("dup")).thenReturn(true);
            when(repo.existsByUsername("nuevo")).thenReturn(false);
            SeguridadBiometriaServiceImpl s = servicio();
            assertThatThrownBy(() -> s.crearDatosSeguridad("dup", "p", null, "b")).isInstanceOf(ReglaNegocioException.class);
            assertThat(s.crearDatosSeguridad("nuevo", "p", null, "b").getTipoBiometria()).isEqualTo(TipoBiometria.HUELLA_DACTILAR);
            assertThat(s.crearDatosSeguridad("nuevo", "p", TipoBiometria.IRIS, "b").getTipoBiometria()).isEqualTo(TipoBiometria.IRIS);
        }

        private LoginResponse estado(Boolean loggeado, LocalDateTime ultimaActividad) {
            DatosSeguridadBiometria datos = DatosSeguridadBiometria.builder()
                    .username("u").loggeado(loggeado).ultimaActividad(ultimaActividad).build();
            when(repo.findByUsername("u")).thenReturn(Optional.of(datos));
            return servicio().consultarEstadoSesion("u");
        }

        @Test
        @DisplayName("Estado de sesión según actividad e inactividad")
        void estadoSesion() {
            assertThat(estado(true, LocalDateTime.now()).getEstadoSesion()).startsWith("SESIÓN ACTIVA (");
            assertThat(estado(true, LocalDateTime.now().minusMinutes(16)).getEstadoSesion()).startsWith("SESIÓN EXPIRADA");
            assertThat(estado(false, LocalDateTime.now()).isLoggeado()).isFalse();
            assertThat(estado(true, null).getEstadoSesion()).isEqualTo("SESIÓN ACTIVA");
            assertThat(estado(null, null).getEstadoSesion()).isEqualTo("SESIÓN INACTIVA");
        }

        @Test
        @DisplayName("Usuario inexistente responde 404 en consulta y cierre de sesión")
        void usuarioInexistente() {
            when(repo.findByUsername("x")).thenReturn(Optional.empty());
            SeguridadBiometriaServiceImpl s = servicio();
            assertThatThrownBy(() -> s.consultarEstadoSesion("x")).isInstanceOf(RecursoNoEncontradoException.class);
            assertThatThrownBy(() -> s.cerrarSesion("x")).isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("Cerrar sesión invalida el identificador de sesión")
        void cerrarSesion() {
            DatosSeguridadBiometria datos = DatosSeguridadBiometria.builder().username("u").loggeado(true).sesionId("S").build();
            when(repo.findByUsername("u")).thenReturn(Optional.of(datos));
            servicio().cerrarSesion("u");
            assertThat(datos.getLoggeado()).isFalse();
            assertThat(datos.getSesionId()).isNull();
        }
    }
}
