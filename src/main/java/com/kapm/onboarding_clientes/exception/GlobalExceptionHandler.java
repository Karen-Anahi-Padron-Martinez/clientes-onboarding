package com.kapm.onboarding_clientes.exception;

import com.kapm.onboarding_clientes.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
                .exito(false)
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoDuplicado(
            RecursoDuplicadoException ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
                .exito(false)
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> manejarReglaNegocio(
            ReglaNegocioException ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
                .exito(false)
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Violación de Regla de Negocio")
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ErrorValidacionException.class)
    public ResponseEntity<ErrorResponse> manejarErrorValidacionPersonalizado(
            ErrorValidacionException ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
                .exito(false)
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Error de Validación de Datos")
                .mensaje(ex.getMessage())
                .erroresValidacion(ex.getErroresValidacion())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionSpring(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String campo = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .exito(false)
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Error de Validación de Parámetros")
                .mensaje("La solicitud contiene campos inválidos o incompletos")
                .erroresValidacion(errores)
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
                .exito(false)
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .exito(false)
                .status(HttpStatus.NOT_FOUND.value())
                .error("Ruta no encontrada")
                .mensaje("La ruta '" + request.getRequestURI() + "' no existe. Recuerda usar el plural '/api/clientes'.")
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarExcepcionGeneral(
            Exception ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
                .exito(false)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .mensaje("Ocurrió un error interno en el servidor: " + ex.getMessage())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
