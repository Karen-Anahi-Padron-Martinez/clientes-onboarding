package com.kapm.onboarding_clientes.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.kapm.onboarding_clientes.entity.DatosSeguridadBiometria;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Configuración: beans con lógica condicional")
class ConfiguracionUnitTest {

    @Test
    @DisplayName("RestClient de Brevo y PuntoRed se construye con o sin URL base")
    void restClients() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        for (String url : new String[]{null, " ", "https://api.ejemplo.com"}) {
            BrevoProperties brevo = new BrevoProperties();
            brevo.setBaseUrl(url);
            assertThat(new BrevoConfig(brevo).brevoRestClient(factory)).isNotNull();

            PuntoRedProperties puntoRed = new PuntoRedProperties();
            puntoRed.setBaseUrl(url);
            assertThat(new PuntoRedConfig(puntoRed).puntoRedRestClient(factory)).isNotNull();
        }
    }

    @Test
    @DisplayName("Fechas aceptan yyyy-MM-dd y yyyy/MM/dd; cualquier otro formato se rechaza")
    void deserializadorFechas() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new SimpleModule()
                .addDeserializer(LocalDate.class, new JacksonConfig.FlexibleLocalDateDeserializer()));
        assertThat(mapper.readValue("\"2005-05-24\"", LocalDate.class)).isEqualTo(LocalDate.of(2005, 5, 24));
        assertThat(mapper.readValue("\" 2005/05/24 \"", LocalDate.class)).isEqualTo(LocalDate.of(2005, 5, 24));
        assertThatThrownBy(() -> mapper.readValue("\"24-05-2005\"", LocalDate.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("Formato de fecha inválido");
    }

    @Test
    @DisplayName("Montos se serializan siempre con dos decimales y null como null")
    void serializadorMontos() throws Exception {
        JacksonConfig.BigDecimalTwoDecimalsSerializer serializer = new JacksonConfig.BigDecimalTwoDecimalsSerializer();
        JsonGenerator gen = mock(JsonGenerator.class);
        serializer.serialize(null, gen, null);
        verify(gen).writeNull();
        serializer.serialize(new BigDecimal("10.555"), gen, null);
        verify(gen).writeRawValue("10.56");

        assertThat(new JacksonConfig().jacksonCustomizer()).isNotNull();
    }

    @Test
    @DisplayName("El administrador solo existe si usuario y contraseña están configurados")
    void usuarioAdministrador() {
        SecurityConfig config = new SecurityConfig();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        assertThat(((InMemoryUserDetailsManager) config.userDetailsService(encoder, "", "x")).userExists("")).isFalse();
        assertThat(((InMemoryUserDetailsManager) config.userDetailsService(encoder, "admin", " ")).userExists("admin")).isFalse();
        assertThat(((InMemoryUserDetailsManager) config.userDetailsService(encoder, "admin", "secreto")).userExists("admin")).isTrue();
    }

    @Test
    @DisplayName("CORS descarta orígenes vacíos y recorta espacios")
    void cors() {
        CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource("http://a.com, ,http://b.com ");
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api"));
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly("http://a.com", "http://b.com");
    }

    @Test
    @DisplayName("Llave JWT: efímera sin secreto, error si es corta, válida con 32 bytes")
    void llaveJwt() {
        JwtConfig config = new JwtConfig();
        assertThat(config.jwtSecretKey(null).getEncoded()).hasSize(32);
        assertThat(config.jwtSecretKey(" ").getEncoded()).hasSize(32);
        String corta = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> config.jwtSecretKey(corta)).isInstanceOf(IllegalStateException.class);
        SecretKey llave = config.jwtSecretKey(Base64.getEncoder().encodeToString(new byte[32]));
        assertThat(llave.getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    @DisplayName("La sesión solo es vigente si está loggeada y el jti coincide con la sesión activa")
    void sesionVigente() {
        DatosSeguridadBiometriaRepository repo = mock(DatosSeguridadBiometriaRepository.class);
        SesionVigenteValidator validator = new SesionVigenteValidator(repo);
        DatosSeguridadBiometria activa = DatosSeguridadBiometria.builder().username("u").loggeado(true).sesionId("S1").build();
        DatosSeguridadBiometria cerrada = DatosSeguridadBiometria.builder().username("c").loggeado(false).sesionId("S1").build();
        when(repo.findByUsername("u")).thenReturn(Optional.of(activa));
        when(repo.findByUsername("c")).thenReturn(Optional.of(cerrada));
        when(repo.findByUsername("x")).thenReturn(Optional.empty());

        assertThat(validator.validate(jwt(null, "S1")).hasErrors()).isTrue();
        assertThat(validator.validate(jwt("u", null)).hasErrors()).isTrue();
        assertThat(validator.validate(jwt("x", "S1")).hasErrors()).isTrue();
        assertThat(validator.validate(jwt("c", "S1")).hasErrors()).isTrue();
        assertThat(validator.validate(jwt("u", "OTRA")).hasErrors()).isTrue();
        assertThat(validator.validate(jwt("u", "S1")).hasErrors()).isFalse();
    }

    private static Jwt jwt(String subject, String id) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "HS256").claim("scope", "x");
        if (subject != null) {
            builder.subject(subject);
        }
        if (id != null) {
            builder.jti(id);
        }
        return builder.build();
    }

    @Test
    @DisplayName("PuntoRedProperties: alias de usuario e idDistribuidor numérico seguro")
    void puntoRedProperties() {
        PuntoRedProperties props = new PuntoRedProperties();
        props.setUsername("operador");
        assertThat(props.getUsername()).isEqualTo("operador");
        assertThat(props.getUsuario()).isEqualTo("operador");

        props.setIdDistribuidor(null);
        assertThat(props.getIdDistribuidorAsInteger()).isNull();
        props.setIdDistribuidor(" ");
        assertThat(props.getIdDistribuidorAsInteger()).isNull();
        props.setIdDistribuidor("abc");
        assertThat(props.getIdDistribuidorAsInteger()).isNull();
        props.setIdDistribuidor(" 77 ");
        assertThat(props.getIdDistribuidorAsInteger()).isEqualTo(77);
    }
}
