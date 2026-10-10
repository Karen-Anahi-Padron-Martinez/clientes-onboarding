package com.kapm.onboarding_clientes.controller;

import com.kapm.onboarding_clientes.dto.request.ActualizarContactoRequest;
import com.kapm.onboarding_clientes.dto.request.ClienteRegistrationRequest;
import com.kapm.onboarding_clientes.dto.request.ClienteUpdateRequest;
import com.kapm.onboarding_clientes.dto.response.ApiResponse;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.CuentaResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionIdentificadorResponse;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Clientes", description = "Endpoints para la gestión, registro, consulta y actualización de clientes personas físicas")
public class ClienteController {

    private final ClienteService clienteService;

    // =========================================================================
    // REGISTRO (POST)
    // =========================================================================

    @PostMapping
    @Operation(summary = "Registrar un nuevo cliente", description = "Registra un cliente persona física, realiza validaciones de negocio, crea automáticamente su cuenta bancaria y encripta datos de login y biometría. (La respuesta no expone identificadores de BD).")
    public ResponseEntity<ApiResponse<ClienteResponse>> registrarCliente(
            @Valid @RequestBody ClienteRegistrationRequest request) {
        ClienteResponse cliente = clienteService.registrarCliente(request);
        return new ResponseEntity<>(
                ApiResponse.exito("Cliente registrado exitosamente y cuenta bancaria creada", cliente),
                HttpStatus.CREATED
        );
    }

    // =========================================================================
    // CONSULTAS (GET) - CON REQUEST PARAM Y GENERALES
    // =========================================================================

    @GetMapping
    @Operation(summary = "Consultar clientes", description = "Retorna el listado completo de clientes, o filtra por CURP o RFC si se envían como RequestParam (?curp=... o ?rfc=...).")
    public ResponseEntity<ApiResponse<?>> obtenerClientes(
            @Parameter(description = "Filtrar por CURP (opcional)", example = "MEHC920824HDFRMN09")
            @RequestParam(required = false) String curp,
            @Parameter(description = "Filtrar por RFC (opcional)", example = "MEHC9208243A8")
            @RequestParam(required = false) String rfc) {
        if (curp != null && !curp.isBlank()) {
            ClienteResponse cliente = clienteService.obtenerClientePorCurp(curp);
            return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por CURP", cliente));
        }
        if (rfc != null && !rfc.isBlank()) {
            ClienteResponse cliente = clienteService.obtenerClientePorRfc(rfc);
            return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por RFC", cliente));
        }
        List<ClienteResponse> clientes = clienteService.obtenerTodosLosClientes();
        return ResponseEntity.ok(ApiResponse.exito("Consulta realizada con éxito", clientes));
    }

    @GetMapping("/activos")
    @Operation(summary = "Consultar clientes activos", description = "Retorna únicamente el listado de clientes cuyo estatus sea activo.")
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerClientesActivos() {
        List<ClienteResponse> clientes = clienteService.obtenerClientesActivos();
        return ResponseEntity.ok(ApiResponse.exito("Clientes activos consultados con éxito", clientes));
    }

    @GetMapping("/curp")
    @Operation(summary = "Consultar cliente por CURP usando RequestParam", description = "Busca y retorna la información de un cliente por clave CURP recibida en el request (?curp=...).")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorCurpParam(@RequestParam String curp) {
        ClienteResponse cliente = clienteService.obtenerClientePorCurp(curp);
        return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por CURP", cliente));
    }

    @GetMapping("/rfc")
    @Operation(summary = "Consultar cliente por RFC usando RequestParam", description = "Busca y retorna la información de un cliente por clave RFC recibida en el request (?rfc=...).")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorRfcParam(@RequestParam String rfc) {
        ClienteResponse cliente = clienteService.obtenerClientePorRfc(rfc);
        return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por RFC", cliente));
    }

    @GetMapping("/rango-fechas")
    @Operation(summary = "Consultar clientes por Rango de Fechas", description = "Filtra clientes cuyo registro se realizó entre fechaInicio y fechaFin (Formato: YYYY-MM-DD).")
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerClientesPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        List<ClienteResponse> clientes = clienteService.obtenerClientesPorRangoFechas(fechaInicio, fechaFin);
        return ResponseEntity.ok(ApiResponse.exito("Consulta por rango de fechas completada", clientes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar cliente por ID", description = "Busca y retorna un cliente por su identificador primario.")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorId(@PathVariable Long id) {
        ClienteResponse cliente = clienteService.obtenerClientePorId(id);
        return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado", cliente));
    }

    @GetMapping("/curp/{curp}")
    @Operation(summary = "Consultar cliente por CURP (PathVariable)", description = "Busca y retorna la información de un cliente por su clave de CURP en la URL.")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorCurpPath(@PathVariable String curp) {
        ClienteResponse cliente = clienteService.obtenerClientePorCurp(curp);
        return ResponseEntity.ok(ApiResponse.exito("Cliente encontrado por CURP", cliente));
    }

    @GetMapping("/rfc/{rfc}")
    @Operation(summary = "Consultar cliente por RFC (PathVariable)", description = "Busca y retorna la información de un cliente por su RFC en la URL.")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorRfcPath(@PathVariable String rfc) {
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

    // =========================================================================
    // ACTUALIZACIÓN (PUT) - MEDIANTE REQUEST (RequestParam o Request Body)
    // =========================================================================

    @PutMapping
    @Operation(
        summary = "Actualizar información de cliente por CURP o RFC con Request",
        description = "Permite actualizar al cliente identificándolo por CURP o RFC recibidos por RequestParam (?curp=... o ?rfc=...) o directamente dentro del cuerpo JSON (Request Body). No requiere PathVariable."
    )
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarClienteConRequest(
            @Parameter(description = "CURP del cliente a actualizar (opcional si se envía en el body)", example = "MEHC920824HDFRMN09")
            @RequestParam(required = false) String curp,
            @Parameter(description = "RFC del cliente a actualizar (opcional si se envía en el body)", example = "MEHC9208243A8")
            @RequestParam(required = false) String rfc,
            @Valid @RequestBody ClienteUpdateRequest request) {
        
        String curpTarget = (curp != null && !curp.isBlank()) ? curp : request.getCurp();
        String rfcTarget = (rfc != null && !rfc.isBlank()) ? rfc : request.getRfc();

        if (curpTarget != null && !curpTarget.isBlank()) {
            ClienteResponse cliente = clienteService.actualizarClientePorCurp(curpTarget, request);
            return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado correctamente mediante CURP: " + curpTarget, cliente));
        } else if (rfcTarget != null && !rfcTarget.isBlank()) {
            ClienteResponse cliente = clienteService.actualizarClientePorRfc(rfcTarget, request);
            return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado correctamente mediante RFC: " + rfcTarget, cliente));
        } else {
            throw new ReglaNegocioException("Debe proporcionar la CURP o el RFC del cliente a actualizar en el request (como parámetro ?curp=... / ?rfc=... o en el cuerpo JSON de la solicitud).");
        }
    }

    @PutMapping("/curp")
    @Operation(summary = "Actualizar cliente por CURP con RequestParam", description = "Busca al cliente por la CURP enviada en el request (?curp=... o en el cuerpo) y actualiza sus datos.")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarClientePorCurpParam(
            @Parameter(description = "CURP del cliente a actualizar", example = "MEHC920824HDFRMN09")
            @RequestParam(required = false) String curp,
            @Valid @RequestBody ClienteUpdateRequest request) {
        String curpFinal = (curp != null && !curp.isBlank()) ? curp : request.getCurp();
        if (curpFinal == null || curpFinal.isBlank()) {
            throw new ReglaNegocioException("La CURP es obligatoria en el request para esta actualización (?curp=... o en el cuerpo).");
        }
        ClienteResponse cliente = clienteService.actualizarClientePorCurp(curpFinal, request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado correctamente mediante CURP: " + curpFinal, cliente));
    }

    @PutMapping("/rfc")
    @Operation(summary = "Actualizar cliente por RFC con RequestParam", description = "Busca al cliente por el RFC enviado en el request (?rfc=... o en el cuerpo) y actualiza sus datos.")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarClientePorRfcParam(
            @Parameter(description = "RFC del cliente a actualizar", example = "MEHC9208243A8")
            @RequestParam(required = false) String rfc,
            @Valid @RequestBody ClienteUpdateRequest request) {
        String rfcFinal = (rfc != null && !rfc.isBlank()) ? rfc : request.getRfc();
        if (rfcFinal == null || rfcFinal.isBlank()) {
            throw new ReglaNegocioException("El RFC es obligatorio en el request para esta actualización (?rfc=... o en el cuerpo).");
        }
        ClienteResponse cliente = clienteService.actualizarClientePorRfc(rfcFinal, request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado correctamente mediante RFC: " + rfcFinal, cliente));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar información de cliente por ID", description = "Permite modificar datos de cliente por ID primario.")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarClientePorId(
            @PathVariable Long id,
            @Valid @RequestBody ClienteUpdateRequest request) {
        ClienteResponse cliente = clienteService.actualizarCliente(id, request);
        return ResponseEntity.ok(ApiResponse.exito("Información del cliente actualizada correctamente", cliente));
    }

    @PutMapping("/curp/{curp}")
    @Operation(summary = "Actualizar información de cliente por CURP (PathVariable)", description = "Busca al cliente por su CURP en la URL y actualiza sus datos.")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarClientePorCurpPath(
            @PathVariable String curp,
            @Valid @RequestBody ClienteUpdateRequest request) {
        ClienteResponse cliente = clienteService.actualizarClientePorCurp(curp, request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado correctamente mediante CURP: " + curp, cliente));
    }

    @PutMapping("/rfc/{rfc}")
    @Operation(summary = "Actualizar información de cliente por RFC (PathVariable)", description = "Busca al cliente por su RFC en la URL y actualiza sus datos.")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarClientePorRfcPath(
            @PathVariable String rfc,
            @Valid @RequestBody ClienteUpdateRequest request) {
        ClienteResponse cliente = clienteService.actualizarClientePorRfc(rfc, request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado correctamente mediante RFC: " + rfc, cliente));
    }

    // =========================================================================
    // BAJA LÓGICA (DELETE) - CON REQUEST Y PATH VARIABLE
    // =========================================================================

    @DeleteMapping
    @Operation(summary = "Baja Lógica de cliente por RequestParam", description = "Desactiva lógicamente al cliente usando ?curp=... o ?rfc=... en el request.")
    public ResponseEntity<ApiResponse<Void>> desactivarClientePorRequest(
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc) {
        if (curp != null && !curp.isBlank()) {
            clienteService.desactivarClientePorCurp(curp);
            return ResponseEntity.ok(ApiResponse.exito("Cliente con CURP " + curp + " dado de baja lógicamente", null));
        } else if (rfc != null && !rfc.isBlank()) {
            clienteService.desactivarClientePorRfc(rfc);
            return ResponseEntity.ok(ApiResponse.exito("Cliente con RFC " + rfc + " dado de baja lógicamente", null));
        } else {
            throw new ReglaNegocioException("Debe proporcionar ?curp=... o ?rfc=... para realizar la baja lógica.");
        }
    }

    @DeleteMapping("/curp")
    @Operation(summary = "Baja Lógica de cliente por CURP usando RequestParam", description = "Desactiva lógicamente al cliente usando ?curp=...")
    public ResponseEntity<ApiResponse<Void>> desactivarClientePorCurpParam(@RequestParam String curp) {
        clienteService.desactivarClientePorCurp(curp);
        return ResponseEntity.ok(ApiResponse.exito("Cliente con CURP " + curp + " dado de baja lógicamente con éxito", null));
    }

    @DeleteMapping("/rfc")
    @Operation(summary = "Baja Lógica de cliente por RFC usando RequestParam", description = "Desactiva lógicamente al cliente usando ?rfc=...")
    public ResponseEntity<ApiResponse<Void>> desactivarClientePorRfcParam(@RequestParam String rfc) {
        clienteService.desactivarClientePorRfc(rfc);
        return ResponseEntity.ok(ApiResponse.exito("Cliente con RFC " + rfc + " dado de baja lógicamente con éxito", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Baja Lógica de cliente por ID", description = "Desactiva lógicamente al cliente por ID primario.")
    public ResponseEntity<ApiResponse<Void>> desactivarClientePorId(@PathVariable Long id) {
        clienteService.desactivarCliente(id);
        return ResponseEntity.ok(ApiResponse.exito("Cliente dado de baja lógicamente y cuenta desactivada con éxito", null));
    }

    @DeleteMapping("/curp/{curp}")
    @Operation(summary = "Baja Lógica de cliente por CURP (PathVariable)", description = "Desactiva lógicamente al cliente utilizando su clave CURP en la URL.")
    public ResponseEntity<ApiResponse<Void>> desactivarClientePorCurpPath(@PathVariable String curp) {
        clienteService.desactivarClientePorCurp(curp);
        return ResponseEntity.ok(ApiResponse.exito("Cliente con CURP " + curp + " dado de baja lógicamente con éxito", null));
    }

    @DeleteMapping("/rfc/{rfc}")
    @Operation(summary = "Baja Lógica de cliente por RFC (PathVariable)", description = "Desactiva lógicamente al cliente utilizando su clave RFC en la URL.")
    public ResponseEntity<ApiResponse<Void>> desactivarClientePorRfcPath(@PathVariable String rfc) {
        clienteService.desactivarClientePorRfc(rfc);
        return ResponseEntity.ok(ApiResponse.exito("Cliente con RFC " + rfc + " dado de baja lógicamente con éxito", null));
    }

    // =========================================================================
    // REACTIVACIÓN (PATCH) - CON REQUEST Y PATH VARIABLE
    // =========================================================================

    @PatchMapping("/reactivar")
    @Operation(summary = "Reactivar cliente por RequestParam", description = "Reactiva al cliente y su cuenta bancaria mediante ?curp=... o ?rfc=... en el request.")
    public ResponseEntity<ApiResponse<ClienteResponse>> reactivarClientePorRequest(
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc) {
        if (curp != null && !curp.isBlank()) {
            ClienteResponse cliente = clienteService.reactivarClientePorCurp(curp);
            return ResponseEntity.ok(ApiResponse.exito("Cliente con CURP " + curp + " reactivado exitosamente", cliente));
        } else if (rfc != null && !rfc.isBlank()) {
            ClienteResponse cliente = clienteService.reactivarClientePorRfc(rfc);
            return ResponseEntity.ok(ApiResponse.exito("Cliente con RFC " + rfc + " reactivado exitosamente", cliente));
        } else {
            throw new ReglaNegocioException("Debe proporcionar ?curp=... o ?rfc=... para reactivar al cliente.");
        }
    }

    @PatchMapping("/curp/{curp}/reactivar")
    @Operation(summary = "Reactivar cliente por CURP (PathVariable)", description = "Reactiva el cliente mediante CURP en la URL.")
    public ResponseEntity<ApiResponse<ClienteResponse>> reactivarClientePorCurpPath(@PathVariable String curp) {
        ClienteResponse cliente = clienteService.reactivarClientePorCurp(curp);
        return ResponseEntity.ok(ApiResponse.exito("Cliente con CURP " + curp + " reactivado exitosamente", cliente));
    }

    @PatchMapping("/rfc/{rfc}/reactivar")
    @Operation(summary = "Reactivar cliente por RFC (PathVariable)", description = "Reactiva el cliente mediante RFC en la URL.")
    public ResponseEntity<ApiResponse<ClienteResponse>> reactivarClientePorRfcPath(@PathVariable String rfc) {
        ClienteResponse cliente = clienteService.reactivarClientePorRfc(rfc);
        return ResponseEntity.ok(ApiResponse.exito("Cliente con RFC " + rfc + " reactivado exitosamente", cliente));
    }

    // =========================================================================
    // PRE-VALIDACIÓN Y DISPONIBILIDAD (GET)
    // =========================================================================

    @GetMapping("/validar")
    @Operation(summary = "Validar disponibilidad de CURP o RFC usando RequestParam", description = "Pre-valida formato y disponibilidad mediante ?curp=... o ?rfc=...")
    public ResponseEntity<ApiResponse<ValidacionIdentificadorResponse>> validarDisponibilidadParam(
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc) {
        if (curp != null && !curp.isBlank()) {
            ValidacionIdentificadorResponse resultado = clienteService.validarDisponibilidadCurp(curp);
            return ResponseEntity.ok(ApiResponse.exito(resultado.getMensaje(), resultado));
        } else if (rfc != null && !rfc.isBlank()) {
            ValidacionIdentificadorResponse resultado = clienteService.validarDisponibilidadRfc(rfc);
            return ResponseEntity.ok(ApiResponse.exito(resultado.getMensaje(), resultado));
        } else {
            throw new ReglaNegocioException("Debe proporcionar ?curp=... o ?rfc=... para validar.");
        }
    }

    @GetMapping("/validar/curp/{curp}")
    @Operation(summary = "Validar formato y disponibilidad de CURP (PathVariable)", description = "Verifica en tiempo real si la CURP tiene formato legal válido RENAPO y si está disponible.")
    public ResponseEntity<ApiResponse<ValidacionIdentificadorResponse>> validarDisponibilidadCurp(@PathVariable String curp) {
        ValidacionIdentificadorResponse resultado = clienteService.validarDisponibilidadCurp(curp);
        return ResponseEntity.ok(ApiResponse.exito(resultado.getMensaje(), resultado));
    }

    @GetMapping("/validar/rfc/{rfc}")
    @Operation(summary = "Validar formato y disponibilidad de RFC (PathVariable)", description = "Verifica en tiempo real si el RFC tiene formato legal válido SAT y si está disponible.")
    public ResponseEntity<ApiResponse<ValidacionIdentificadorResponse>> validarDisponibilidadRfc(@PathVariable String rfc) {
        ValidacionIdentificadorResponse resultado = clienteService.validarDisponibilidadRfc(rfc);
        return ResponseEntity.ok(ApiResponse.exito(resultado.getMensaje(), resultado));
    }

    // =========================================================================
    // ACTUALIZACIÓN PARCIAL DE CONTACTO (PATCH)
    // =========================================================================

    @PatchMapping("/contacto")
    @Operation(summary = "Actualizar contacto de cliente por Request", description = "Actualiza correo y teléfonos identificando al cliente mediante ?curp=... / ?rfc=... o en el cuerpo JSON.")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarContactoPorRequest(
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc,
            @Valid @RequestBody ActualizarContactoRequest request) {
        String curpFinal = (curp != null && !curp.isBlank()) ? curp : request.getCurp();
        String rfcFinal = (rfc != null && !rfc.isBlank()) ? rfc : request.getRfc();

        if (curpFinal != null && !curpFinal.isBlank()) {
            ClienteResponse cliente = clienteService.actualizarContactoPorCurp(curpFinal, request);
            return ResponseEntity.ok(ApiResponse.exito("Datos de contacto actualizados correctamente", cliente));
        } else if (rfcFinal != null && !rfcFinal.isBlank()) {
            ClienteResponse cliente = clienteService.actualizarContactoPorRfc(rfcFinal, request);
            return ResponseEntity.ok(ApiResponse.exito("Datos de contacto actualizados correctamente", cliente));
        } else {
            throw new ReglaNegocioException("Debe proporcionar la CURP o el RFC en el request para actualizar el contacto.");
        }
    }

    @PatchMapping("/curp/{curp}/contacto")
    @Operation(summary = "Actualizar contacto de cliente por CURP (PathVariable)", description = "Permite actualizar rápidamente teléfono y correo buscando por CURP en la URL.")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarContactoPorCurpPath(
            @PathVariable String curp,
            @Valid @RequestBody ActualizarContactoRequest request) {
        ClienteResponse cliente = clienteService.actualizarContactoPorCurp(curp, request);
        return ResponseEntity.ok(ApiResponse.exito("Datos de contacto actualizados correctamente", cliente));
    }

    @PatchMapping("/rfc/{rfc}/contacto")
    @Operation(summary = "Actualizar contacto de cliente por RFC (PathVariable)", description = "Permite actualizar rápidamente teléfono y correo buscando por RFC en la URL.")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarContactoPorRfcPath(
            @PathVariable String rfc,
            @Valid @RequestBody ActualizarContactoRequest request) {
        ClienteResponse cliente = clienteService.actualizarContactoPorRfc(rfc, request);
        return ResponseEntity.ok(ApiResponse.exito("Datos de contacto actualizados correctamente", cliente));
    }

    // =========================================================================
    // CONSULTA DIRECTA DE CUENTA (GET)
    // =========================================================================

    @GetMapping("/cuenta")
    @Operation(summary = "Consultar cuenta bancaria por RequestParam", description = "Obtiene los detalles de la cuenta y saldo mediante ?curp=... o ?rfc=... en el request.")
    public ResponseEntity<ApiResponse<CuentaResponse>> obtenerCuentaPorRequest(
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc) {
        if (curp != null && !curp.isBlank()) {
            CuentaResponse cuenta = clienteService.obtenerCuentaPorCurp(curp);
            return ResponseEntity.ok(ApiResponse.exito("Cuenta consultada exitosamente mediante CURP", cuenta));
        } else if (rfc != null && !rfc.isBlank()) {
            CuentaResponse cuenta = clienteService.obtenerCuentaPorRfc(rfc);
            return ResponseEntity.ok(ApiResponse.exito("Cuenta consultada exitosamente mediante RFC", cuenta));
        } else {
            throw new ReglaNegocioException("Debe proporcionar ?curp=... o ?rfc=... para consultar la cuenta.");
        }
    }

    @GetMapping("/curp/{curp}/cuenta")
    @Operation(summary = "Consultar cuenta bancaria por CURP (PathVariable)", description = "Obtiene los detalles de la cuenta bancaria por CURP en la URL.")
    public ResponseEntity<ApiResponse<CuentaResponse>> obtenerCuentaPorCurpPath(@PathVariable String curp) {
        CuentaResponse cuenta = clienteService.obtenerCuentaPorCurp(curp);
        return ResponseEntity.ok(ApiResponse.exito("Cuenta consultada exitosamente mediante CURP", cuenta));
    }

    @GetMapping("/rfc/{rfc}/cuenta")
    @Operation(summary = "Consultar cuenta bancaria por RFC (PathVariable)", description = "Obtiene los detalles de la cuenta bancaria por RFC en la URL.")
    public ResponseEntity<ApiResponse<CuentaResponse>> obtenerCuentaPorRfcPath(@PathVariable String rfc) {
        CuentaResponse cuenta = clienteService.obtenerCuentaPorRfc(rfc);
        return ResponseEntity.ok(ApiResponse.exito("Cuenta consultada exitosamente mediante RFC", cuenta));
    }
}
