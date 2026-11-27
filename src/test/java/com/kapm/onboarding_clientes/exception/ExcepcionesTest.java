package com.kapm.onboarding_clientes.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Constructores y códigos de las excepciones de dominio")
class ExcepcionesTest {

    private static final RuntimeException CAUSA = new RuntimeException("causa");

    @Test
    @DisplayName("RecursoNoEncontrado y subclases componen mensaje con detalle opcional")
    void recursoNoEncontrado() {
        assertThat(new RecursoNoEncontradoException("msg").getErrorMessage()).isEqualTo(ErrorMessage.RECURSO_NO_ENCONTRADO);
        assertThat(new RecursoNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, null).getMessage())
                .isEqualTo(ErrorMessage.CLIENTE_NO_ENCONTRADO.getMensaje());
        assertThat(new RecursoNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, " ").getMessage())
                .isEqualTo(ErrorMessage.CLIENTE_NO_ENCONTRADO.getMensaje());
        assertThat(new RecursoNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO, "x").getMessage())
                .isEqualTo(ErrorMessage.CLIENTE_NO_ENCONTRADO.getMensaje() + ": x");
        assertThat(new RecursoNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO).getCodigo()).isEqualTo(3);

        assertThat(new ClienteNoEncontradoException().getErrorMessage()).isEqualTo(ErrorMessage.CLIENTE_NO_ENCONTRADO);
        assertThat(new ClienteNoEncontradoException(ErrorMessage.CLIENTE_SIN_CUENTA).getErrorMessage()).isEqualTo(ErrorMessage.CLIENTE_SIN_CUENTA);
        assertThat(new ClienteNoEncontradoException(ErrorMessage.CLIENTE_SIN_CUENTA, "d").getMessage()).endsWith(": d");
        assertThat(new ClienteNoEncontradoException("CURP").getMessage()).endsWith(": CURP");
        assertThat(new ClienteNoEncontradoException(7L).getMessage()).endsWith(": 7");

        assertThat(new CuentaNoEncontradaException().getErrorMessage()).isEqualTo(ErrorMessage.CUENTA_NO_ENCONTRADA);
        assertThat(new CuentaNoEncontradaException(ErrorMessage.CUENTA_NO_ASOCIADA_CLIENTE).getErrorMessage()).isEqualTo(ErrorMessage.CUENTA_NO_ASOCIADA_CLIENTE);
        assertThat(new CuentaNoEncontradaException(ErrorMessage.CUENTA_NO_ASOCIADA_CLIENTE, "d").getMessage()).endsWith(": d");
        assertThat(new CuentaNoEncontradaException("123").getMessage()).endsWith(": 123");
        assertThat(new CuentaNoEncontradaException(9L).getMessage()).endsWith(": 9");
    }

    @Test
    @DisplayName("RecursoDuplicado y subclases componen mensaje con detalle opcional")
    void recursoDuplicado() {
        assertThat(new RecursoDuplicadoException("msg").getErrorMessage()).isEqualTo(ErrorMessage.RECURSO_DUPLICADO);
        assertThat(new RecursoDuplicadoException(ErrorMessage.CURP_DUPLICADA, null).getMessage()).isEqualTo(ErrorMessage.CURP_DUPLICADA.getMensaje());
        assertThat(new RecursoDuplicadoException(ErrorMessage.CURP_DUPLICADA, "").getMessage()).isEqualTo(ErrorMessage.CURP_DUPLICADA.getMensaje());
        assertThat(new RecursoDuplicadoException(ErrorMessage.CURP_DUPLICADA).getCodigo()).isEqualTo(4);

        assertThat(new CurpDuplicadaException().getErrorMessage()).isEqualTo(ErrorMessage.CURP_DUPLICADA);
        assertThat(new CurpDuplicadaException(ErrorMessage.RECURSO_DUPLICADO).getErrorMessage()).isEqualTo(ErrorMessage.RECURSO_DUPLICADO);
        assertThat(new CurpDuplicadaException(ErrorMessage.CURP_DUPLICADA, "C").getMessage()).endsWith(": C");
        assertThat(new CurpDuplicadaException("C").getMessage()).endsWith(": C");

        assertThat(new RfcDuplicadoException().getErrorMessage()).isEqualTo(ErrorMessage.RFC_DUPLICADO);
        assertThat(new RfcDuplicadoException(ErrorMessage.RECURSO_DUPLICADO).getErrorMessage()).isEqualTo(ErrorMessage.RECURSO_DUPLICADO);
        assertThat(new RfcDuplicadoException(ErrorMessage.RFC_DUPLICADO, "R").getMessage()).endsWith(": R");
        assertThat(new RfcDuplicadoException("R").getMessage()).endsWith(": R");

        assertThat(new CorreoDuplicadoException().getErrorMessage()).isEqualTo(ErrorMessage.CORREO_DUPLICADO);
        assertThat(new CorreoDuplicadoException(ErrorMessage.RECURSO_DUPLICADO).getErrorMessage()).isEqualTo(ErrorMessage.RECURSO_DUPLICADO);
        assertThat(new CorreoDuplicadoException(ErrorMessage.CORREO_DUPLICADO, "a@b.c").getMessage()).endsWith(": a@b.c");
        assertThat(new CorreoDuplicadoException("a@b.c").getMessage()).endsWith(": a@b.c");

        assertThat(new ClienteYaRegistradoException().getErrorMessage()).isEqualTo(ErrorMessage.CLIENTE_YA_REGISTRADO);
        assertThat(new ClienteYaRegistradoException(ErrorMessage.RECURSO_DUPLICADO).getErrorMessage()).isEqualTo(ErrorMessage.RECURSO_DUPLICADO);
        assertThat(new ClienteYaRegistradoException(ErrorMessage.CLIENTE_YA_REGISTRADO, "d").getMessage()).endsWith(": d");
        assertThat(new ClienteYaRegistradoException("d").getMessage()).endsWith(": d");
    }

    @Test
    @DisplayName("ReglaNegocio compone mensaje con detalle opcional")
    void reglaNegocio() {
        assertThat(new ReglaNegocioException("msg").getErrorMessage()).isEqualTo(ErrorMessage.REGLA_NEGOCIO);
        assertThat(new ReglaNegocioException(ErrorMessage.SALDO_INSUFICIENTE, null).getMessage()).isEqualTo(ErrorMessage.SALDO_INSUFICIENTE.getMensaje());
        assertThat(new ReglaNegocioException(ErrorMessage.SALDO_INSUFICIENTE, " ").getMessage()).isEqualTo(ErrorMessage.SALDO_INSUFICIENTE.getMensaje());
        assertThat(new ReglaNegocioException(ErrorMessage.SALDO_INSUFICIENTE, "d").getMessage()).endsWith(": d");
        assertThat(new ReglaNegocioException(ErrorMessage.SALDO_INSUFICIENTE).getCodigo()).isEqualTo(5);
    }

    @Test
    @DisplayName("ErrorValidacion conserva el mapa de errores y el código")
    void errorValidacion() {
        Map<String, String> errores = Map.of("campo", "inválido");
        ErrorValidacionException conEnum = new ErrorValidacionException(ErrorMessage.DATOS_INVALIDOS, errores);
        assertThat(conEnum.getMessage()).isEqualTo(ErrorMessage.DATOS_INVALIDOS.getMensaje());
        assertThat(conEnum.getErroresValidacion()).isEqualTo(errores);
        assertThat(conEnum.getCodigo()).isEqualTo(1);

        ErrorValidacionException conMensaje = new ErrorValidacionException("libre", errores);
        assertThat(conMensaje.getMessage()).isEqualTo("libre");
        assertThat(conMensaje.getErrorMessage()).isEqualTo(ErrorMessage.VALIDACION_FALLIDA);
    }

    @Test
    @DisplayName("CorreoNoEnviado admite código, detalle y causa")
    void correoNoEnviado() {
        int codigoCorreo = ErrorMessage.CORREO_NO_ENVIADO.getCodigo();
        assertThat(new CorreoNoEnviadoException("m").getCodigo()).isEqualTo(codigoCorreo);
        assertThat(new CorreoNoEnviadoException("m", CAUSA).getCause()).isSameAs(CAUSA);
        assertThat(new CorreoNoEnviadoException(ErrorMessage.CORREO_NO_ENVIADO, null).getMessage()).isEqualTo(ErrorMessage.CORREO_NO_ENVIADO.getMensaje());
        assertThat(new CorreoNoEnviadoException(ErrorMessage.CORREO_NO_ENVIADO, "").getMessage()).isEqualTo(ErrorMessage.CORREO_NO_ENVIADO.getMensaje());
        assertThat(new CorreoNoEnviadoException(ErrorMessage.CORREO_NO_ENVIADO, "d").getMessage()).endsWith(": d");
        CorreoNoEnviadoException conCodigo = new CorreoNoEnviadoException(42, "m");
        assertThat(conCodigo.getCodigo()).isEqualTo(42);
        CorreoNoEnviadoException conCausa = new CorreoNoEnviadoException(43, "m", CAUSA);
        assertThat(conCausa.getCodigo()).isEqualTo(43);
        assertThat(conCausa.getCause()).isSameAs(CAUSA);
    }

    @Test
    @DisplayName("Excepciones de proveedor y acceso exponen su código")
    void proveedorYAcceso() {
        assertThat(new ProveedorNoDisponibleException(CAUSA).getCodigo()).isEqualTo(ErrorMessage.PROVEEDOR_NO_DISPONIBLE.getCodigo());
        assertThat(new AccesoDenegadoException().getCodigo()).isEqualTo(ErrorMessage.OPERACION_NO_AUTORIZADA.getCodigo());
        PuntoRedRespuestaInvalidaException invalida = new PuntoRedRespuestaInvalidaException("m", CAUSA);
        assertThat(invalida.getCodigo()).isEqualTo(ErrorMessage.PUNTORED_RESPUESTA_INVALIDA.getCodigo());
        assertThat(invalida.getCause()).isSameAs(CAUSA);
    }
}
