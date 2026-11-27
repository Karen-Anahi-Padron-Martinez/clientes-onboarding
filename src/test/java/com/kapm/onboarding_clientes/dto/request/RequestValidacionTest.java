package com.kapm.onboarding_clientes.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validaciones cruzadas (@AssertTrue) y valores seguros de los DTOs de entrada")
class RequestValidacionTest {

    private static void verificarIdentificador(BiFunction<String, String, Boolean> presente) {
        assertThat(presente.apply("CURP", null)).isTrue();
        assertThat(presente.apply(null, "RFC")).isTrue();
        assertThat(presente.apply(" ", "RFC")).isTrue();
        assertThat(presente.apply(null, null)).isFalse();
        assertThat(presente.apply(" ", " ")).isFalse();
    }

    @Test
    @DisplayName("Los DTOs identificados por CURP o RFC exigen al menos uno")
    void identificadorPresente() {
        verificarIdentificador((c, r) -> ClienteIdentificadorRequest.builder().curp(c).rfc(r).build().isIdentificadorPresente());
        verificarIdentificador((c, r) -> ValidarIdentificadorRequest.builder().curp(c).rfc(r).build().isIdentificadorPresente());
        assertThat(ValidarIdentificadorRequest.builder().correo("a@b.com").build().isIdentificadorPresente()).isTrue();
        assertThat(ValidarIdentificadorRequest.builder().username("usuario1").build().isIdentificadorPresente()).isTrue();
        verificarIdentificador((c, r) -> ActualizarContactoRequest.builder().curp(c).rfc(r).build().isIdentificadorPresente());
        verificarIdentificador((c, r) -> ClienteUpdateRequest.builder().curp(c).rfc(r).build().isIdentificadorPresente());
    }

    @Test
    @DisplayName("La conciliación exige número de autorización solo si el proveedor procesó la transacción")
    void autorizacionSiProcesada() {
        Function<ResolverConciliacionRequest.ResolverConciliacionRequestBuilder, Boolean> valida =
                b -> b.build().isAutorizacionPresenteSiProcesada();
        assertThat(valida.apply(ResolverConciliacionRequest.builder().procesadaPorProveedor(false))).isTrue();
        assertThat(valida.apply(ResolverConciliacionRequest.builder())).isTrue();
        assertThat(valida.apply(ResolverConciliacionRequest.builder().procesadaPorProveedor(true))).isFalse();
        assertThat(valida.apply(ResolverConciliacionRequest.builder().procesadaPorProveedor(true).numAutorizacion(" "))).isFalse();
        assertThat(valida.apply(ResolverConciliacionRequest.builder().procesadaPorProveedor(true).numAutorizacion("AUT"))).isTrue();
    }

    @Test
    @DisplayName("La búsqueda requiere al menos un criterio")
    void criterioBusqueda() {
        Function<BuscarClienteRequest.BuscarClienteRequestBuilder, Boolean> presente = b -> b.build().isCriterioPresente();
        assertThat(presente.apply(BuscarClienteRequest.builder())).isFalse();
        assertThat(presente.apply(BuscarClienteRequest.builder().curp(" ").rfc(" ").correo(" ").numeroCuenta(" ")
                .nombre(" ").apellidoPaterno(" ").apellidoMaterno(" "))).isFalse();
        assertThat(presente.apply(BuscarClienteRequest.builder().curp("C"))).isTrue();
        assertThat(presente.apply(BuscarClienteRequest.builder().rfc("R"))).isTrue();
        assertThat(presente.apply(BuscarClienteRequest.builder().correo("a@b.c"))).isTrue();
        assertThat(presente.apply(BuscarClienteRequest.builder().numeroCuenta("1"))).isTrue();
        assertThat(presente.apply(BuscarClienteRequest.builder().nombre("N"))).isTrue();
        assertThat(presente.apply(BuscarClienteRequest.builder().apellidoPaterno("P"))).isTrue();
        assertThat(presente.apply(BuscarClienteRequest.builder().apellidoMaterno("M"))).isTrue();
        assertThat(presente.apply(BuscarClienteRequest.builder().activo(false))).isTrue();
        assertThat(presente.apply(BuscarClienteRequest.builder().fechaInicio(LocalDate.now()))).isTrue();
        assertThat(presente.apply(BuscarClienteRequest.builder().fechaFin(LocalDate.now()))).isTrue();
    }

    @Test
    @DisplayName("La paginación sustituye valores nulos o fuera de rango por los predeterminados")
    void paginacionSegura() {
        assertThat(BuscarClienteRequest.builder().pagina(null).tamanio(null).build())
                .satisfies(r -> {
                    assertThat(r.getPaginaSegura()).isZero();
                    assertThat(r.getTamanioSeguro()).isEqualTo(10);
                });
        assertThat(BuscarClienteRequest.builder().pagina(-1).tamanio(0).build())
                .satisfies(r -> {
                    assertThat(r.getPaginaSegura()).isZero();
                    assertThat(r.getTamanioSeguro()).isEqualTo(10);
                });
        assertThat(BuscarClienteRequest.builder().pagina(3).tamanio(25).build())
                .satisfies(r -> {
                    assertThat(r.getPaginaSegura()).isEqualTo(3);
                    assertThat(r.getTamanioSeguro()).isEqualTo(25);
                });
    }
}
