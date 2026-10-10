package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.CatalogoItemDTO;
import com.kapm.onboarding_clientes.dto.response.CatalogosResponse;
import com.kapm.onboarding_clientes.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
@Tag(name = "Catálogos", description = "Endpoints de catálogos estandarizados para prevenir variaciones y discrepancias en los datos")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping
    @Operation(summary = "Consultar todos los catálogos del sistema", description = "Retorna en una sola petición el conjunto completo de catálogos (Sexos, Estados Civiles, Nacionalidades, Ocupaciones, Estados de la República, Países, Tipos de Biometría y Estatus de Cuenta).")
    public ResponseEntity<ApiResponse<CatalogosResponse>> obtenerTodosLosCatalogos() {
        return ResponseEntity.ok(ApiResponse.exito("Catálogos consultados exitosamente", catalogoService.obtenerTodosLosCatalogos()));
    }

    @GetMapping("/sexos")
    @Operation(summary = "Catálogo de Sexos", description = "Retorna las opciones válidas para el campo sexo (MASCULINO, FEMENINO, OTRO).")
    public ResponseEntity<ApiResponse<List<CatalogoItemDTO>>> obtenerSexos() {
        return ResponseEntity.ok(ApiResponse.exito("Catálogo de sexos", catalogoService.obtenerSexos()));
    }

    @GetMapping("/estados-civiles")
    @Operation(summary = "Catálogo de Estados Civiles", description = "Retorna las opciones válidas de estado civil (SOLTERO, CASADO, DIVORCIADO, VIUDO, UNION_LIBRE).")
    public ResponseEntity<ApiResponse<List<CatalogoItemDTO>>> obtenerEstadosCiviles() {
        return ResponseEntity.ok(ApiResponse.exito("Catálogo de estados civiles", catalogoService.obtenerEstadosCiviles()));
    }

    @GetMapping("/nacionalidades")
    @Operation(summary = "Catálogo de Nacionalidades", description = "Retorna el listado estándar de nacionalidades para evitar errores tipográficos.")
    public ResponseEntity<ApiResponse<List<CatalogoItemDTO>>> obtenerNacionalidades() {
        return ResponseEntity.ok(ApiResponse.exito("Catálogo de nacionalidades", catalogoService.obtenerNacionalidades()));
    }

    @GetMapping("/ocupaciones")
    @Operation(summary = "Catálogo de Ocupaciones", description = "Retorna las actividades laborales estandarizadas.")
    public ResponseEntity<ApiResponse<List<CatalogoItemDTO>>> obtenerOcupaciones() {
        return ResponseEntity.ok(ApiResponse.exito("Catálogo de ocupaciones", catalogoService.obtenerOcupaciones()));
    }

    @GetMapping("/estados-republica")
    @Operation(summary = "Catálogo de Entidades Federativas", description = "Retorna las 32 entidades federativas oficiales de México.")
    public ResponseEntity<ApiResponse<List<CatalogoItemDTO>>> obtenerEstadosRepublica() {
        return ResponseEntity.ok(ApiResponse.exito("Catálogo de entidades federativas", catalogoService.obtenerEstadosRepublica()));
    }

    @GetMapping("/paises")
    @Operation(summary = "Catálogo de Países", description = "Retorna el catálogo de países para domicilio.")
    public ResponseEntity<ApiResponse<List<CatalogoItemDTO>>> obtenerPaises() {
        return ResponseEntity.ok(ApiResponse.exito("Catálogo de países", catalogoService.obtenerPaises()));
    }

    @GetMapping("/tipos-biometria")
    @Operation(summary = "Catálogo de Tipos de Biometría", description = "Retorna las modalidades biométricas soportadas.")
    public ResponseEntity<ApiResponse<List<CatalogoItemDTO>>> obtenerTiposBiometria() {
        return ResponseEntity.ok(ApiResponse.exito("Catálogo de tipos de biometría", catalogoService.obtenerTiposBiometria()));
    }

    @GetMapping("/estatus-cuenta")
    @Operation(summary = "Catálogo de Estatus de Cuenta", description = "Retorna los posibles estados financieros de una cuenta bancaria.")
    public ResponseEntity<ApiResponse<List<CatalogoItemDTO>>> obtenerEstatusCuenta() {
        return ResponseEntity.ok(ApiResponse.exito("Catálogo de estatus de cuenta", catalogoService.obtenerEstatusCuenta()));
    }
}
