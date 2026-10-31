package com.kapm.onboarding_clientes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.config.SecurityConfig;
import com.kapm.onboarding_clientes.dto.request.AplicarPagoRequest;
import com.kapm.onboarding_clientes.dto.request.SolicitarCodigoPagoRequest;
import com.kapm.onboarding_clientes.dto.response.PagoResponse;
import com.kapm.onboarding_clientes.dto.response.SolicitarCodigoPagoResponse;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.service.PagoServicioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PagoController.class)
@Import(SecurityConfig.class)
@DisplayName("Pruebas del Controlador PagoController y Seguridad Spring Security")
class PagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PagoServicioService pagoServicioService;

    // SecurityConfig requiere un JwtDecoder; jwt() de spring-security-test inyecta el token ya decodificado
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static JwtRequestPostProcessor tokenCliente() {
        return jwt().jwt(j -> j.subject("cliente_auth")).authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    // =========================================================================
    // SEGURIDAD: NO PERMITALL EN /api/pagos/**
    // =========================================================================

    @Test
    @DisplayName("Seguridad: Llamada no autenticada a /api/pagos/solicitar-codigo retorna 401 Unauthorized")
    void solicitarCodigo_SinAutenticacion_Retorna401() throws Exception {
        SolicitarCodigoPagoRequest request = SolicitarCodigoPagoRequest.builder()
                .curp("MEHC920824HDFRMN09")
                .numeroCuenta("4819204812")
                .idProducto(55)
                .idServicio(2)
                .telefono("5512345678")
                .monto(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/pagos/solicitar-codigo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Seguridad: Llamada no autenticada a /api/pagos/aplicar retorna 401 Unauthorized")
    void aplicarPago_SinAutenticacion_Retorna401() throws Exception {
        AplicarPagoRequest request = AplicarPagoRequest.builder()
                .curp("MEHC920824HDFRMN09")
                .numeroCuenta("4819204812")
                .idProducto(55)
                .idServicio(2)
                .telefono("5512345678")
                .monto(new BigDecimal("100.00"))
                .codigo("492015")
                .idempotencyKey("KEY-TX-1001")
                .build();

        mockMvc.perform(post("/api/pagos/aplicar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // ENDPOINTS CON USUARIO AUTENTICADO
    // =========================================================================

    @Test
    @DisplayName("Éxito: Solicitar código autenticado retorna 200 y respuesta estructurada")
    void solicitarCodigo_Autenticado_Retorna200() throws Exception {
        SolicitarCodigoPagoRequest request = SolicitarCodigoPagoRequest.builder()
                .curp("MEHC920824HDFRMN09")
                .numeroCuenta("4819204812")
                .idProducto(55)
                .idServicio(2)
                .telefono("5512345678")
                .monto(new BigDecimal("100.00"))
                .build();

        SolicitarCodigoPagoResponse response = SolicitarCodigoPagoResponse.builder()
                .mensaje("Código de verificación enviado al correo registrado")
                .correoEnmascarado("c***@email.com")
                .numeroCuenta("4819204812")
                .idProducto(55)
                .idServicio(2)
                .monto(new BigDecimal("100.00"))
                .expiraEn(LocalDateTime.now().plusMinutes(5))
                .build();

        when(pagoServicioService.solicitarCodigo(any(SolicitarCodigoPagoRequest.class), eq("cliente_auth"))).thenReturn(response);

        mockMvc.perform(post("/api/pagos/solicitar-codigo")
                        .with(tokenCliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.datos.correoEnmascarado").value("c***@email.com"))
                .andExpect(jsonPath("$.datos.numeroCuenta").value("4819204812"));
    }

    @Test
    @DisplayName("Éxito: Aplicar pago autenticado retorna 200 y datos de autorización")
    void aplicarPago_Autenticado_Retorna200() throws Exception {
        AplicarPagoRequest request = AplicarPagoRequest.builder()
                .curp("MEHC920824HDFRMN09")
                .numeroCuenta("4819204812")
                .idProducto(55)
                .idServicio(2)
                .telefono("5512345678")
                .monto(new BigDecimal("100.00"))
                .codigo("492015")
                .idempotencyKey("KEY-TX-1001")
                .build();

        PagoResponse response = PagoResponse.builder()
                .upc("UPC-TX-8819")
                .idempotencyKey("KEY-TX-1001")
                .numeroCuenta("4819204812")
                .idProducto(55)
                .idServicio(2)
                .monto(new BigDecimal("100.00"))
                .estatus(EstatusPago.EXITOSO)
                .numAutorizacion("AUT-88192")
                .idTxProveedor("TX-PR-9912")
                .mensaje("Pago procesado exitosamente")
                .build();

        when(pagoServicioService.aplicarPago(any(AplicarPagoRequest.class), eq("cliente_auth"))).thenReturn(response);

        mockMvc.perform(post("/api/pagos/aplicar")
                        .with(tokenCliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.datos.estatus").value("EXITOSO"))
                .andExpect(jsonPath("$.datos.numAutorizacion").value("AUT-88192"))
                .andExpect(jsonPath("$.datos.upc").value("UPC-TX-8819"));
    }

    @Test
    @DisplayName("Validación: Aplicar pago con datos incompletos (sin código 2FA ni idempotencyKey) retorna 400 Bad Request")
    void aplicarPago_DatosIncompletos_Retorna400() throws Exception {
        AplicarPagoRequest requestInvalido = AplicarPagoRequest.builder()
                .curp("MEHC920824HDFRMN09")
                .numeroCuenta("4819204812")
                .idProducto(55)
                .idServicio(2)
                .monto(new BigDecimal("100.00"))
                .codigo(null) // Código ausente
                .idempotencyKey(null) // Llave ausente
                .build();

        mockMvc.perform(post("/api/pagos/aplicar")
                        .with(tokenCliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Seguridad: un ADMIN no puede ejecutar pagos a nombre de clientes (403)")
    void aplicarPago_ComoAdmin_Retorna403() throws Exception {
        mockMvc.perform(post("/api/pagos/aplicar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Seguridad: un JWT sin rol CLIENTE es rechazado (403)")
    void aplicarPago_TokenSinRolCliente_Retorna403() throws Exception {
        mockMvc.perform(post("/api/pagos/aplicar")
                        .with(jwt().jwt(j -> j.subject("otro")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
