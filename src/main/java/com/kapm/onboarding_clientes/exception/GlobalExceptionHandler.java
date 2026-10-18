package com.kapm.onboarding_clientes.exception;

import com.kapm.onboarding_clientes.dto.response.GenericResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global centralizado de excepciones con @RestControllerAdvice y @Slf4j.
 * Estandariza TODAS las respuestas de error a ÚNICAMENTE dos atributos:
 * { "codigo": <número>, "mensaje": "<texto>" }
 * Protege información sensible, registra trazas en log.error solo para 500
 * y advertencias en log.warn para 4xx.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // 1. BEAN VALIDATION (400) - MethodArgumentNotValidException
    // =========================================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> manejarMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("Falla de Bean Validation en ruta {}: {} error(es)", request.getRequestURI(), ex.getBindingResult().getErrorCount());

        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String mensaje = ErrorMessage.DATOS_INVALIDOS.getMensaje() + ": " + errores;

        GenericResponse response = new GenericResponse(ErrorMessage.DATOS_INVALIDOS.getCodigo(), mensaje);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 2. CUERPO JSON MALFORMADO O ILEGIBLE (400) - HttpMessageNotReadableException
    // =========================================================================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> manejarHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Cuerpo de solicitud mal formado o con propiedades no permitidas en ruta {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorMessage em = ErrorMessage.JSON_MALFORMADO;
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException) {
                em = ErrorMessage.CAMPO_NO_MODIFICABLE;
                break;
            }
            cause = cause.getCause();
        }

        GenericResponse response = new GenericResponse(
                em.getCodigo(),
                em.getMensaje()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 3. RECURSO NO ENCONTRADO (404) - RecursoNoEncontradoException, NoResourceFoundException
    // =========================================================================
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<GenericResponse> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest request) {

        log.warn("Recurso no encontrado en ruta {}: [{}] {}", request.getRequestURI(), ex.getCodigo(), ex.getMessage());

        GenericResponse response = new GenericResponse(ex.getCodigo(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GenericResponse> manejarNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.warn("Ruta no encontrada en el servidor: {}", request.getRequestURI());

        GenericResponse response = new GenericResponse(
                ErrorMessage.RECURSO_NO_ENCONTRADO.getCodigo(),
                ErrorMessage.RECURSO_NO_ENCONTRADO.getMensaje()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // =========================================================================
    // 4. RECURSO DUPLICADO (409) - RecursoDuplicadoException
    // =========================================================================
    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<GenericResponse> manejarRecursoDuplicado(
            RecursoDuplicadoException ex, HttpServletRequest request) {

        log.warn("Conflicto de recurso duplicado en ruta {}: [{}] {}", request.getRequestURI(), ex.getCodigo(), ex.getMessage());

        GenericResponse response = new GenericResponse(ex.getCodigo(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // =========================================================================
    // 5. REGLAS DE NEGOCIO Y VALIDACIÓN PERSONALIZADA (400)
    // =========================================================================
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<GenericResponse> manejarReglaNegocio(
            ReglaNegocioException ex, HttpServletRequest request) {

        log.warn("Violación de regla de negocio en ruta {}: [{}] {}", request.getRequestURI(), ex.getCodigo(), ex.getMessage());

        GenericResponse response = new GenericResponse(ex.getCodigo(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ErrorValidacionException.class)
    public ResponseEntity<GenericResponse> manejarErrorValidacion(
            ErrorValidacionException ex, HttpServletRequest request) {

        log.warn("Error de validación en ruta {}: [{}] {}", request.getRequestURI(), ex.getCodigo(), ex.getMessage());

        String mensaje = ex.getErroresValidacion() != null && !ex.getErroresValidacion().isEmpty()
                ? ex.getMessage() + ": " + new LinkedHashMap<>(ex.getErroresValidacion())
                : ex.getMessage();

        GenericResponse response = new GenericResponse(ex.getCodigo(), mensaje);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 6. CONFLICTO DE INTEGRIDAD DE DATOS (409) - DataIntegrityViolationException
    // =========================================================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> manejarDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("Violación de integridad de datos en BD al procesar ruta {}: {}", request.getRequestURI(), ex.getMessage());

        GenericResponse response = new GenericResponse(
                ErrorMessage.CONFLICTO_INTEGRIDAD.getCodigo(),
                ErrorMessage.CONFLICTO_INTEGRIDAD.getMensaje()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // =========================================================================
    // 7. MÉTODO HTTP NO PERMITIDO (405) - HttpRequestMethodNotSupportedException
    // =========================================================================
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericResponse> manejarMetodoNoSoportado(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("Método HTTP {} no soportado en ruta {}", ex.getMethod(), request.getRequestURI());

        GenericResponse response = new GenericResponse(
                ErrorMessage.METODO_NO_PERMITIDO.getCodigo(),
                ErrorMessage.METODO_NO_PERMITIDO.getMensaje()
        );
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // =========================================================================
    // 8. MEDIA TYPE NO SOPORTADO (415) - HttpMediaTypeNotSupportedException
    // =========================================================================
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GenericResponse> manejarMediaTypeNoSoportado(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        log.warn("Tipo de contenido multimedia {} no soportado en ruta {}", ex.getContentType(), request.getRequestURI());

        GenericResponse response = new GenericResponse(
                ErrorMessage.MEDIA_TYPE_NO_SOPORTADO.getCodigo(),
                ErrorMessage.MEDIA_TYPE_NO_SOPORTADO.getMensaje()
        );
        return new ResponseEntity<>(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // =========================================================================
    // VALIDACIONES ADICIONALES DE PARÁMETROS (400)
    // =========================================================================
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> manejarConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        log.warn("Violación de restricciones de validación en ruta {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> errores = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errores.put(v.getPropertyPath().toString(), v.getMessage()));

        String mensaje = ErrorMessage.DATOS_INVALIDOS.getMensaje() + ": " + errores;
        GenericResponse response = new GenericResponse(ErrorMessage.DATOS_INVALIDOS.getCodigo(), mensaje);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<GenericResponse> manejarHandlerMethodValidation(
            HandlerMethodValidationException ex, HttpServletRequest request) {

        log.warn("Error de validación en controlador en ruta {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> errores = new LinkedHashMap<>();
        ex.getAllErrors().forEach(err -> {
            String campo = err.getCodes() != null && err.getCodes().length > 0 ? err.getCodes()[0] : "parametro";
            errores.put(campo, err.getDefaultMessage());
        });

        String mensaje = ErrorMessage.DATOS_INVALIDOS.getMensaje() + ": " + errores;
        GenericResponse response = new GenericResponse(ErrorMessage.DATOS_INVALIDOS.getCodigo(), mensaje);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse> manejarTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn("Discrepancia de tipo de parámetro en ruta {}: parámetro '{}'", request.getRequestURI(), ex.getName());

        GenericResponse response = new GenericResponse(
                ErrorMessage.TIPO_PARAMETRO_INVALIDO.getCodigo(),
                ErrorMessage.TIPO_PARAMETRO_INVALIDO.getMensaje()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GenericResponse> manejarIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Argumento inválido en ruta {}: {}", request.getRequestURI(), ex.getMessage());

        GenericResponse response = new GenericResponse(
                ErrorMessage.DATOS_INVALIDOS.getCodigo(),
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : ErrorMessage.DATOS_INVALIDOS.getMensaje()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 99. FALLBACK DE ERROR INESPERADO (500) - Exception
    // =========================================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> manejarExcepcionGeneral(
            Exception ex, HttpServletRequest request) {

        log.error("Error no controlado en el servidor al procesar ruta: " + request.getRequestURI(), ex);

        GenericResponse response = new GenericResponse(
                ErrorMessage.ERROR_INTERNO.getCodigo(),
                ErrorMessage.ERROR_INTERNO.getMensaje()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
