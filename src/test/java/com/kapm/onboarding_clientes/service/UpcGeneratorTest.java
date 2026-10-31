package com.kapm.onboarding_clientes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias - UpcGenerator")
class UpcGeneratorTest {

    private UpcGenerator upcGenerator;

    @BeforeEach
    void setUp() {
        upcGenerator = new UpcGenerator();
    }

    @Test
    @DisplayName("El código UPC generado no es nulo, no está en blanco y cumple con máx. 15 caracteres")
    void testGenerarUpcFormato() {
        String upc = upcGenerator.generar();

        assertThat(upc).isNotNull().isNotBlank();
        assertThat(upc.length()).isLessThanOrEqualTo(15);
        assertThat(upc.length()).isGreaterThanOrEqualTo(3);
        assertThat(upc).matches("^[A-Z0-9]+$");
        assertThat(upcGenerator.esValido(upc)).isTrue();
    }

    @Test
    @DisplayName("Generar 5,000 UPCs consecutivos sin colisiones (garantía de unicidad)")
    void testGenerarMuchosUpcsSinColision() {
        int cantidad = 5000;
        Set<String> set = new HashSet<>(cantidad);

        for (int i = 0; i < cantidad; i++) {
            String upc = upcGenerator.generar();
            assertThat(upc.length()).isLessThanOrEqualTo(15);
            assertThat(set.add(upc))
                    .as("El UPC '%s' ya había sido generado (colisión detectada en la iteración %d)", upc, i)
                    .isTrue();
        }

        assertThat(set).hasSize(cantidad);
    }

    @Test
    @DisplayName("Validación esValido con valores correctos e inválidos")
    void testEsValido() {
        assertThat(upcGenerator.esValido("ABC123456789")).isTrue();
        assertThat(upcGenerator.esValido("123456789012345")).isTrue(); // 15 chars exactos
        assertThat(upcGenerator.esValido("UPC")).isTrue(); // 3 chars

        assertThat(upcGenerator.esValido(null)).isFalse();
        assertThat(upcGenerator.esValido("")).isFalse();
        assertThat(upcGenerator.esValido("12")).isFalse(); // menos de 3 chars
        assertThat(upcGenerator.esValido("1234567890123456")).isFalse(); // 16 chars (excede 15)
        assertThat(upcGenerator.esValido("ABC-123_45")).isFalse(); // caracteres no alfanuméricos
    }
}
