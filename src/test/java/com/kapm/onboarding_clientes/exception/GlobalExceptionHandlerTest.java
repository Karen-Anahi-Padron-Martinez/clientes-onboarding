package com.kapm.onboarding_clientes.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapm.onboarding_clientes.dto.request.ClienteIdentificadorRequest;
import com.kapm.onboarding_clientes.service.ClienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias y de integración para GlobalExceptionHandler.
 * Valida el formato estricto de respuesta de error:
 * ÚNICAMENTE dos campos {"codigo": <int>, "mensaje": "<string>"}.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClienteService clienteService;

    @Test
    @DisplayName("1. Validación fallida (400) - Devuelve código 1 y exactamente 2 campos")
    void testValidacionFallidaRetornaCodigo1YDosCampos() throws Exception {
        // Enviar JSON vacío al registro de clientes para activar Bean Validation
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.DATOS_INVALIDOS.getCodigo())))
                .andExpect(jsonPath("$.mensaje", containsString("Datos no válidos")))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.erroresValidacion").doesNotExist());
    }

    @Test
    @DisplayName("2. Recurso no encontrado (404) - Devuelve código 3 y exactamente 2 campos")
    void testRecursoNoEncontradoRetornaCodigo3YDosCampos() throws Exception {
        when(clienteService.obtenerCuenta(any(ClienteIdentificadorRequest.class)))
                .thenThrow(new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO));

        ClienteIdentificadorRequest request = ClienteIdentificadorRequest.builder()
                .curp("PAMK050524MGTDRRA1")
                .build();

        mockMvc.perform(post("/api/clientes/cuenta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.CLIENTE_NO_ENCONTRADO.getCodigo())))
                .andExpect(jsonPath("$.mensaje", is(ErrorMessage.CLIENTE_NO_ENCONTRADO.getMensaje())))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("3. Recurso duplicado (409) - Devuelve código 4 y exactamente 2 campos")
    void testRecursoDuplicadoRetornaCodigo4YDosCampos() throws Exception {
        when(clienteService.registrarCliente(any()))
                .thenThrow(new CurpDuplicadaException(ErrorMessage.CURP_DUPLICADA, "PAMK050524MGTDRRA1"));

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"curp\":\"PAMK050524MGTDRRA1\",\"rfc\":\"PAMK0505243A8\",\"nombre\":\"Karen\",\"apellidoPaterno\":\"Padron\",\"apellidoMaterno\":\"Martinez\",\"fechaNacimiento\":\"1995-05-24\",\"sexo\":\"FEMENINO\",\"nacionalidad\":\"Mexicana\",\"estadoCivil\":\"SOLTERO\",\"correo\":\"karen@example.com\",\"telefonoMovil\":\"5512345678\",\"domicilio\":{\"calle\":\"Principal\",\"numeroExterior\":\"100\",\"colonia\":\"Centro\",\"municipio\":\"Monterrey\",\"estado\":\"Nuevo León\",\"codigoPostal\":\"64000\",\"pais\":\"México\"},\"laboral\":{\"ocupacion\":\"Ingeniera\",\"empresa\":\"Fintech\",\"ingresoMensual\":45000.00}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.CURP_DUPLICADA.getCodigo())))
                .andExpect(jsonPath("$.mensaje", containsString(ErrorMessage.CURP_DUPLICADA.getMensaje())))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("4. JSON malformado (400) - Devuelve código 2 y exactamente 2 campos")
    void testJsonMalformadoRetornaCodigo2YDosCampos() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ json_invalido_sin_comillas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.JSON_MALFORMADO.getCodigo())))
                .andExpect(jsonPath("$.mensaje", is(ErrorMessage.JSON_MALFORMADO.getMensaje())))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("5. Método no permitido (405) - Devuelve código 7 y exactamente 2 campos")
    void testMetodoNoPermitidoRetornaCodigo7YDosCampos() throws Exception {
        // Enviar PATCH al endpoint de búsqueda que solo soporta POST
        mockMvc.perform(patch("/api/clientes/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.METODO_NO_PERMITIDO.getCodigo())))
                .andExpect(jsonPath("$.mensaje", is(ErrorMessage.METODO_NO_PERMITIDO.getMensaje())))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("6. Media type no soportado (415) - Devuelve código 8 y exactamente 2 campos")
    void testMediaTypeNoSoportadoRetornaCodigo8YDosCampos() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml></xml>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.MEDIA_TYPE_NO_SOPORTADO.getCodigo())))
                .andExpect(jsonPath("$.mensaje", is(ErrorMessage.MEDIA_TYPE_NO_SOPORTADO.getMensaje())))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("7. Conflicto de integridad (409) - Devuelve código 6 y exactamente 2 campos")
    void testConflictoIntegridadRetornaCodigo6YDosCampos() throws Exception {
        when(clienteService.obtenerTodosLosClientes())
                .thenThrow(new DataIntegrityViolationException("violacion de constraint de bd"));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.CONFLICTO_INTEGRIDAD.getCodigo())))
                .andExpect(jsonPath("$.mensaje", is(ErrorMessage.CONFLICTO_INTEGRIDAD.getMensaje())))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("8. Error interno no controlado (500) - Devuelve código 99 y oculta traza")
    void testErrorInternoRetornaCodigo99YDosCampos() throws Exception {
        when(clienteService.obtenerTodosLosClientes())
                .thenThrow(new RuntimeException("Error inesperado en servicio"));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.ERROR_INTERNO.getCodigo())))
                .andExpect(jsonPath("$.mensaje", is(ErrorMessage.ERROR_INTERNO.getMensaje())))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("9. Intento de modificar campo inmutable 'nombre' (400) - Rechaza y devuelve código 2")
    void testIntentoModificarNombreRechazado() throws Exception {
        // Enviar JSON de actualización que incluye el campo prohibido 'nombre'
        String jsonConNombre = "{\"curp\":\"MEHC920824HDFRMN09\",\"nombre\":\"IntentoCambioNombre\",\"sexo\":\"MASCULINO\",\"estadoCivil\":\"CASADO\",\"correo\":\"nuevo@email.com\",\"telefonoMovil\":\"5512345678\",\"domicilio\":{\"calle\":\"Insurgentes\",\"numeroExterior\":\"100\",\"colonia\":\"Roma\",\"municipio\":\"Cuauhtémoc\",\"estado\":\"Ciudad de México\",\"codigoPostal\":\"06700\",\"pais\":\"México\"},\"laboral\":{\"ocupacion\":\"Abogado\",\"empresa\":\"Bufete\",\"ingresoMensual\":30000.00}}";

        mockMvc.perform(put("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonConNombre))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.CAMPO_NO_MODIFICABLE.getCodigo())))
                .andExpect(jsonPath("$.mensaje", is(ErrorMessage.CAMPO_NO_MODIFICABLE.getMensaje())))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("10. Intento de modificar campo inmutable 'fechaNacimiento' (400) - Rechaza y devuelve código 2")
    void testIntentoModificarFechaNacimientoRechazado() throws Exception {
        String jsonConFecha = "{\"curp\":\"MEHC920824HDFRMN09\",\"fechaNacimiento\":\"2000-01-01\",\"sexo\":\"MASCULINO\",\"estadoCivil\":\"CASADO\",\"correo\":\"nuevo@email.com\",\"telefonoMovil\":\"5512345678\",\"domicilio\":{\"calle\":\"Insurgentes\",\"numeroExterior\":\"100\",\"colonia\":\"Roma\",\"municipio\":\"Cuauhtémoc\",\"estado\":\"Ciudad de México\",\"codigoPostal\":\"06700\",\"pais\":\"México\"},\"laboral\":{\"ocupacion\":\"Abogado\",\"empresa\":\"Bufete\",\"ingresoMensual\":30000.00}}";

        mockMvc.perform(put("/api/clientes/curp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonConFecha))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.CAMPO_NO_MODIFICABLE.getCodigo())))
                .andExpect(jsonPath("$.mensaje", is(ErrorMessage.CAMPO_NO_MODIFICABLE.getMensaje())))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("11. Actualización de cliente inexistente (404) - Devuelve código 3 y exactamente 2 campos")
    void testActualizacionClienteInexistenteRetorna404() throws Exception {
        when(clienteService.actualizarCliente(any()))
                .thenThrow(new ClienteNoEncontradoException(ErrorMessage.CLIENTE_NO_ENCONTRADO));

        String jsonValido = "{\"curp\":\"MEHC920824HDFRMN09\",\"sexo\":\"MASCULINO\",\"estadoCivil\":\"CASADO\",\"correo\":\"nuevo@email.com\",\"telefonoMovil\":\"5512345678\",\"domicilio\":{\"calle\":\"Insurgentes\",\"numeroExterior\":\"100\",\"colonia\":\"Roma\",\"municipio\":\"Cuauhtémoc\",\"estado\":\"Ciudad de México\",\"codigoPostal\":\"06700\",\"pais\":\"México\"},\"laboral\":{\"ocupacion\":\"Abogado\",\"empresa\":\"Bufete\",\"ingresoMensual\":30000.00}}";

        mockMvc.perform(put("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.CLIENTE_NO_ENCONTRADO.getCodigo())))
                .andExpect(jsonPath("$.mensaje", is(ErrorMessage.CLIENTE_NO_ENCONTRADO.getMensaje())))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("12. Actualización con correo duplicado de otro cliente (409) - Devuelve código 4")
    void testActualizacionCorreoDuplicadoRetorna409() throws Exception {
        when(clienteService.actualizarCliente(any()))
                .thenThrow(new CorreoDuplicadoException(ErrorMessage.CORREO_DUPLICADO, "existente@email.com"));

        String jsonValido = "{\"curp\":\"MEHC920824HDFRMN09\",\"sexo\":\"MASCULINO\",\"estadoCivil\":\"CASADO\",\"correo\":\"existente@email.com\",\"telefonoMovil\":\"5512345678\",\"domicilio\":{\"calle\":\"Insurgentes\",\"numeroExterior\":\"100\",\"colonia\":\"Roma\",\"municipio\":\"Cuauhtémoc\",\"estado\":\"Ciudad de México\",\"codigoPostal\":\"06700\",\"pais\":\"México\"},\"laboral\":{\"ocupacion\":\"Abogado\",\"empresa\":\"Bufete\",\"ingresoMensual\":30000.00}}";

        mockMvc.perform(put("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is(ErrorMessage.CORREO_DUPLICADO.getCodigo())))
                .andExpect(jsonPath("$.mensaje", containsString(ErrorMessage.CORREO_DUPLICADO.getMensaje())))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.ruta").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}
