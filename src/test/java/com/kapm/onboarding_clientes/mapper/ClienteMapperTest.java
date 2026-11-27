package com.kapm.onboarding_clientes.mapper;

import com.kapm.onboarding_clientes.dto.request.BuscarClienteRequest;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
import com.kapm.onboarding_clientes.entity.*;
import com.kapm.onboarding_clientes.entity.catalogo.*;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import com.kapm.onboarding_clientes.repository.specification.ClienteSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("ClienteMapper y ClienteSpecification")
class ClienteMapperTest {

    private final DatosSeguridadBiometriaRepository repo = mock(DatosSeguridadBiometriaRepository.class);
    private final ClienteMapper mapper = new ClienteMapper(repo, 5);

    private static Cliente clienteCompleto(DatosSeguridadBiometria seguridad) {
        return Cliente.builder()
                .nombre("Juan").segundoNombre("Carlos").apellidoPaterno("Pérez").apellidoMaterno("Gómez")
                .fechaNacimiento(LocalDate.now().minusYears(30))
                .curp("CURP").rfc("RFC")
                .sexo(CatSexo.builder().codigo("MASCULINO").build())
                .nacionalidad(CatNacionalidad.builder().descripcion("Mexicana").build())
                .estadoCivil(CatEstadoCivil.builder().codigo("SOLTERO").build())
                .ocupacion(CatOcupacion.builder().descripcion("Estudiante").build())
                .ingresoMensual(new BigDecimal("100.5"))
                .domicilio(Domicilio.builder().calle("C")
                        .estado(CatEstadoRepublica.builder().descripcion("Jalisco").build())
                        .pais(CatPais.builder().descripcion("México").build()).build())
                .cuenta(Cuenta.builder().numeroCuenta("1234567890").saldo(new BigDecimal("10")).build())
                .datosSeguridad(seguridad)
                .activo(true)
                .build();
    }

    private static DatosSeguridadBiometria seguridad(Boolean loggeado, LocalDateTime actividad, String hash) {
        return DatosSeguridadBiometria.builder().username("u").loggeado(loggeado).ultimaActividad(actividad).hashBiometrico(hash).build();
    }

    @Test
    @DisplayName("Inactividad no positiva usa el límite predeterminado")
    void limitePredeterminado() {
        ClienteMapper conCero = new ClienteMapper(repo, 0);
        Cliente cliente = clienteCompleto(seguridad(true, LocalDateTime.now().minusSeconds(3), "h"));
        assertThat(conCero.mapearAClienteResponse(cliente).getLoggeado()).isTrue();
    }

    @Test
    @DisplayName("Cliente completo con sesión activa")
    void clienteCompletoSesionActiva() {
        ClienteResponse r = mapper.mapearAClienteResponse(clienteCompleto(seguridad(true, LocalDateTime.now(), "hash")));
        assertThat(r.getNombreCompleto()).isEqualTo("Juan Carlos Pérez Gómez");
        assertThat(r.getEdad()).isEqualTo(30);
        assertThat(r.getSexo()).isEqualTo("MASCULINO");
        assertThat(r.getNacionalidad()).isEqualTo("Mexicana");
        assertThat(r.getEstadoCivil()).isEqualTo("SOLTERO");
        assertThat(r.getDomicilio().getEstado()).isEqualTo("Jalisco");
        assertThat(r.getDomicilio().getPais()).isEqualTo("México");
        assertThat(r.getLaboral().getIngresoMensual()).isEqualByComparingTo("100.50");
        assertThat(r.getCuenta().getClienteCurp()).isEqualTo("CURP");
        assertThat(r.getBiometriaRegistrada()).isTrue();
        assertThat(r.getLoggeado()).isTrue();
        verify(repo).save(any());
    }

    @Test
    @DisplayName("Sesión expirada, no loggeada o sin actividad registrada")
    void variantesSesion() {
        assertThat(mapper.mapearAClienteResponse(clienteCompleto(seguridad(true, LocalDateTime.now().minusMinutes(1), " "))).getLoggeado()).isFalse();
        assertThat(mapper.mapearAClienteResponse(clienteCompleto(seguridad(false, LocalDateTime.now(), null))).getBiometriaRegistrada()).isFalse();
        assertThat(mapper.mapearAClienteResponse(clienteCompleto(seguridad(true, null, "h"))).getLoggeado()).isTrue();
        verify(repo, times(2)).save(any());
    }

    @Test
    @DisplayName("Cliente mínimo: sin catálogos, domicilio, cuenta ni seguridad")
    void clienteMinimo() {
        Cliente minimo = Cliente.builder().nombre("A").segundoNombre(" ").apellidoPaterno("B").apellidoMaterno("C").build();
        ClienteResponse r = mapper.mapearAClienteResponse(minimo);
        assertThat(r.getEdad()).isZero();
        assertThat(r.getNombreCompleto()).isEqualTo("A B C");
        assertThat(r.getSexo()).isNull();
        assertThat(r.getNacionalidad()).isNull();
        assertThat(r.getEstadoCivil()).isNull();
        assertThat(r.getDomicilio()).isNull();
        assertThat(r.getCuenta()).isNull();
        assertThat(r.getLaboral().getOcupacion()).isNull();
        assertThat(r.getLaboral().getIngresoMensual()).isNull();
        assertThat(r.getLoggeado()).isFalse();

        Cliente sinSegundoNombre = Cliente.builder().nombre("A").apellidoPaterno("B").apellidoMaterno("C").build();
        assertThat(mapper.mapearAClienteResponse(sinSegundoNombre).getNombreCompleto()).isEqualTo("A B C");

        assertThat(mapper.mapearAClienteResponse(null)).isNull();
        assertThat(mapper.mapearADatosLaboralesDTO(null)).isNull();
        assertThat(mapper.mapearADomicilioDTO(Domicilio.builder().build()).getEstado()).isNull();
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Cuenta con y sin titular")
    void cuentas() {
        Cuenta sinSaldo = Cuenta.builder().numeroCuenta("1").build();
        assertThat(mapper.mapearACuentaResponseConCliente(null, null)).isNull();
        CuentaResponse sinCliente = mapper.mapearACuentaResponseConCliente(sinSaldo, null);
        assertThat(sinCliente.getClienteNombreCompleto()).isEqualTo("No asignado");
        assertThat(sinCliente.getClienteCurp()).isEqualTo("N/A");
        assertThat(sinCliente.getSaldo()).isNull();

        assertThat(mapper.mapearACuentaResponse(null, Optional.empty())).isNull();
        assertThat(mapper.mapearACuentaResponse(sinSaldo, Optional.empty()).getClienteCurp()).isEqualTo("N/A");
        Cuenta conSaldo = Cuenta.builder().numeroCuenta("1").saldo(BigDecimal.ONE).build();
        assertThat(mapper.mapearACuentaResponse(conSaldo, Optional.of(clienteCompleto(null))).getSaldo()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("Saldo con todas las variantes de sesión del titular")
    void saldos() {
        Cuenta cuenta = Cuenta.builder().numeroCuenta("1").build();
        Saldo vacio = Saldo.builder().build();
        Saldo lleno = Saldo.builder().saldoDisponible(BigDecimal.ONE).saldoContable(BigDecimal.TEN).build();

        SaldoResponse sinTitular = mapper.mapearASaldoResponse(cuenta, Optional.empty(), vacio);
        assertThat(sinTitular.getTitularCurp()).isEqualTo("N/A");
        assertThat(sinTitular.getSaldoDisponible()).isNull();
        assertThat(sinTitular.getSaldoContable()).isNull();

        assertThat(mapper.mapearASaldoResponse(cuenta, Optional.of(clienteCompleto(null)), lleno).getSaldoContable()).isEqualByComparingTo("10.00");
        assertThat(mapper.mapearASaldoResponse(cuenta, Optional.of(clienteCompleto(seguridad(true, LocalDateTime.now(), "h"))), lleno).getUsuarioLoggeado()).isTrue();
        assertThat(mapper.mapearASaldoResponse(cuenta, Optional.of(clienteCompleto(seguridad(true, LocalDateTime.now().minusMinutes(1), "h"))), lleno).getUsuarioLoggeado()).isFalse();
        assertThat(mapper.mapearASaldoResponse(cuenta, Optional.of(clienteCompleto(seguridad(false, LocalDateTime.now(), "h"))), lleno).getUsuarioLoggeado()).isFalse();
        assertThat(mapper.mapearASaldoResponse(cuenta, Optional.of(clienteCompleto(seguridad(true, null, "h"))), lleno).getUsuarioLoggeado()).isTrue();
    }

    @Test
    @DisplayName("La especificación agrega un predicado por cada filtro informado")
    @SuppressWarnings("unchecked")
    void especificacion() {
        Root<Cliente> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

        BuscarClienteRequest todos = BuscarClienteRequest.builder()
                .curp("c").rfc("r").correo("E").numeroCuenta("1").nombre("n").apellidoPaterno("p").apellidoMaterno("m")
                .activo(true).fechaInicio(LocalDate.now()).fechaFin(LocalDate.now()).build();
        BuscarClienteRequest blancos = BuscarClienteRequest.builder()
                .curp(" ").rfc(" ").correo(" ").numeroCuenta(" ").nombre(" ").apellidoPaterno(" ").apellidoMaterno(" ").build();

        ClienteSpecification.conFiltros(todos).toPredicate(root, query, cb);
        ClienteSpecification.conFiltros(blancos).toPredicate(root, query, cb);
        ClienteSpecification.conFiltros(new BuscarClienteRequest()).toPredicate(root, query, cb);

        verify(cb, times(3)).like(any(), any(String.class));
        verify(cb).greaterThanOrEqualTo(any(), any(LocalDateTime.class));
        verify(cb).lessThanOrEqualTo(any(), any(LocalDateTime.class));
    }
}
