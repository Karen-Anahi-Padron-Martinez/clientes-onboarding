package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.ClienteRegistrationRequest;
import com.kapm.onboarding_clientes.dto.request.ClienteUpdateRequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints para la gestión y consulta de clientes personas físicas")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @Operation(summary = "Registrar un nuevo cliente", description = "Registra un cliente persona física, realiza validaciones de negocio, crea automáticamente su cuenta bancaria y encripta datos de login y biometría.")
    public ResponseEntity<ApiResponse<ClienteResponse>> registrarCliente(
            @Valid @RequestBody ClienteRegistrationRequest request) {
        ClienteResponse cliente = clienteService.registrarCliente(request);
        return new ResponseEntity<>(
                ApiResponse.exito("Cliente registrado exitosamente y cuenta bancaria creada", cliente),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @Operation(summary = "Consultar todos los clientes", description = "Retorna el listado completo de todos los clientes registrados en el sistema.")
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerTodosLosClientes() {
        List<ClienteResponse> clientes = clienteService.obtenerTodosLosClientes();
        return ResponseEntity.ok(ApiResponse.exito("Consulta realizada con éxito", clientes));
    }

    @GetMapping("/activos")
    @Operation(summary = "Consultar clientes activos", description = "Retorna únicamente el listado de clientes cuyo estatus sea activo.")
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerClientesActivos() {
        List<ClienteResponse> clientes = clienteService.obtenerClientesActivos();
        return ResponseEntity.ok(ApiResponse.exito("Clientes activos consultados con éxito", clientes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar cliente por ID", description = "Busca y retorna un cliente por su identificador primario.")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorId(@PathVariable Long id) {
        ClienteResponse cliente = clienteService.obtenerClientePorId(id);
        return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado", cliente));
    }

    @GetMapping("/curp/{curp}")
    @Operation(summary = "Consultar cliente por CURP", description = "Busca y retorna la información de un cliente por su clave de CURP.")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorCurp(@PathVariable String curp) {
        ClienteResponse cliente = clienteService.obtenerClientePorCurp(curp);
        return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por CURP", cliente));
    }

    @GetMapping("/rfc/{rfc}")
    @Operation(summary = "Consultar cliente por RFC", description = "Busca y retorna la información de un cliente por su RFC.")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorRfc(@PathVariable String rfc) {
        ClienteResponse cliente = clienteService.obtenerClientePorRfc(rfc);
        return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por RFC", cliente));
    }

    @GetMapping("/correo/{correo}")
    @Operation(summary = "Consultar cliente por Correo Electrónico", description = "Busca un cliente registrado por su correo electrónico.")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorCorreo(@PathVariable String correo) {
        ClienteResponse cliente = clienteService.obtenerClientePorCorreo(correo);
        return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por Correo", cliente));
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    @Operation(summary = "Consultar cliente por Número de Cuenta", description = "Busca la información del titular asociado a un número de cuenta bancaria.")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorNumeroCuenta(@PathVariable String numeroCuenta) {
        ClienteResponse cliente = clienteService.obtenerClientePorNumeroCuenta(numeroCuenta);
        return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por Número de Cuenta", cliente));
    }

    @GetMapping("/rango-fechas")
    @Operation(summary = "Consultar clientes por Rango de Fechas", description = "Filtra clientes cuyo registro se realizó entre fechaInicio y fechaFin (Formato: YYYY-MM-DD).")
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerClientesPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        List<ClienteResponse> clientes = clienteService.obtenerClientesPorRangoFechas(fechaInicio, fechaFin);
        return ResponseEntity.ok(ApiResponse.exito("Consulta por rango de fechas completada", clientes));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar información de cliente", description = "Permite modificar datos personales, de contacto, domicilio e información laboral. (NO permite modificar CURP, RFC ni número de cuenta).")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteUpdateRequest request) {
        ClienteResponse cliente = clienteService.actualizarCliente(id, request);
        return ResponseEntity.ok(ApiResponse.exito("Información del cliente actualizada correctamente", cliente));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Baja Lógica de cliente", description = "Desactiva lógicamente al cliente sin eliminarlo de la base de datos y desactiva su cuenta bancaria asociada.")
    public ResponseEntity<ApiResponse<Void>> desactivarCliente(@PathVariable Long id) {
        clienteService.desactivarCliente(id);
        return ResponseEntity.ok(ApiResponse.exito("Cliente dado de baja lógicamente y cuenta desactivada con éxito", null));
    }
}
