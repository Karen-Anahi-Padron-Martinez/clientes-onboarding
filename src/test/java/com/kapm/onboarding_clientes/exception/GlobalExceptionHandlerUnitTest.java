package com.kapm.onboarding_clientes.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.kapm.onboarding_clientes.dto.response.GenericResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler: cada manejador responde estatus y {codigo, mensaje}")
class GlobalExceptionHandlerUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/prueba");

    private static GenericResponse cuerpo(ResponseEntity<GenericResponse> respuesta, HttpStatus esperado) {
        assertThat(respuesta.getStatusCode()).isEqualTo(esperado);
        assertThat(respuesta.getBody()).isNotNull();
        return respuesta.getBody();
    }

    static class Dto {
        @NotNull(message = "requerido")
        String campo;
    }

    @Test
    @DisplayName("Bean Validation lista cada campo inválido")
    void methodArgumentNotValid() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Dto(), "dto");
        binding.addError(new FieldError("dto", "campo", "requerido"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mock(MethodParameter.class), binding);

        GenericResponse body = cuerpo(handler.manejarMethodArgumentNotValid(ex, request), HttpStatus.BAD_REQUEST);
        assertThat(body.getMensaje()).contains("campo=requerido");
    }

    @Test
    @DisplayName("JSON ilegible distingue propiedades no permitidas en cualquier nivel de la causa")
    void httpMessageNotReadable() {
        MockHttpInputMessage input = new MockHttpInputMessage(new byte[0]);
        UnrecognizedPropertyException noPermitida =
                new UnrecognizedPropertyException(null, "x", null, Dto.class, "id", List.of());

        HttpMessageNotReadableException sinCausa = new HttpMessageNotReadableException("m", null, input);
        HttpMessageNotReadableException otraCausa = new HttpMessageNotReadableException("m", new RuntimeException(), input);
        HttpMessageNotReadableException anidada = new HttpMessageNotReadableException("m", new RuntimeException(noPermitida), input);

        assertThat(cuerpo(handler.manejarHttpMessageNotReadable(sinCausa, request), HttpStatus.BAD_REQUEST).getCodigo())
                .isEqualTo(ErrorMessage.JSON_MALFORMADO.getCodigo());
        assertThat(cuerpo(handler.manejarHttpMessageNotReadable(otraCausa, request), HttpStatus.BAD_REQUEST).getMensaje())
                .isEqualTo(ErrorMessage.JSON_MALFORMADO.getMensaje());
        assertThat(cuerpo(handler.manejarHttpMessageNotReadable(anidada, request), HttpStatus.BAD_REQUEST).getMensaje())
                .isEqualTo(ErrorMessage.CAMPO_NO_MODIFICABLE.getMensaje());
    }

    @Test
    @DisplayName("Recursos inexistentes, duplicados y reglas de negocio")
    void recursosYReglas() {
        assertThat(cuerpo(handler.manejarRecursoNoEncontrado(new ClienteNoEncontradoException(), request), HttpStatus.NOT_FOUND).getCodigo()).isEqualTo(3);
        assertThat(cuerpo(handler.manejarNoResourceFound(mock(NoResourceFoundException.class), request), HttpStatus.NOT_FOUND).getMensaje())
                .isEqualTo(ErrorMessage.RECURSO_NO_ENCONTRADO.getMensaje());
        assertThat(cuerpo(handler.manejarRecursoDuplicado(new CurpDuplicadaException(), request), HttpStatus.CONFLICT).getCodigo()).isEqualTo(4);
        assertThat(cuerpo(handler.manejarReglaNegocio(new ReglaNegocioException(ErrorMessage.SALDO_INSUFICIENTE), request), HttpStatus.BAD_REQUEST).getCodigo()).isEqualTo(5);
        assertThat(cuerpo(handler.manejarAccesoDenegado(new AccesoDenegadoException(), request), HttpStatus.FORBIDDEN).getCodigo())
                .isEqualTo(ErrorMessage.OPERACION_NO_AUTORIZADA.getCodigo());
    }

    @Test
    @DisplayName("ErrorValidacion agrega el detalle solo cuando hay errores")
    void errorValidacion() {
        String base = ErrorMessage.VALIDACION_FALLIDA.getMensaje();
        assertThat(cuerpo(handler.manejarErrorValidacion(new ErrorValidacionException(ErrorMessage.VALIDACION_FALLIDA, null), request), HttpStatus.BAD_REQUEST).getMensaje())
                .isEqualTo(base);
        assertThat(cuerpo(handler.manejarErrorValidacion(new ErrorValidacionException(ErrorMessage.VALIDACION_FALLIDA, Map.of()), request), HttpStatus.BAD_REQUEST).getMensaje())
                .isEqualTo(base);
        assertThat(cuerpo(handler.manejarErrorValidacion(new ErrorValidacionException(ErrorMessage.VALIDACION_FALLIDA, Map.of("a", "b")), request), HttpStatus.BAD_REQUEST).getMensaje())
                .isEqualTo(base + ": {a=b}");
    }

    @Test
    @DisplayName("Errores de PuntoRed se traducen a mensajes de catálogo según su tipo")
    void puntoRed() {
        assertThat(cuerpo(handler.manejarPuntoRedIntegracion(new PuntoRedAutenticacionException("m", "10", "t"), request), HttpStatus.BAD_GATEWAY).getMensaje())
                .isEqualTo(ErrorMessage.PUNTORED_ERROR_AUTENTICACION.getMensaje());
        assertThat(cuerpo(handler.manejarPuntoRedIntegracion(new PuntoRedRechazoException("06", "t"), request), HttpStatus.UNPROCESSABLE_ENTITY).getMensaje())
                .isEqualTo(ErrorMessage.PUNTORED_TRANSACCION_RECHAZADA.getMensaje());
        assertThat(cuerpo(handler.manejarPuntoRedIntegracion(new PuntoRedTimeoutException("m"), request), HttpStatus.GATEWAY_TIMEOUT).getMensaje())
                .isEqualTo(ErrorMessage.PUNTORED_TIMEOUT.getMensaje());
        assertThat(cuerpo(handler.manejarPuntoRedIntegracion(new PuntoRedIntegracionException("m"), request), HttpStatus.BAD_GATEWAY).getMensaje())
                .isEqualTo(ErrorMessage.ERROR_INTERNO.getMensaje());
    }

    @Test
    @DisplayName("Proveedor no disponible y correo no enviado")
    void proveedorYCorreo() {
        assertThat(cuerpo(handler.manejarProveedorNoDisponible(new ProveedorNoDisponibleException(null), request), HttpStatus.SERVICE_UNAVAILABLE).getCodigo())
                .isEqualTo(ErrorMessage.PROVEEDOR_NO_DISPONIBLE.getCodigo());
        assertThat(cuerpo(handler.manejarProveedorNoDisponible(new ProveedorNoDisponibleException(new RuntimeException()), request), HttpStatus.SERVICE_UNAVAILABLE).getMensaje())
                .isEqualTo(ErrorMessage.PROVEEDOR_NO_DISPONIBLE.getMensaje());
        assertThat(cuerpo(handler.manejarCorreoNoEnviado(new CorreoNoEnviadoException("api key inválida"), request), HttpStatus.INTERNAL_SERVER_ERROR).getMensaje())
                .isEqualTo(ErrorMessage.CORREO_NO_ENVIADO.getMensaje());
    }

    @Test
    @DisplayName("Integridad, método, media type, tipo de parámetro y argumentos inválidos")
    void erroresHttp() {
        assertThat(cuerpo(handler.manejarDataIntegrityViolation(new DataIntegrityViolationException("sql"), request), HttpStatus.CONFLICT).getCodigo())
                .isEqualTo(ErrorMessage.CONFLICTO_INTEGRIDAD.getCodigo());
        assertThat(cuerpo(handler.manejarMetodoNoSoportado(new HttpRequestMethodNotSupportedException("PATCH"), request), HttpStatus.METHOD_NOT_ALLOWED).getCodigo())
                .isEqualTo(ErrorMessage.METODO_NO_PERMITIDO.getCodigo());
        assertThat(cuerpo(handler.manejarMediaTypeNoSoportado(new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of()), request), HttpStatus.UNSUPPORTED_MEDIA_TYPE).getCodigo())
                .isEqualTo(ErrorMessage.MEDIA_TYPE_NO_SOPORTADO.getCodigo());
        assertThat(cuerpo(handler.manejarTypeMismatch(mock(MethodArgumentTypeMismatchException.class), request), HttpStatus.BAD_REQUEST).getMensaje())
                .isEqualTo(ErrorMessage.TIPO_PARAMETRO_INVALIDO.getMensaje());
        assertThat(cuerpo(handler.manejarIllegalArgument(new IllegalArgumentException("técnico"), request), HttpStatus.BAD_REQUEST).getMensaje())
                .isEqualTo(ErrorMessage.DATOS_INVALIDOS.getMensaje());
        assertThat(cuerpo(handler.manejarExcepcionGeneral(new Exception("boom"), request), HttpStatus.INTERNAL_SERVER_ERROR).getMensaje())
                .isEqualTo(ErrorMessage.ERROR_INTERNO.getMensaje());
    }

    @Test
    @DisplayName("ConstraintViolation lista la ruta de cada propiedad")
    void constraintViolation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            ConstraintViolationException ex = new ConstraintViolationException(validator.validate(new Dto()));
            assertThat(cuerpo(handler.manejarConstraintViolation(ex, request), HttpStatus.BAD_REQUEST).getMensaje())
                    .contains("campo=requerido");
        }
    }

    @Test
    @DisplayName("HandlerMethodValidation usa el primer código del error o 'parametro'")
    void handlerMethodValidation() {
        HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
        List<MessageSourceResolvable> errores = List.of(
                new DefaultMessageSourceResolvable(new String[]{"monto"}, "inválido"),
                new DefaultMessageSourceResolvable(new String[0], "vacío"),
                new DefaultMessageSourceResolvable(null, null, "sin códigos"));
        when(ex.getAllErrors()).thenAnswer(inv -> errores);

        assertThat(cuerpo(handler.manejarHandlerMethodValidation(ex, request), HttpStatus.BAD_REQUEST).getMensaje())
                .contains("monto=inválido")
                .contains("parametro=sin códigos");
    }
}
