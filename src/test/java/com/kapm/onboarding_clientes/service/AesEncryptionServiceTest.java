package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pruebas Unitarias - AesEncryptionService (AES/CBC/PKCS5Padding)")
class AesEncryptionServiceTest {

    // 32 bytes (256 bits) en Base64
    private static final String VALID_KEY_256_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    // 24 bytes (192 bits) en Base64
    private static final String VALID_KEY_192_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3";
    // 16 bytes (128 bits) en Base64
    private static final String VALID_KEY_128_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZg==";
    // 16 bytes (128 bits) en Base64 para IV
    private static final String VALID_IV_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZg==";

    private PuntoRedProperties properties;
    private AesEncryptionService aesService;

    @BeforeEach
    void setUp() {
        properties = new PuntoRedProperties();
        properties.setAesKey(VALID_KEY_256_BASE64);
        properties.setAesIv(VALID_IV_BASE64);

        aesService = new AesEncryptionService(properties);
        aesService.init();
    }

    @Nested
    @DisplayName("1. Pruebas de Round-Trip (Cifrado y Descifrado Básico)")
    class RoundTripTests {

        @Test
        @DisplayName("Cifrado y descifrado exitoso de una cadena simple")
        void testRoundTripSimple() {
            String textoOriginal = "TransaccionPuntoRed_123456";

            String cifrado = aesService.encrypt(textoOriginal);
            assertThat(cifrado).isNotNull().isNotBlank().isNotEqualTo(textoOriginal);

            String descifrado = aesService.decrypt(cifrado);
            assertThat(descifrado).isEqualTo(textoOriginal);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "A",
                "1234567890123456", // Exactamente 16 bytes (1 bloque)
                "12345678901234567890123456789012", // Exactamente 32 bytes (2 bloques)
                "Cuerpo de mensaje con multiples lineas\nSegunda linea de transaccion\r\nTercera linea",
                "<pago><referencia>CFE-987654</referencia><monto>345.50</monto></pago>"
        })
        @DisplayName("Round-trip con diversas longitudes y estructuras de texto")
        void testRoundTripDiversasLongitudes(String input) {
            String cifrado = aesService.encrypt(input);
            assertThat(cifrado).isNotNull();

            String descifrado = aesService.decrypt(cifrado);
            assertThat(descifrado).isEqualTo(input);
        }

        @Test
        @DisplayName("Soporte para diferentes tamaños de llave válidos: 128, 192 y 256 bits")
        void testSoporteDistintosTamanosLlave() {
            String mensaje = "Prueba de tamaño de llave";

            // AES-128
            String cifrado128 = aesService.encrypt(mensaje, VALID_KEY_128_BASE64, VALID_IV_BASE64);
            assertThat(aesService.decrypt(cifrado128, VALID_KEY_128_BASE64, VALID_IV_BASE64)).isEqualTo(mensaje);

            // AES-192
            String cifrado192 = aesService.encrypt(mensaje, VALID_KEY_192_BASE64, VALID_IV_BASE64);
            assertThat(aesService.decrypt(cifrado192, VALID_KEY_192_BASE64, VALID_IV_BASE64)).isEqualTo(mensaje);

            // AES-256
            String cifrado256 = aesService.encrypt(mensaje, VALID_KEY_256_BASE64, VALID_IV_BASE64);
            assertThat(aesService.decrypt(cifrado256, VALID_KEY_256_BASE64, VALID_IV_BASE64)).isEqualTo(mensaje);
        }
    }

    @Nested
    @DisplayName("2. Pruebas con Caracteres Especiales (ñ, acentos, diéresis, símbolos y UTF-8)")
    class CaracteresEspecialesTests {

        @Test
        @DisplayName("Cifrado y descifrado con 'ñ', mayúsculas, minúsculas y tildes en español")
        void testCaracteresEspanol() {
            String texto = "Señorita Peña pagó su recibo de energía eléctrica en el año 2026. " +
                    "Ñandú, pingüino, camión, acción, éxito, azúcar, López, Muñoz.";

            String cifrado = aesService.encrypt(texto);
            String descifrado = aesService.decrypt(cifrado);

            assertThat(descifrado).isEqualTo(texto);
        }

        @Test
        @DisplayName("Cifrado y descifrado con símbolos monetarios, matemáticos y caracteres de puntuación")
        void testSimbolosEspeciales() {
            String texto = "Total: $1,499.99 MXN | Comisión: 16% + IVA | Folio: #PUNTO-RED-8902_V2.0 & <XML>";

            String cifrado = aesService.encrypt(texto);
            String descifrado = aesService.decrypt(cifrado);

            assertThat(descifrado).isEqualTo(texto);
        }

        @Test
        @DisplayName("Cifrado y descifrado con caracteres multibyte UTF-8 y emojis")
        void testCaracteresMultibyteYEmojis() {
            String texto = "Pago exitoso ⚡💡 — ¡Gracias por su preferencia! 👍 🇲🇽";

            String cifrado = aesService.encrypt(texto);
            String descifrado = aesService.decrypt(cifrado);

            assertThat(descifrado).isEqualTo(texto);
        }
    }

    @Nested
    @DisplayName("3. Pruebas de Validación y Detección de Llave e IV Inválidos")
    class ValidacionesKeyEivTests {

        @Test
        @DisplayName("Falla al iniciar con llave de longitud menor a 16 bytes (ej. 10 bytes)")
        void testKeyLongitudMenorA16Bytes() {
            byte[] diezBytes = "1234567890".getBytes(StandardCharsets.UTF_8);
            String keyInvalida = Base64.getEncoder().encodeToString(diezBytes);

            properties.setAesKey(keyInvalida);
            AesEncryptionService servicioInvalido = new AesEncryptionService(properties);

            assertThatThrownBy(servicioInvalido::init)
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("Longitud de llave AES inválida (10 bytes)");
        }

        @Test
        @DisplayName("Falla al iniciar con llave de 15 bytes (no permitida)")
        void testKeyLongitud15Bytes() {
            byte[] quinceBytes = "123456789012345".getBytes(StandardCharsets.UTF_8);
            String keyInvalida = Base64.getEncoder().encodeToString(quinceBytes);

            assertThatThrownBy(() -> aesService.encrypt("test", keyInvalida, VALID_IV_BASE64))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("Longitud de llave AES inválida (15 bytes)");
        }

        @Test
        @DisplayName("Falla al iniciar con llave de 33 bytes (excede los 256 bits permitidos)")
        void testKeyLongitud33Bytes() {
            byte[] treintaYTresBytes = "123456789012345678901234567890123".getBytes(StandardCharsets.UTF_8);
            String keyInvalida = Base64.getEncoder().encodeToString(treintaYTresBytes);

            assertThatThrownBy(() -> aesService.encrypt("test", keyInvalida, VALID_IV_BASE64))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("Longitud de llave AES inválida (33 bytes)");
        }

        @Test
        @DisplayName("Falla cuando la llave no tiene codificación Base64 válida")
        void testKeyFormatoBase64Invalido() {
            String keyNoBase64 = "¡No-Es-Base64-Valido!===";

            assertThatThrownBy(() -> aesService.encrypt("test", keyNoBase64, VALID_IV_BASE64))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("no tiene un formato Base64 válido");
        }

        @Test
        @DisplayName("Falla cuando la llave es nula o vacía")
        void testKeyNulaOVacia() {
            assertThatThrownBy(() -> aesService.encrypt("test", null, VALID_IV_BASE64))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("requerida y no puede estar vacía");

            assertThatThrownBy(() -> aesService.encrypt("test", "   ", VALID_IV_BASE64))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("requerida y no puede estar vacía");
        }

        @Test
        @DisplayName("Falla con IV de longitud diferente a 16 bytes (ej. 8 bytes)")
        void testIvLongitudDiferenteA16Bytes() {
            byte[] ochoBytes = "12345678".getBytes(StandardCharsets.UTF_8);
            String ivInvalido = Base64.getEncoder().encodeToString(ochoBytes);

            assertThatThrownBy(() -> aesService.encrypt("test", VALID_KEY_256_BASE64, ivInvalido))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("Longitud de vector de inicialización (IV) inválida (8 bytes)");
        }

        @Test
        @DisplayName("Falla cuando el IV no tiene codificación Base64 válida")
        void testIvFormatoBase64Invalido() {
            String ivNoBase64 = "###-IV-Invalido-###";

            assertThatThrownBy(() -> aesService.encrypt("test", VALID_KEY_256_BASE64, ivNoBase64))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("no tiene un formato Base64 válido");
        }

        @Test
        @DisplayName("Falla cuando el IV es nulo o vacío")
        void testIvNuloOVacio() {
            assertThatThrownBy(() -> aesService.encrypt("test", VALID_KEY_256_BASE64, null))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("vector de inicialización IV (Base64) es requerido");
        }
    }

    @Nested
    @DisplayName("4. Pruebas de Entradas Nulas o Criptograma Corrupto")
    class ManejoErroresEntradaTests {

        @Test
        @DisplayName("Falla al intentar cifrar un texto nulo")
        void testCifrarTextoNulo() {
            assertThatThrownBy(() -> aesService.encrypt(null))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("El texto en claro a cifrar no puede ser nulo");
        }

        @Test
        @DisplayName("Falla al intentar descifrar un texto nulo")
        void testDescifrarTextoNulo() {
            assertThatThrownBy(() -> aesService.decrypt(null))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("El texto cifrado a descifrar no puede ser nulo");
        }

        @Test
        @DisplayName("Falla al intentar descifrar un texto que no es Base64")
        void testDescifrarNoBase64() {
            assertThatThrownBy(() -> aesService.decrypt("%%%NoBase64%%%"))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("Error durante el descifrado AES");
        }

        @Test
        @DisplayName("Falla al intentar descifrar un criptograma corrupto o alterado")
        void testDescifrarCriptogramaCorrupto() {
            String cifrado = aesService.encrypt("Mensaje Original");

            // Alterar deliberadamente el texto cifrado
            byte[] bytesCifrados = Base64.getDecoder().decode(cifrado);
            bytesCifrados[0] = (byte) (bytesCifrados[0] ^ 0xFF); // invertir bits
            String cifradoCorrupto = Base64.getEncoder().encodeToString(bytesCifrados);

            assertThatThrownBy(() -> aesService.decrypt(cifradoCorrupto))
                    .isInstanceOf(PuntoRedIntegracionException.class)
                    .hasMessageContaining("Error durante el descifrado AES");
        }
    }
}
