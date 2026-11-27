package com.kapm.onboarding_clientes.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

/**
 * ddl-auto=update agrega columnas pero nunca quita un NOT NULL. Empresa y biometría pasaron a ser opcionales,
 * así que en bases PostgreSQL creadas antes se relajan esas restricciones al arrancar (idempotente).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MigracionColumnasOpcionales implements ApplicationRunner {

    private static final List<String> SENTENCIAS = List.of(
            "ALTER TABLE clientes ALTER COLUMN empresa DROP NOT NULL",
            "ALTER TABLE datos_seguridad_biometria ALTER COLUMN tipo_biometria DROP NOT NULL",
            "ALTER TABLE datos_seguridad_biometria ALTER COLUMN tipo_biometria DROP DEFAULT",
            "ALTER TABLE datos_seguridad_biometria ALTER COLUMN hash_biometrico DROP NOT NULL"
    );

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conexion = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(conexion.getMetaData().getDatabaseProductName())) {
                return;
            }
        } catch (Exception e) {
            log.warn("No se pudo identificar la base de datos para la migración de columnas opcionales: {}", e.getMessage());
            return;
        }

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String sentencia : SENTENCIAS) {
            try {
                jdbc.execute(sentencia);
            } catch (Exception e) {
                log.warn("Migración omitida ({}): {}", sentencia, e.getMessage());
            }
        }
    }
}
