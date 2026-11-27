package com.kapm.onboarding_clientes.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.client.brevo.BrevoEmailClient;
import com.kapm.onboarding_clientes.dto.request.ClienteRegistrationRequest;
import com.kapm.onboarding_clientes.dto.request.DatosLaboralesDTO;
import com.kapm.onboarding_clientes.dto.request.DatosSeguridadDTO;
import com.kapm.onboarding_clientes.dto.request.DomicilioDTO;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.CodigoAccesoRepository;
import com.kapm.onboarding_clientes.service.ClienteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de punta a punta de la seguridad: login en dos pasos con código al correo, JWT firmado,
 * revocación por logout, recuperación de contraseña, perfil/saldo y matriz de acceso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integración - Autenticación JWT con segundo factor y matriz de acceso")
class AuthSeguridadIT {

    private static final String USUARIO = "it.usuario.jwt";
    private static final String PASSWORD = "Segura#2026";
    private static final String CORREO = "it.usuario.jwt@correo.com";
    private static final String CURP = "LOAK950310MDFRPR07";
    private static final Pattern CODIGO_EN_CORREO = Pattern.compile(">(\\d{6})</div>");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ClienteService clienteService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CodigoAccesoRepository codigoAccesoRepository;

    @MockitoBean private BrevoEmailClient brevoEmailClient;

    @BeforeEach
    void crearCliente() {
        // Sin biometría ni empresa: ambos son opcionales en el registro
        clienteService.registrarCliente(ClienteRegistrationRequest.builder()
                .nombre("Karla").apellidoPaterno("López").apellidoMaterno("Arias")
                .fechaNacimiento(LocalDate.of(1995, 3, 10))
                .curp(CURP).rfc("LOAK950310AB1")
                .sexo("FEMENINO").nacionalidad("Mexicana").estadoCivil("SOLTERO")
                .correo(CORREO).telefonoMovil("5512345678")
                .domicilio(DomicilioDTO.builder().calle("Av. Reforma").numeroExterior("10").colonia("Juárez")
                        .municipio("Cuauhtémoc").estado("Ciudad de México").codigoPostal("06600").pais("México").build())
                .laboral(DatosLaboralesDTO.builder().ocupacion("Desarrollador de Software")
                        .ingresoMensual(new BigDecimal("20000.00")).build())
                .seguridad(DatosSeguridadDTO.builder().username(USUARIO).password(PASSWORD).build())
                .saldoInicial(new BigDecimal("1500.00"))
                .build());
    }

    @AfterEach
    void limpiar() {
        clienteRepository.findByCurp(CURP).ifPresent(clienteRepository::delete);
        codigoAccesoRepository.deleteAll();
    }

    private String json(Map<String, String> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    /** Último código enviado por correo (el mock de Brevo recibe el HTML con el código). */
    private String ultimoCodigoEnviado() {
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(brevoEmailClient, atLeastOnce()).enviarCorreo(eq(CORREO), anyString(), html.capture());
        Matcher m = CODIGO_EN_CORREO.matcher(html.getValue());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private String login(String password) throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", USUARIO, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.requiereCodigo").value(true))
                .andExpect(jsonPath("$.datos.accessToken").doesNotExist());

        String respuesta = mockMvc.perform(post("/api/auth/login/verificar").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", USUARIO, "codigo", ultimoCodigoEnviado()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode datos = objectMapper.readTree(respuesta).path("datos");
        return datos.path("accessToken").asText(null);
    }

    @Test
    @DisplayName("Login en dos pasos emite JWT válido; tras logout el mismo token es rechazado (401)")
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
    @DisplayName("Código incorrecto no emite token; el código es de un solo uso")
    void codigoIncorrectoYReuso() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", USUARIO, "password", PASSWORD))))
                .andExpect(status().isOk());
        String codigo = ultimoCodigoEnviado();
        String incorrecto = codigo.equals("000000") ? "111111" : "000000";

        mockMvc.perform(post("/api/auth/login/verificar").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", USUARIO, "codigo", incorrecto))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.datos.accessToken").doesNotExist());

        mockMvc.perform(post("/api/auth/login/verificar").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", USUARIO, "codigo", codigo))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login/verificar").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", USUARIO, "codigo", codigo))))
                .andExpect(status().isBadRequest());
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
    @DisplayName("Usuario inexistente y contraseña incorrecta responden igual y no envían código")
    void loginFallido_MismaRespuesta() throws Exception {
        String inexistente = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no.existe\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        String incorrecta = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + USUARIO + "\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(inexistente.replaceAll("\"timestamp\":\"[^\"]*\"", ""))
                .isEqualTo(incorrecta.replaceAll("\"timestamp\":\"[^\"]*\"", ""));
        verify(brevoEmailClient, never()).enviarCorreo(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Recuperación: el código del correo permite fijar una nueva contraseña y revoca la sesión abierta")
    void recuperarPassword() throws Exception {
        String token = login(PASSWORD);
        clearInvocations(brevoEmailClient);

        mockMvc.perform(post("/api/auth/recuperar/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("identificador", CORREO))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/recuperar/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("identificador", USUARIO, "codigo", ultimoCodigoEnviado(),
                                "nuevaPassword", "OtraSegura#2026"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/estado").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", USUARIO, "password", PASSWORD))))
                .andExpect(status().isBadRequest());
        assertThat(login("OtraSegura#2026")).isNotBlank();
    }

    @Test
    @DisplayName("Recuperación con un identificador inexistente responde igual y no envía correo")
    void recuperarPassword_IdentificadorInexistente() throws Exception {
        mockMvc.perform(post("/api/auth/recuperar/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("identificador", "nadie@correo.com"))))
                .andExpect(status().isOk());
        verify(brevoEmailClient, never()).enviarCorreo(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Perfil y saldo del titular del token; cambio de contraseña exige la actual")
    void perfilSaldoYPassword() throws Exception {
        String token = login(PASSWORD);
        String bearer = "Bearer " + token;

        mockMvc.perform(post("/api/perfil").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.username").value(USUARIO))
                .andExpect(jsonPath("$.datos.correo").value(CORREO))
                .andExpect(jsonPath("$.datos.empresa").doesNotExist());

        mockMvc.perform(post("/api/perfil/saldo").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.saldoDisponible").value(1500.00));

        mockMvc.perform(post("/api/perfil/password").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("passwordActual", "incorrecta", "nuevaPassword", "OtraSegura#2026"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/perfil/password").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("passwordActual", PASSWORD, "nuevaPassword", "OtraSegura#2026"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/perfil")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Perfil editable: cambia datos personales y de contacto; CURP/RFC no se aceptan y el correo exige contraseña")
    void actualizarPerfil() throws Exception {
        String bearer = "Bearer " + login(PASSWORD);
        String perfil = mockMvc.perform(post("/api/perfil").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.datosEditables.sexo").value("FEMENINO"))
                .andReturn().getResponse().getContentAsString();
        com.fasterxml.jackson.databind.node.ObjectNode editables =
                (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(perfil).path("datos").path("datosEditables");

        // Las apps envían el ingreso como texto con 2 decimales (readTree lo convierte en 20000.0)
        editables.put("ingresoMensual", "20000.00");
        editables.put("nombre", "Karina").put("telefonoMovil", "5598765432").put("empresa", "Mi Empresa S.A.");
        mockMvc.perform(post("/api/perfil/actualizar").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(editables.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.datosEditables.nombre").value("Karina"))
                .andExpect(jsonPath("$.datos.telefonoMovil").value("5598765432"))
                .andExpect(jsonPath("$.datos.empresa").value("Mi Empresa S.A."))
                .andExpect(jsonPath("$.datos.curp").value(CURP));

        // CURP y RFC no forman parte del contrato
        mockMvc.perform(post("/api/perfil/actualizar").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(editables.deepCopy().put("curp", "XXXX000000HDFXXX00").toString()))
                .andExpect(status().isBadRequest());

        // Menor de edad
        mockMvc.perform(post("/api/perfil/actualizar").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(editables.deepCopy().put("fechaNacimiento", "2015-01-01").toString()))
                .andExpect(status().isBadRequest());

        // Cambiar el correo sin contraseña, y luego con ella
        editables.put("correo", "nuevo.correo@correo.com");
        mockMvc.perform(post("/api/perfil/actualizar").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(editables.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/perfil/actualizar").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(editables.deepCopy().put("passwordActual", PASSWORD).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.correo").value("nuevo.correo@correo.com"));
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
