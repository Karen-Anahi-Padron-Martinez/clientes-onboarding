package com.kapm.onboarding_clientes.validation;

import com.kapm.onboarding_clientes.util.DatosSensibles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Utilidades de validación de CURP/RFC, montos y enmascaramiento")
class ValidacionUtilTest {

    private static final String CURP = "PEGJ900515HDFRMN01";

    @Test
    @DisplayName("CURP: longitud, formato y sanitización")
    void curp() {
        assertThat(CurpRfcUtil.esCurpValida(null)).isFalse();
        assertThat(CurpRfcUtil.esCurpValida("CORTA")).isFalse();
        assertThat(CurpRfcUtil.esCurpValida("1234567890123456AB")).isFalse();
        assertThat(CurpRfcUtil.esCurpValida(" " + CURP.toLowerCase() + " ")).isTrue();
        assertThat(CurpRfcUtil.sanitizarCurp(" pegj ")).isEqualTo("PEGJ");
        assertThat(CurpRfcUtil.sanitizarCurp(null)).isNull();
    }

    @Test
    @DisplayName("RFC: longitud, formato y sanitización")
    void rfc() {
        assertThat(CurpRfcUtil.esRfcValido(null)).isFalse();
        assertThat(CurpRfcUtil.esRfcValido("PEG900515")).isFalse();
        assertThat(CurpRfcUtil.esRfcValido("PEGJX9005151A2")).isFalse();
        assertThat(CurpRfcUtil.esRfcValido("PEGJ9005151A2")).isTrue();
        assertThat(CurpRfcUtil.esRfcValido("PEG9005151A2")).isTrue();
        assertThat(CurpRfcUtil.esRfcValido("PEGJ90051X1A2")).isFalse();
        assertThat(CurpRfcUtil.sanitizarRfc(" peg ")).isEqualTo("PEG");
        assertThat(CurpRfcUtil.sanitizarRfc(null)).isNull();
    }

    @Test
    @DisplayName("La fecha de nacimiento debe coincidir con los dígitos AAMMDD de la CURP")
    void coincideFecha() {
        assertThat(CurpRfcUtil.coincideFechaConCurp(CURP, LocalDate.of(1990, 5, 15))).isTrue();
        assertThat(CurpRfcUtil.coincideFechaConCurp(CURP, LocalDate.of(1991, 5, 15))).isFalse();
        assertThat(CurpRfcUtil.coincideFechaConCurp(CURP, null)).isFalse();
        assertThat(CurpRfcUtil.coincideFechaConCurp("INVALIDA", LocalDate.of(1990, 5, 15))).isFalse();
    }

    @Test
    @DisplayName("El monto debe tener exactamente dos decimales; null se delega a @NotNull")
    void montoDecimal() {
        MontoDecimalValidator validator = new MontoDecimalValidator();
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid(new BigDecimal("56.00"), null)).isTrue();
        assertThat(validator.isValid(new BigDecimal("56.0"), null)).isFalse();
    }

    @Test
    @DisplayName("Enmascarar conserva solo los últimos 4 caracteres")
    void enmascarar() {
        assertThat(DatosSensibles.enmascarar(null)).isEqualTo("****");
        assertThat(DatosSensibles.enmascarar("123")).isEqualTo("****");
        assertThat(DatosSensibles.enmascarar("5512345678")).isEqualTo("******5678");
    }
}
