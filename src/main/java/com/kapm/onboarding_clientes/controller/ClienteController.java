package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.*;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionIdentificadorResponse;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
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
@Tag(name = "Clientes", description = "Endpoints para la gestión, registro, consulta y actualización de clientes personas físicas (100% mediante Request Body)")
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
    // 2. ACTUALIZACIÓN (PUT) - TODO POR REQUEST BODY
    // =========================================================================

    @PutMapping
    @Operation(
        summary = "Actualizar información de cliente (Request Body)",
        description = "Actualiza los datos del cliente mediante el Request Body. Se identifica al cliente por el campo 'curp' o 'rfc' enviados dentro del JSON. No utiliza PathVariable."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o falta identificador (curp/rfc) en el Request Body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado para el identificador proporcionado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "La nueva CURP o RFC ya pertenecen a otro cliente")
    })
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarCliente(
            @Valid @RequestBody ClienteUpdateRequest request) {
        
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            ClienteResponse cliente = clienteService.actualizarClientePorCurp(request.getCurp(), request);
            return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado correctamente mediante CURP (" + request.getCurp() + ")", cliente));
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            ClienteResponse cliente = clienteService.actualizarClientePorRfc(request.getRfc(), request);
            return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado correctamente mediante RFC (" + request.getRfc() + ")", cliente));
        } else {
            throw new ReglaNegocioException("Debe proporcionar la CURP o el RFC dentro del cuerpo JSON (Request Body) para identificar al cliente a actualizar.");
        }
    }

    // =========================================================================
    // 3. BÚSQUEDA Y CONSULTA ESPECÍFICA (POST) - POR REQUEST BODY
    // =========================================================================

    @PostMapping("/buscar")
    @Operation(
        summary = "Buscar cliente por criterios (Request Body)",
        description = "Busca y retorna un cliente identificándolo por 'curp', 'rfc', 'correo', 'numeroCuenta' o 'id' enviados dentro del Request Body. Sin PathVariable."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No se proporcionó ningún criterio de búsqueda en el Request Body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado con el criterio indicado")
    })
    public ResponseEntity<ApiResponse<ClienteResponse>> buscarCliente(
            @RequestBody BuscarClienteRequest request) {
        
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            ClienteResponse cliente = clienteService.obtenerClientePorCurp(request.getCurp());
            return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por CURP", cliente));
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            ClienteResponse cliente = clienteService.obtenerClientePorRfc(request.getRfc());
            return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por RFC", cliente));
        } else if (request.getCorreo() != null && !request.getCorreo().isBlank()) {
            ClienteResponse cliente = clienteService.obtenerClientePorCorreo(request.getCorreo());
            return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por Correo", cliente));
        } else if (request.getNumeroCuenta() != null && !request.getNumeroCuenta().isBlank()) {
            ClienteResponse cliente = clienteService.obtenerClientePorNumeroCuenta(request.getNumeroCuenta());
            return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por Número de Cuenta", cliente));
        } else if (request.getId() != null) {
            ClienteResponse cliente = clienteService.obtenerClientePorId(request.getId());
            return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por ID", cliente));
        } else {
            throw new ReglaNegocioException("Debe proporcionar al menos un criterio de búsqueda en el Request Body (curp, rfc, correo, numeroCuenta o id).");
        }
    }

    // =========================================================================
    // 4. BAJA LÓGICA (POST / DELETE) - POR REQUEST BODY
    // =========================================================================

    @PostMapping("/desactivar")
    @Operation(
        summary = "Baja Lógica de cliente (Request Body)",
        description = "Desactiva lógicamente al cliente y su cuenta bancaria. Identifica al cliente por 'curp', 'rfc' o 'id' enviados en el Request Body."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente y cuenta desactivados exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Falta identificador en el Request Body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> desactivarCliente(
            @RequestBody ClienteIdentificadorRequest request) {
        ejecutarBajaLogica(request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente dado de baja lógicamente y cuenta desactivada con éxito", null));
    }

    @DeleteMapping
    @Operation(
        summary = "Baja Lógica de cliente (DELETE con Request Body)",
        description = "Desactiva lógicamente al cliente enviando 'curp' o 'rfc' en el cuerpo JSON de la petición."
    )
    public ResponseEntity<ApiResponse<Void>> desactivarClienteDelete(
            @RequestBody ClienteIdentificadorRequest request) {
        ejecutarBajaLogica(request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente dado de baja lógicamente con éxito", null));
    }

    private void ejecutarBajaLogica(ClienteIdentificadorRequest request) {
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            clienteService.desactivarClientePorCurp(request.getCurp());
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            clienteService.desactivarClientePorRfc(request.getRfc());
        } else if (request.getId() != null) {
            clienteService.desactivarCliente(request.getId());
        } else {
            throw new ReglaNegocioException("Debe proporcionar la CURP, el RFC o el ID dentro del Request Body para la baja lógica.");
        }
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
            @RequestBody ClienteIdentificadorRequest request) {
        return ResponseEntity.ok(ApiResponse.exito("Cliente reactivado exitosamente", ejecutarReactivacion(request)));
    }

    @PatchMapping("/reactivar")
    @Operation(
        summary = "Reactivar cliente (PATCH con Request Body)",
        description = "Reactiva al cliente y su cuenta bancaria enviando la 'curp' o 'rfc' en el Request Body."
    )
    public ResponseEntity<ApiResponse<ClienteResponse>> reactivarClientePatch(
            @RequestBody ClienteIdentificadorRequest request) {
        return ResponseEntity.ok(ApiResponse.exito("Cliente reactivado exitosamente", ejecutarReactivacion(request)));
    }

    private ClienteResponse ejecutarReactivacion(ClienteIdentificadorRequest request) {
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            return clienteService.reactivarClientePorCurp(request.getCurp());
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            return clienteService.reactivarClientePorRfc(request.getRfc());
        } else {
            throw new ReglaNegocioException("Debe proporcionar la CURP o el RFC dentro del Request Body para reactivar al cliente.");
        }
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
            @RequestBody ValidarIdentificadorRequest request) {
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            ValidacionIdentificadorResponse resultado = clienteService.validarDisponibilidadCurp(request.getCurp());
            return ResponseEntity.ok(ApiResponse.exito(resultado.getMensaje(), resultado));
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            ValidacionIdentificadorResponse resultado = clienteService.validarDisponibilidadRfc(request.getRfc());
            return ResponseEntity.ok(ApiResponse.exito(resultado.getMensaje(), resultado));
        } else {
            throw new ReglaNegocioException("Debe proporcionar 'curp' o 'rfc' en el Request Body para validar.");
        }
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
        
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            ClienteResponse cliente = clienteService.actualizarContactoPorCurp(request.getCurp(), request);
            return ResponseEntity.ok(ApiResponse.exito("Datos de contacto actualizados correctamente mediante CURP", cliente));
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            ClienteResponse cliente = clienteService.actualizarContactoPorRfc(request.getRfc(), request);
            return ResponseEntity.ok(ApiResponse.exito("Datos de contacto actualizados correctamente mediante RFC", cliente));
        } else {
            throw new ReglaNegocioException("Debe proporcionar 'curp' o 'rfc' dentro del Request Body para actualizar el contacto.");
        }
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
            @RequestBody ClienteIdentificadorRequest request) {
        if (request.getCurp() != null && !request.getCurp().isBlank()) {
            CuentaResponse cuenta = clienteService.obtenerCuentaPorCurp(request.getCurp());
            return ResponseEntity.ok(ApiResponse.exito("Cuenta consultada exitosamente por CURP", cuenta));
        } else if (request.getRfc() != null && !request.getRfc().isBlank()) {
            CuentaResponse cuenta = clienteService.obtenerCuentaPorRfc(request.getRfc());
            return ResponseEntity.ok(ApiResponse.exito("Cuenta consultada exitosamente por RFC", cuenta));
        } else {
            throw new ReglaNegocioException("Debe proporcionar 'curp' o 'rfc' en el Request Body para consultar la cuenta.");
        }
    }

    // =========================================================================
    // 9. FILTRO POR RANGO DE FECHAS (POST) - POR REQUEST BODY
    // =========================================================================

    @PostMapping("/rango-fechas")
    @Operation(
        summary = "Consultar clientes por rango de fechas (Request Body)",
        description = "Filtra clientes registrados entre 'fechaInicio' y 'fechaFin' enviadas dentro del Request Body."
    )
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerClientesPorRangoFechas(
            @Valid @RequestBody RangoFechasRequest request) {
        List<ClienteResponse> clientes = clienteService.obtenerClientesPorRangoFechas(request.getFechaInicio(), request.getFechaFin());
        return ResponseEntity.ok(ApiResponse.exito("Consulta por rango de fechas completada", clientes));
    }

    // =========================================================================
    // 10. LISTADOS GENERALES (GET) - SIN PATH VARIABLE
    // =========================================================================

    @GetMapping
    @Operation(
        summary = "Consultar todos los clientes",
        description = "Retorna el listado completo de todos los clientes registrados en el sistema."
    )
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerTodosLosClientes() {
        List<ClienteResponse> clientes = clienteService.obtenerTodosLosClientes();
        return ResponseEntity.ok(ApiResponse.exito("Consulta realizada con éxito", clientes));
    }

    @GetMapping("/activos")
    @Operation(
        summary = "Consultar clientes activos",
        description = "Retorna únicamente el listado de clientes cuyo estatus sea activo."
    )
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerClientesActivos() {
        List<ClienteResponse> clientes = clienteService.obtenerClientesActivos();
        return ResponseEntity.ok(ApiResponse.exito("Clientes activos consultados con éxito", clientes));
    }
}
