package com.kapm.onboarding_clientes;

import com.kapm.onboarding_clientes.dto.request.*;
import com.kapm.onboarding_clientes.dto.response.ClienteResponse;
import com.kapm.onboarding_clientes.dto.response.SaldoResponse;
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
import java.util.List;

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
    @DisplayName("Debe registrar cliente exitosamente y crear cuenta bancaria única con saldo asignado")
    void debeRegistrarClienteExitosamente() {
        ClienteRegistrationRequest request = crearRequestValido("PEGJ900515HDFRMN01", "PEGJ9005151A2", "juan.perez@email.com");
        
        ClienteResponse response = clienteService.registrarCliente(request);

        assertNotNull(response.getId());
        assertEquals("Juan", response.getNombre());
        assertEquals("PEGJ900515HDFRMN01", response.getCurp());
        assertTrue(response.getActivo());
        
        // Verificar cuenta bancaria autogenerada
        assertNotNull(response.getCuenta());
        assertNotNull(response.getCuenta().getNumeroCuenta());
        assertEquals(10, response.getCuenta().getNumeroCuenta().length());
        assertEquals(new BigDecimal("2500.00"), response.getCuenta().getSaldo());
        assertEquals(EstatusCuenta.ACTIVA, response.getCuenta().getEstatus());
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
    @DisplayName("Debe ejecutar baja lógica de cliente y desactivar su cuenta")
    void debeRealizarBajaLogica() {
        ClienteRegistrationRequest request = crearRequestValido("PEGJ900515HDFRMN04", "PEGJ9005151A6", "baja@email.com");
        ClienteResponse creado = clienteService.registrarCliente(request);

        clienteService.desactivarCliente(creado.getId());

        ClienteResponse inactivo = clienteService.obtenerClientePorId(creado.getId());
        assertFalse(inactivo.getActivo());
        assertEquals(EstatusCuenta.INACTIVA, inactivo.getCuenta().getEstatus());
    }
}
