package com.kapm.onboarding_clientes.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.entity.TipoBiometria;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import com.kapm.onboarding_clientes.service.SeguridadBiometriaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de punta a punta de la seguridad: login real, JWT firmado, revocación por logout y matriz de acceso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integración - Autenticación JWT y matriz de acceso")
class AuthSeguridadIT {

    private static final String USUARIO = "it.usuario.jwt";
    private static final String PASSWORD = "Segura#2026";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SeguridadBiometriaService seguridadBiometriaService;
    @Autowired private DatosSeguridadBiometriaRepository seguridadRepository;

    @BeforeEach
    void crearUsuario() {
        seguridadRepository.save(seguridadBiometriaService.crearDatosSeguridad(
                USUARIO, PASSWORD, TipoBiometria.HUELLA_DACTILAR, "plantilla-it"));
    }

    @AfterEach
    void limpiar() {
        seguridadRepository.findByUsername(USUARIO).ifPresent(seguridadRepository::delete);
    }

    private String login(String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", USUARIO, "password", password));
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        JsonNode datos = objectMapper.readTree(json).path("datos");
        return datos.path("accessToken").asText(null);
    }

    @Test
    @DisplayName("Login emite JWT válido; tras logout el mismo token es rechazado (401)")
    void loginYLogout_RevocaToken() throws Exception {
        String token = login(PASSWORD);
        assertThat(token).isNotBlank();

        mockMvc.perform(post("/api/auth/estado").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.username").value(USUARIO));

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/estado").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un nuevo login invalida el token de la sesión anterior")
    void nuevoLogin_InvalidaTokenAnterior() throws Exception {
        String primero = login(PASSWORD);
        String segundo = login(PASSWORD);

        mockMvc.perform(post("/api/auth/estado").header(HttpHeaders.AUTHORIZATION, "Bearer " + primero))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/estado").header(HttpHeaders.AUTHORIZATION, "Bearer " + segundo))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Usuario inexistente y contraseña incorrecta responden igual (sin enumeración de usuarios)")
    void loginFallido_MismaRespuesta() throws Exception {
        String inexistente = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no.existe\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        String incorrecta = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + USUARIO + "\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(inexistente).isEqualTo(incorrecta);
    }

    @Test
    @DisplayName("Token manipulado (firma inválida) es rechazado")
    void tokenManipulado_Rechazado() throws Exception {
        String token = login(PASSWORD);
        String manipulado = token.substring(0, token.length() - 4) + "AAAA";

        mockMvc.perform(post("/api/auth/estado").header(HttpHeaders.AUTHORIZATION, "Bearer " + manipulado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Matriz de acceso: catálogos públicos; clientes/cuentas requieren ADMIN; un JWT de cliente no basta")
    void matrizDeAcceso() throws Exception {
        mockMvc.perform(get("/api/catalogos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/cuentas/activas")).andExpect(status().isUnauthorized());

        String token = login(PASSWORD);
        mockMvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
