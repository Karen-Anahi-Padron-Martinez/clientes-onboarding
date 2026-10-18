package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.*;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.PageResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionIdentificadorResponse;
import com.kapm.onboarding_clientes.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints para la gestión, registro, consulta y actualización de clientes personas físicas (100% mediante Request Body, Cero ID de BD)")
public class ClienteController {

    private final ClienteService clienteService;

    // =========================================================================
    // 1. REGISTRO (POST) - TODO POR REQUEST BODY
    // =========================================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Registrar nuevo cliente (Request Body)",
        description = "Registra un cliente persona física a partir de los datos en el Request Body. Aplica validaciones bancarias, genera cuenta bancaria y NO expone IDs de BD en la respuesta."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Cliente registrado exitosamente y cuenta bancaria creada"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o reglas de negocio incumplidas"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "CURP, RFC o Correo ya registrados en el sistema")
    })
    public ResponseEntity<ApiResponse<ClienteResponse>> registrarCliente(
            @Valid @RequestBody ClienteRegistrationRequest request) {
        ClienteResponse cliente = clienteService.registrarCliente(request);
        return new ResponseEntity<>(
                ApiResponse.exito("Cliente registrado exitosamente y cuenta bancaria creada", cliente),
                HttpStatus.CREATED
        );
    }

    // =========================================================================
    // 2. ACTUALIZACIÓN POR CURP Y POR RFC (PUT DEDICADOS) - POR REQUEST BODY
    // =========================================================================

    @PutMapping("/curp")
    @Operation(
        summary = "Actualizar cliente por CURP (Request Body)",
        description = "Actualiza los campos permitidos del cliente (contacto, domicilio, ocupación) identificándolo mediante su CURP oficial en el Request Body. Los campos de identidad (nombre, apellidos, fecha de nacimiento, CURP, RFC y nacionalidad) son inmutables y no se pueden modificar. Sin IDs de base de datos ni PathVariables."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente por CURP"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos, CURP incorrecto o intento de modificar campos inmutables"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado con la CURP proporcionada"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El nuevo correo electrónico ya pertenece a otro cliente")
    })
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarClientePorCurp(
            @Valid @RequestBody ActualizarPorCurpRequest request) {
        ClienteResponse cliente = clienteService.actualizarClientePorCurp(request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado correctamente mediante CURP (" + request.getCurp() + ")", cliente));
    }

    @PutMapping("/rfc")
    @Operation(
        summary = "Actualizar cliente por RFC (Request Body)",
        description = "Actualiza los campos permitidos del cliente (contacto, domicilio, ocupación) identificándolo mediante su RFC oficial en el Request Body. Los campos de identidad (nombre, apellidos, fecha de nacimiento, CURP, RFC y nacionalidad) son inmutables y no se pueden modificar. Sin IDs de base de datos ni PathVariables."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente por RFC"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos, RFC incorrecto o intento de modificar campos inmutables"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado con el RFC proporcionado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El nuevo correo electrónico ya pertenece a otro cliente")
    })
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarClientePorRfc(
            @Valid @RequestBody ActualizarPorRfcRequest request) {
        ClienteResponse cliente = clienteService.actualizarClientePorRfc(request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado correctamente mediante RFC (" + request.getRfc() + ")", cliente));
    }

    @PutMapping
    @Operation(
        summary = "Actualizar información de cliente general (Request Body)",
        description = "Actualiza los campos permitidos del cliente mediante el Request Body. Se identifica al cliente por el campo 'curp' o 'rfc' enviados dentro del JSON. Los 7 campos de identidad (nombre, segundoNombre, apellidos, fechaNacimiento, curp, rfc y nacionalidad) son inmutables y no se pueden modificar bajo ninguna circunstancia. Cero IDs y Cero PathVariable."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o intento de modificar campos inmutables"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El nuevo correo electrónico ya pertenece a otro cliente")
    })
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarCliente(
            @Valid @RequestBody ClienteUpdateRequest request) {
        ClienteResponse cliente = clienteService.actualizarCliente(request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado correctamente", cliente));
    }

    // =========================================================================
    // 3. BÚSQUEDA DINÁMICA MEDIANTE ENDPOINT ÚNICO CON FILTROS COMBINABLES
    // =========================================================================

    @PostMapping("/buscar")
    @Operation(
        summary = "Buscar cliente por criterios combinables (Request Body)",
        description = "Endpoint único de búsqueda dinámica que permite combinar cualquier filtro: curp, rfc, correo, numeroCuenta, nombre, apellidos, activo y rango de fechas en el Request Body. Sin ID de BD, sin PathVariable y sin QueryParam."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Búsqueda exitosa con los clientes encontrados"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No se proporcionó ningún criterio válido de búsqueda en el Request Body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No se encontraron clientes con los criterios indicados")
    })
    public ResponseEntity<ApiResponse<PageResponse<ClienteResponse>>> buscarCliente(
            @Valid @RequestBody BuscarClienteRequest request) {
        PageResponse<ClienteResponse> clientes = clienteService.buscarClientes(request);
        return ResponseEntity.ok(ApiResponse.exito("Búsqueda realizada exitosamente", clientes));
    }

    // =========================================================================
    // 4. BAJA LÓGICA (POST / DELETE) - POR REQUEST BODY
    // =========================================================================

    @PostMapping("/desactivar")
    @Operation(
        summary = "Baja Lógica de cliente (Request Body)",
        description = "Desactiva lógicamente al cliente y su cuenta bancaria. Identifica al cliente por 'curp' o 'rfc' enviados en el Request Body (sin ID)."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente y cuenta desactivados exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Falta identificador en el Request Body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> desactivarCliente(
            @Valid @RequestBody ClienteIdentificadorRequest request) {
        clienteService.desactivarCliente(request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente dado de baja lógicamente y cuenta desactivada con éxito", null));
    }

    @DeleteMapping
    @Operation(
        summary = "Baja Lógica de cliente (DELETE con Request Body)",
        description = "Desactiva lógicamente al cliente enviando 'curp' o 'rfc' en el cuerpo JSON de la petición."
    )
    public ResponseEntity<ApiResponse<Void>> desactivarClienteDelete(
            @Valid @RequestBody ClienteIdentificadorRequest request) {
        clienteService.desactivarCliente(request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente dado de baja lógicamente con éxito", null));
    }

    // =========================================================================
    // 5. REACTIVACIÓN (POST / PATCH) - POR REQUEST BODY
    // =========================================================================

    @PostMapping("/reactivar")
    @Operation(
        summary = "Reactivar cliente (POST con Request Body)",
        description = "Reactiva al cliente y su cuenta bancaria identificándolo por 'curp' o 'rfc' enviados en el Request Body."
    )
    public ResponseEntity<ApiResponse<ClienteResponse>> reactivarClientePost(
            @Valid @RequestBody ClienteIdentificadorRequest request) {
        ClienteResponse cliente = clienteService.reactivarCliente(request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente reactivado exitosamente", cliente));
    }

    @PatchMapping("/reactivar")
    @Operation(
        summary = "Reactivar cliente (PATCH con Request Body)",
        description = "Reactiva al cliente y su cuenta bancaria enviando la 'curp' o 'rfc' en el Request Body."
    )
    public ResponseEntity<ApiResponse<ClienteResponse>> reactivarClientePatch(
            @Valid @RequestBody ClienteIdentificadorRequest request) {
        ClienteResponse cliente = clienteService.reactivarCliente(request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente reactivado exitosamente", cliente));
    }

    // =========================================================================
    // 6. PRE-VALIDACIÓN Y DISPONIBILIDAD (POST) - POR REQUEST BODY
    // =========================================================================

    @PostMapping("/validar")
    @Operation(
        summary = "Pre-validar disponibilidad y sintaxis (Request Body)",
        description = "Verifica la sintaxis oficial y la disponibilidad de 'curp' o 'rfc' enviados dentro del Request Body."
    )
    public ResponseEntity<ApiResponse<ValidacionIdentificadorResponse>> validarDisponibilidad(
            @Valid @RequestBody ValidarIdentificadorRequest request) {
        ValidacionIdentificadorResponse resultado = clienteService.validarDisponibilidad(request);
        return ResponseEntity.ok(ApiResponse.exito(resultado.getMensaje(), resultado));
    }

    // =========================================================================
    // 7. ACTUALIZACIÓN PARCIAL DE CONTACTO (PATCH / PUT) - POR REQUEST BODY
    // =========================================================================

    @PatchMapping("/contacto")
    @Operation(
        summary = "Actualizar contacto de cliente (Request Body)",
        description = "Actualiza correo electrónico y teléfonos identificando al cliente por 'curp' o 'rfc' enviados dentro del Request Body."
    )
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarContacto(
            @Valid @RequestBody ActualizarContactoRequest request) {
        ClienteResponse cliente = clienteService.actualizarContacto(request);
        return ResponseEntity.ok(ApiResponse.exito("Datos de contacto actualizados correctamente", cliente));
    }

    // =========================================================================
    // 8. CONSULTA DE CUENTA Y SALDO (POST) - POR REQUEST BODY
    // =========================================================================

    @PostMapping("/cuenta")
    @Operation(
        summary = "Consultar cuenta bancaria y saldo (Request Body)",
        description = "Obtiene los detalles de cuenta bancaria y saldo identificando al titular por 'curp' o 'rfc' enviados en el Request Body."
    )
    public ResponseEntity<ApiResponse<CuentaResponse>> obtenerCuenta(
            @Valid @RequestBody ClienteIdentificadorRequest request) {
        CuentaResponse cuenta = clienteService.obtenerCuenta(request);
        return ResponseEntity.ok(ApiResponse.exito("Cuenta consultada exitosamente", cuenta));
    }



    // =========================================================================
    // 10. LISTADOS GENERALES (GET) - SIN PATH VARIABLE
    // =========================================================================

    @GetMapping
    @Operation(
        summary = "Consultar todos los clientes",
        description = "Retorna el listado completo de todos los clientes registrados en el sistema (sin exponer IDs de base de datos)."
    )
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerTodosLosClientes() {
        List<ClienteResponse> clientes = clienteService.obtenerTodosLosClientes();
        return ResponseEntity.ok(ApiResponse.exito("Consulta realizada con éxito", clientes));
    }

    @GetMapping("/activos")
    @Operation(
        summary = "Consultar clientes activos",
        description = "Retorna únicamente el listado de clientes cuyo estatus sea activo (sin exponer IDs de base de datos)."
    )
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerClientesActivos() {
        List<ClienteResponse> clientes = clienteService.obtenerClientesActivos();
        return ResponseEntity.ok(ApiResponse.exito("Clientes activos consultados con éxito", clientes));
    }
}
