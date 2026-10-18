package com.kapm.onboarding_clientes;

import com.kapm.onboarding_clientes.dto.request.*;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.PageResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionIdentificadorResponse;
import com.kapm.onboarding_clientes.exception.CurpDuplicadaException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.entity.EstatusCuenta;
import com.kapm.onboarding_clientes.entity.TipoBiometria;
import com.kapm.onboarding_clientes.service.ClienteService;
import com.kapm.onboarding_clientes.service.CuentaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClienteServiceTest {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CuentaService cuentaService;

    private ClienteRegistrationRequest crearRequestValido(String curp, String rfc, String correo) {
        return ClienteRegistrationRequest.builder()
                .nombre("Juan")
                .segundoNombre("Carlos")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("Gómez")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .curp(curp)
                .rfc(rfc)
                .sexo("MASCULINO")
                .nacionalidad("Mexicana")
                .estadoCivil("SOLTERO")
                .correo(correo)
                .telefonoMovil("5512345678")
                .telefonoAlternativo("5587654321")
                .domicilio(DomicilioDTO.builder()
                        .calle("Av. Reforma")
                        .numeroExterior("123")
                        .numeroInterior("A-1")
                        .colonia("Juárez")
                        .municipio("Cuauhtémoc")
                        .estado("Ciudad de México")
                        .codigoPostal("06600")
                        .pais("México")
                        .build())
                .laboral(DatosLaboralesDTO.builder()
                        .ocupacion("Desarrollador de Software")
                        .empresa("Tecnología Financiera SA de CV")
                        .ingresoMensual(new BigDecimal("35000.00"))
                        .build())
                .seguridad(DatosSeguridadDTO.builder()
                        .username("user_" + curp.toLowerCase())
                        .password("SecurePass2026*")
                        .tipoBiometria(TipoBiometria.HUELLA_DACTILAR)
                        .datosBiometricos("SAMPLE-FINGERPRINT-PATTERN-DATA-999")
                        .build())
                .saldoInicial(new BigDecimal("2500.00"))
                .build();
    }

    @Test
    @DisplayName("Debe registrar cliente exitosamente sin exponer ID en la respuesta y crear cuenta bancaria")
    void debeRegistrarClienteExitosamente() {
        ClienteRegistrationRequest request = crearRequestValido("PEGJ900515HDFRMN01", "PEGJ9005151A2", "juan.perez@email.com");
        
        ClienteResponse response = clienteService.registrarCliente(request);

        // Requerimiento: el response del POST no debe mandar el ID
        assertNull(response.getId(), "El response del POST de cliente no debe mandar el ID");
        assertEquals("Juan", response.getNombre());
        assertEquals("PEGJ900515HDFRMN01", response.getCurp());
        assertTrue(response.getActivo());
        assertTrue(response.getBiometriaRegistrada(), "Debe marcar biometriaRegistrada en true sin exponer el hash sensible");
        
        // Verificar cuenta bancaria autogenerada (tampoco expone ID de cuenta)
        assertNotNull(response.getCuenta());
        assertNull(response.getCuenta().getId(), "El response tampoco expone el ID de la cuenta");
        assertNotNull(response.getCuenta().getNumeroCuenta());
        assertEquals(10, response.getCuenta().getNumeroCuenta().length());
        assertEquals(new BigDecimal("2500.00"), response.getCuenta().getSaldo());
        assertEquals(EstatusCuenta.ACTIVA, response.getCuenta().getEstatus());
    }

    @Test
    @DisplayName("Debe actualizar cliente por CURP exitosamente")
    void debeActualizarClientePorCurp() {
        ClienteRegistrationRequest reg = crearRequestValido("PEGJ900515HDFRMN90", "PEGJ9005151A9", "actualizar.curp@email.com");
        clienteService.registrarCliente(reg);

        ClienteUpdateRequest updateReq = ClienteUpdateRequest.builder()
                .curp("PEGJ900515HDFRMN90")
                .rfc("PEGJ9005151A9")
                .sexo("MASCULINO")
                .estadoCivil("CASADO")
                .correo("nuevo.correo@email.com")
                .telefonoMovil("5599887766")
                .domicilio(reg.getDomicilio())
                .laboral(reg.getLaboral())
                .build();

        ClienteResponse actualizado = clienteService.actualizarClientePorCurp("PEGJ900515HDFRMN90", updateReq);

        // Los campos de identidad deben permanecer inalterados
        assertEquals("Juan", actualizado.getNombre());
        assertEquals("Carlos", actualizado.getSegundoNombre());
        assertEquals("Pérez", actualizado.getApellidoPaterno());
        assertEquals("Gómez", actualizado.getApellidoMaterno());
        assertEquals(LocalDate.of(1990, 5, 15), actualizado.getFechaNacimiento());
        assertEquals("PEGJ900515HDFRMN90", actualizado.getCurp());
        assertEquals("PEGJ9005151A9", actualizado.getRfc());
        assertEquals("Mexicana", actualizado.getNacionalidad());

        // Campos modificados
        assertEquals("nuevo.correo@email.com", actualizado.getCorreo());
        assertEquals("5599887766", actualizado.getTelefonoMovil());
    }

    @Test
    @DisplayName("Debe rechazar actualización con CURP inválida")
    void debeRechazarActualizacionCurpInvalida() {
        ClienteUpdateRequest updateReq = ClienteUpdateRequest.builder()
                .sexo("MASCULINO")
                .estadoCivil("SOLTERO")
                .correo("test@email.com")
                .telefonoMovil("5512345678")
                .domicilio(DomicilioDTO.builder().calle("Calle").numeroExterior("1").colonia("Col").municipio("Mun").estado("Ciudad de México").codigoPostal("01000").pais("México").build())
                .laboral(DatosLaboralesDTO.builder().ocupacion("Empleado Sector Privado").empresa("Emp").ingresoMensual(BigDecimal.TEN).build())
                .build();

        assertThrows(ReglaNegocioException.class, () -> {
            clienteService.actualizarClientePorCurp("CURP_INVALIDA", updateReq);
        });
    }

    @Test
    @DisplayName("Debe validar disponibilidad de CURP y RFC correctamente")
    void debeValidarDisponibilidadCurpYRfc() {
        String curp = "PEGJ900515HDFRMN88";
        String rfc = "PEGJ9005151A8";

        ValidacionIdentificadorResponse valCurpAntes = clienteService.validarDisponibilidadCurp(curp);
        assertTrue(valCurpAntes.isFormatoValido());
        assertTrue(valCurpAntes.isDisponible());

        clienteService.registrarCliente(crearRequestValido(curp, rfc, "disponibilidad@email.com"));

        ValidacionIdentificadorResponse valCurpDespues = clienteService.validarDisponibilidadCurp(curp);
        assertTrue(valCurpDespues.isFormatoValido());
        assertFalse(valCurpDespues.isDisponible());

        ValidacionIdentificadorResponse valRfcDespues = clienteService.validarDisponibilidadRfc(rfc);
        assertTrue(valRfcDespues.isFormatoValido());
        assertFalse(valRfcDespues.isDisponible());
    }

    @Test
    @DisplayName("Debe rechazar registro de cliente menor de edad (< 18 años)")
    void debeRechazarMenorDeEdad() {
        ClienteRegistrationRequest request = crearRequestValido("PEGJ100515HDFRMN02", "PEGJ1005151A3", "menor@email.com");
        request.setFechaNacimiento(LocalDate.now().minusYears(17)); // 17 años

        ReglaNegocioException exception = assertThrows(ReglaNegocioException.class, () -> {
            clienteService.registrarCliente(request);
        });

        assertTrue(exception.getMessage().contains("mayor de edad"));
    }

    @Test
    @DisplayName("Debe rechazar registro con CURP duplicada")
    void debeRechazarCurpDuplicada() {
        ClienteRegistrationRequest request1 = crearRequestValido("PEGJ900515HDFRMN03", "PEGJ9005151A4", "juan1@email.com");
        clienteService.registrarCliente(request1);

        ClienteRegistrationRequest request2 = crearRequestValido("PEGJ900515HDFRMN03", "PEGJ9005151A5", "juan2@email.com");

        assertThrows(CurpDuplicadaException.class, () -> {
            clienteService.registrarCliente(request2);
        });
    }

    @Test
    @DisplayName("Debe ejecutar baja lógica de cliente por CURP y reactivarlo")
    void debeRealizarBajaLogicaYReactivacionPorCurp() {
        String curp = "PEGJ900515HDFRMN04";
        ClienteRegistrationRequest request = crearRequestValido(curp, "PEGJ9005151A6", "baja@email.com");
        clienteService.registrarCliente(request);

        clienteService.desactivarClientePorCurp(curp);

        ClienteResponse inactivo = clienteService.obtenerClientePorCurp(curp);
        assertFalse(inactivo.getActivo());
        assertEquals(EstatusCuenta.INACTIVA, inactivo.getCuenta().getEstatus());

        ClienteResponse reactivado = clienteService.reactivarClientePorCurp(curp);
        assertTrue(reactivado.getActivo());
        assertEquals(EstatusCuenta.ACTIVA, reactivado.getCuenta().getEstatus());
    }

    @Test
    @DisplayName("Debe actualizar cliente mediante ActualizarPorCurpRequest sin modificar campos inmutables")
    void debeActualizarClienteMedianteActualizarPorCurpRequest() {
        String curp = "PEGJ900515HDFRMN77";
        String rfc = "PEGJ9005151A7";
        ClienteRegistrationRequest reg = crearRequestValido(curp, rfc, "dto.curp@email.com");
        clienteService.registrarCliente(reg);

        ActualizarPorCurpRequest actReq = ActualizarPorCurpRequest.builder()
                .curp(curp)
                .sexo("MASCULINO")
                .estadoCivil("CASADO")
                .correo("nuevo.dto.curp@email.com")
                .telefonoMovil("5511223344")
                .domicilio(reg.getDomicilio())
                .laboral(reg.getLaboral())
                .build();

        ClienteResponse resp = clienteService.actualizarClientePorCurp(actReq);
        // Los 7 campos inmutables se mantienen intactos en la respuesta y en BD
        assertEquals("Juan", resp.getNombre());
        assertEquals("Carlos", resp.getSegundoNombre());
        assertEquals("Pérez", resp.getApellidoPaterno());
        assertEquals("Gómez", resp.getApellidoMaterno());
        assertEquals(LocalDate.of(1990, 5, 15), resp.getFechaNacimiento());
        assertEquals(curp, resp.getCurp());
        assertEquals(rfc, resp.getRfc());
        assertEquals("Mexicana", resp.getNacionalidad());

        // Campos modificables actualizados
        assertEquals("nuevo.dto.curp@email.com", resp.getCorreo());
        assertEquals("5511223344", resp.getTelefonoMovil());
        assertEquals("CASADO", resp.getEstadoCivil());
    }

    @Test
    @DisplayName("Debe actualizar cliente mediante ActualizarPorRfcRequest sin modificar campos inmutables")
    void debeActualizarClienteMedianteActualizarPorRfcRequest() {
        String curp = "PEGJ900515HDFRMN66";
        String rfc = "PEGJ9005151A6";
        ClienteRegistrationRequest reg = crearRequestValido(curp, rfc, "dto.rfc@email.com");
        clienteService.registrarCliente(reg);

        ActualizarPorRfcRequest actReq = ActualizarPorRfcRequest.builder()
                .rfc(rfc)
                .sexo("MASCULINO")
                .estadoCivil("SOLTERO")
                .correo("nuevo.dto.rfc@email.com")
                .telefonoMovil("5599001122")
                .domicilio(reg.getDomicilio())
                .laboral(reg.getLaboral())
                .build();

        ClienteResponse resp = clienteService.actualizarClientePorRfc(actReq);
        // Los 7 campos inmutables se mantienen intactos
        assertEquals("Juan", resp.getNombre());
        assertEquals("Carlos", resp.getSegundoNombre());
        assertEquals("Pérez", resp.getApellidoPaterno());
        assertEquals("Gómez", resp.getApellidoMaterno());
        assertEquals(LocalDate.of(1990, 5, 15), resp.getFechaNacimiento());
        assertEquals(curp, resp.getCurp());
        assertEquals(rfc, resp.getRfc());
        assertEquals("Mexicana", resp.getNacionalidad());

        // Campos modificables actualizados
        assertEquals("nuevo.dto.rfc@email.com", resp.getCorreo());
        assertEquals("5599001122", resp.getTelefonoMovil());
    }

    @Test
    @DisplayName("Debe rechazar actualización si el nuevo correo pertenece a otro cliente (409)")
    void debeRechazarActualizacionConCorreoDuplicado() {
        String curp1 = "PEGJ900515HDFRMN81";
        String rfc1 = "PEGJ900515181";
        clienteService.registrarCliente(crearRequestValido(curp1, rfc1, "cliente1@email.com"));

        String curp2 = "PEGJ900515HDFRMN82";
        String rfc2 = "PEGJ900515182";
        ClienteRegistrationRequest reg2 = crearRequestValido(curp2, rfc2, "cliente2@email.com");
        clienteService.registrarCliente(reg2);

        ActualizarPorCurpRequest actReq = ActualizarPorCurpRequest.builder()
                .curp(curp2)
                .sexo("MASCULINO")
                .estadoCivil("SOLTERO")
                .correo("cliente1@email.com") // Correo del cliente 1
                .telefonoMovil("5599887766")
                .domicilio(reg2.getDomicilio())
                .laboral(reg2.getLaboral())
                .build();

        assertThrows(com.kapm.onboarding_clientes.exception.CorreoDuplicadoException.class, () -> {
            clienteService.actualizarClientePorCurp(actReq);
        });
    }

    @Test
    @DisplayName("Debe rechazar actualización si el cliente no existe (404)")
    void debeRechazarActualizacionClienteInexistente() {
        ActualizarPorCurpRequest actReq = ActualizarPorCurpRequest.builder()
                .curp("XXXX900515HDFRMN99")
                .sexo("MASCULINO")
                .estadoCivil("SOLTERO")
                .correo("inexistente@email.com")
                .telefonoMovil("5599887766")
                .domicilio(DomicilioDTO.builder()
                        .calle("Calle").numeroExterior("1").colonia("Colonia")
                        .municipio("Municipio").estado("Ciudad de México").codigoPostal("06600").pais("México")
                        .build())
                .laboral(DatosLaboralesDTO.builder()
                        .ocupacion("Empleado").empresa("Empresa").ingresoMensual(new java.math.BigDecimal("15000.00"))
                        .build())
                .build();

        assertThrows(com.kapm.onboarding_clientes.exception.ClienteNoEncontradoException.class, () -> {
            clienteService.actualizarClientePorCurp(actReq);
        });
    }

    @Test
    @DisplayName("Debe buscar clientes mediante consulta dinámica con múltiples filtros combinados")
    void debeBuscarClientesPorFiltrosCombinados() {
        String curp = "PEGJ900515HDFRMN55";
        String rfc = "PEGJ9005151A5";
        ClienteRegistrationRequest reg = crearRequestValido(curp, rfc, "busqueda.combinada@email.com");
        clienteService.registrarCliente(reg);

        BuscarClienteRequest request = BuscarClienteRequest.builder()
                .curp(curp)
                .rfc(rfc)
                .nombre("Juan")
                .activo(true)
                .build();

        PageResponse<ClienteResponse> resultados = clienteService.buscarClientes(request);
        assertNotNull(resultados);
        assertNotNull(resultados.getContenido());
        assertFalse(resultados.getContenido().isEmpty());
        assertEquals(curp, resultados.getContenido().get(0).getCurp());
        assertEquals(rfc, resultados.getContenido().get(0).getRfc());
        assertTrue(resultados.getContenido().get(0).getActivo());
        assertEquals(0, resultados.getPaginaActual());
        assertTrue(resultados.getTotalElementos() >= 1);
    }

    @Test
    @DisplayName("Debe lanzar ClienteNoEncontradoException cuando la búsqueda dinámica no encuentra coincidencias")
    void debeLanzarExcepcionCuandoNoHayResultados() {
        BuscarClienteRequest request = BuscarClienteRequest.builder()
                .curp("XXXX990101HDFRMN99")
                .build();

        assertThrows(com.kapm.onboarding_clientes.exception.ClienteNoEncontradoException.class, () -> {
            clienteService.buscarClientes(request);
        });
    }

    @Test
    @DisplayName("Debe dar de baja y reactivar cliente usando ClienteIdentificadorRequest")
    void debeDesactivarYReactivarConClienteIdentificadorRequest() {
        String curp = "PEGJ900515HDFRMN44";
        String rfc = "PEGJ9005151A4";
        ClienteRegistrationRequest reg = crearRequestValido(curp, rfc, "baja.identificador@email.com");
        clienteService.registrarCliente(reg);

        ClienteIdentificadorRequest idReq = ClienteIdentificadorRequest.builder()
                .curp(curp)
                .build();

        clienteService.desactivarCliente(idReq);
        ClienteResponse inactivo = clienteService.obtenerClientePorCurp(curp);
        assertFalse(inactivo.getActivo());

        ClienteResponse reactivado = clienteService.reactivarCliente(idReq);
        assertTrue(reactivado.getActivo());
    }

    @Test
    @DisplayName("Debe paginar correctamente los resultados de búsqueda")
    void debePaginarResultadosDeBusqueda() {
        String curp = "PEGJ900515HDFRMN33";
        String rfc = "PEGJ9005151A3";
        ClienteRegistrationRequest reg = crearRequestValido(curp, rfc, "paginacion.test@email.com");
        clienteService.registrarCliente(reg);

        BuscarClienteRequest request = BuscarClienteRequest.builder()
                .curp(curp)
                .pagina(0)
                .tamanio(5)
                .build();

        PageResponse<ClienteResponse> page = clienteService.buscarClientes(request);
        assertNotNull(page);
        assertEquals(0, page.getPaginaActual());
        assertEquals(5, page.getTamanoPagina());
        assertTrue(page.getTotalElementos() >= 1);
        assertTrue(page.isPrimera());
    }
}
