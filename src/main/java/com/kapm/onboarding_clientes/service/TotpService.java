package com.kapm.onboarding_clientes.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

/**
 * Motor TOTP compatible con Google Authenticator (RFC 6238 / RFC 4226: HMAC-SHA1, 6 dígitos, periodo de 30 s).
 * Genera QR en PNG sin depender de java.awt para funcionar en contenedores Linux headless.
 */
@Service
public class TotpService {

    public static final String EMISOR_DEFAULT = "GestoPago";
    private static final int PERIODO_SEGUNDOS = 30;
    private static final int DIGITOS = 6;
    private static final int VENTANA_PASOS = 1;
    private static final int BYTES_SECRETO = 20;
    private static final String BASE32_ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generarSecretoBase32() {
        byte[] bytes = new byte[BYTES_SECRETO];
        SECURE_RANDOM.nextBytes(bytes);
        return codificarBase32(bytes);
    }

    public String construirOtpAuthUri(String emisor, String cuenta, String secretoBase32) {
        String issuer = (emisor == null || emisor.isBlank()) ? EMISOR_DEFAULT : emisor.trim();
        String account = (cuenta == null || cuenta.isBlank()) ? "usuario" : cuenta.trim();
        String label = urlEncode(issuer) + ":" + urlEncode(account);
        return "otpauth://totp/" + label
                + "?secret=" + secretoBase32.trim().toUpperCase(Locale.ROOT)
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITOS
                + "&period=" + PERIODO_SEGUNDOS;
    }

    public String generarQrDataUrl(String contenido) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 240, 240, hints);
            byte[] png = renderizarPngEscalaGrises(matrix);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No se pudo generar el código QR para Google Authenticator", e);
        }
    }

    public Optional<Long> validarCodigo(String secretoBase32, String codigo, Long ultimoPasoUsado) {
        return validarCodigo(secretoBase32, codigo, ultimoPasoUsado, Instant.now());
    }

    public Optional<Long> validarCodigo(String secretoBase32, String codigo, Long ultimoPasoUsado, Instant instante) {
        if (secretoBase32 == null || secretoBase32.isBlank() || codigo == null || !codigo.matches("^\\d{6}$")) {
            return Optional.empty();
        }
        long pasoActual = Math.floorDiv(instante.getEpochSecond(), PERIODO_SEGUNDOS);
        byte[] codigoIngresado = codigo.getBytes(StandardCharsets.UTF_8);

        for (long delta = -VENTANA_PASOS; delta <= VENTANA_PASOS; delta++) {
            long paso = pasoActual + delta;
            if (ultimoPasoUsado != null && paso <= ultimoPasoUsado) {
                continue;
            }
            String esperado = calcularCodigo(secretoBase32, paso);
            if (MessageDigest.isEqual(codigoIngresado, esperado.getBytes(StandardCharsets.UTF_8))) {
                return Optional.of(paso);
            }
        }
        return Optional.empty();
    }

    public String calcularCodigo(String secretoBase32, long pasoTiempo) {
        byte[] llave = decodificarBase32(secretoBase32);
        byte[] contador = ByteBuffer.allocate(8).putLong(pasoTiempo).array();
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(llave, "HmacSHA1"));
            byte[] hash = mac.doFinal(contador);
            int offset = hash[hash.length - 1] & 0x0F;
            int binario = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binario % 1_000_000;
            return String.format("%06d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error al calcular código TOTP", e);
        }
    }

    static String codificarBase32(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                sb.append(BASE32_ALFABETO.charAt((buffer >> bits) & 0x1F));
            }
        }
        if (bits > 0) {
            sb.append(BASE32_ALFABETO.charAt((buffer << (5 - bits)) & 0x1F));
        }
        return sb.toString();
    }

    static byte[] decodificarBase32(String base32) {
        String limpio = base32.trim().replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        ByteArrayOutputStream out = new ByteArrayOutputStream(limpio.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < limpio.length(); i++) {
            int val = BASE32_ALFABETO.indexOf(limpio.charAt(i));
            if (val < 0) {
                throw new IllegalArgumentException("Carácter inválido en secreto Base32");
            }
            buffer = (buffer << 5) | val;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                out.write((buffer >> bits) & 0xFF);
            }
        }
        return out.toByteArray();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Codificador PNG minimalista (8-bit grayscale, tipo de color 0) sin java.awt. */
    private static byte[] renderizarPngEscalaGrises(BitMatrix matrix) throws IOException {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        ByteArrayOutputStream raw = new ByteArrayOutputStream(height * (width + 1));
        for (int y = 0; y < height; y++) {
            raw.write(0); // filtro PNG: None
            for (int x = 0; x < width; x++) {
                raw.write(matrix.get(x, y) ? 0x00 : 0xFF);
            }
        }

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(compressed)) {
            deflater.write(raw.toByteArray());
        }

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(png);
        dos.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});

        ByteArrayOutputStream ihdr = new ByteArrayOutputStream(13);
        DataOutputStream ihdrOut = new DataOutputStream(ihdr);
        ihdrOut.writeInt(width);
        ihdrOut.writeInt(height);
        ihdrOut.writeByte(8); // bit depth
        ihdrOut.writeByte(0); // grayscale
        ihdrOut.writeByte(0); // compression
        ihdrOut.writeByte(0); // filter
        ihdrOut.writeByte(0); // interlace
        escribirChunk(dos, "IHDR", ihdr.toByteArray());
        escribirChunk(dos, "IDAT", compressed.toByteArray());
        escribirChunk(dos, "IEND", new byte[0]);
        dos.flush();
        return png.toByteArray();
    }

    private static void escribirChunk(DataOutputStream out, String tipo, byte[] data) throws IOException {
        byte[] typeBytes = tipo.getBytes(StandardCharsets.US_ASCII);
        out.writeInt(data.length);
        out.write(typeBytes);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }
}
