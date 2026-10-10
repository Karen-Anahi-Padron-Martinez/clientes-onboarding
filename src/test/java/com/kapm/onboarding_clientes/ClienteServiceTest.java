package com.kapm.onboarding_clientes;

import com.kapm.onboarding_clientes.dto.request.*;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.ValidacionIdentificadorResponse;
import com.kapm.onboarding_clientes.exception.CurpDuplicadaException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.model.EstatusCuenta;
import com.kapm.onboarding_clientes.model.TipoBiometria;
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
                        .username("user_" + curp.substring(0, 8).toLowerCase())
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
                .nombre("Juan Actualizado")
                .segundoNombre("Carlos")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("Gómez")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .curp("PEGJ900515HDFRMN90")
                .rfc("PEGJ9005151A9")
                .sexo("MASCULINO")
                .nacionalidad("Mexicana")
                .estadoCivil("CASADO")
                .correo("nuevo.correo@email.com")
                .telefonoMovil("5599887766")
                .domicilio(reg.getDomicilio())
                .laboral(reg.getLaboral())
                .build();

        ClienteResponse actualizado = clienteService.actualizarClientePorCurp("PEGJ900515HDFRMN90", updateReq);

        assertEquals("Juan Actualizado", actualizado.getNombre());
        assertEquals("nuevo.correo@email.com", actualizado.getCorreo());
        assertEquals("5599887766", actualizado.getTelefonoMovil());
    }

    @Test
    @DisplayName("Debe rechazar actualización con CURP inválida")
    void debeRechazarActualizacionCurpInvalida() {
        ClienteUpdateRequest updateReq = ClienteUpdateRequest.builder()
                .nombre("Test")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .sexo("MASCULINO")
                .nacionalidad("Mexicana")
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
}
