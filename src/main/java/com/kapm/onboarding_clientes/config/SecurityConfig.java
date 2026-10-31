package com.kapm.onboarding_clientes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Matriz de acceso:
 * <ul>
 *   <li>Público: onboarding (registro y prevalidación), login, catálogos, documentación y frontend estático.</li>
 *   <li>CLIENTE (JWT Bearer emitido en /api/auth/login): pagos y gestión de su propia sesión.</li>
 *   <li>ADMIN (HTTP Basic, credenciales por variables de entorno): administración de clientes, cuentas y conciliación.</li>
 * </ul>
 * Todo lo no listado queda denegado por defecto.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Usuario administrador tomado exclusivamente de variables de entorno (nunca del repositorio).
     * Si no está configurado, el usuario no existe y la administración queda cerrada.
     */
    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${app.security.admin-user:}") String adminUser,
            @Value("${app.security.admin-password:}") String adminPassword) {

        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        if (!adminUser.isBlank() && !adminPassword.isBlank()) {
            manager.createUser(User.withUsername(adminUser).password(passwordEncoder.encode(adminPassword)).roles("ADMIN").build());
        }
        return manager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            // API sin cookies de sesión: CSRF no aplica porque cada petición trae su credencial explícita
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .headers(headers -> headers.frameOptions(frame -> frame.deny()))
            .authorizeHttpRequests(auth -> auth
                // Público
                .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/clientes", "/api/clientes/validar", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/catalogos/**").permitAll()
                // Cliente autenticado con JWT
                .requestMatchers("/api/pagos/**", "/api/auth/estado", "/api/auth/logout").hasRole("CLIENTE")
                // Administración
                .requestMatchers("/api/admin/**", "/api/clientes/**", "/api/cuentas/**").hasRole("ADMIN")
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(JwtConfig.CLAIM_ROLES);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.security.cors.allowed-origins:http://localhost:8081}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
