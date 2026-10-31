package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.config.PuntoRedProperties;
import com.kapm.onboarding_clientes.exception.PuntoRedIntegracionException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


@Slf4j
@Service
@RequiredArgsConstructor
public class AesEncryptionService {

    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";

    private final PuntoRedProperties properties;

    private SecretKey secretKey;
    private IvParameterSpec ivSpec;


    @PostConstruct
    public void init() {
        validarYConfigurarCredenciales(properties.getAesKey(), properties.getAesIv());
        log.info("Servicio de cifrado AES de PuntoRed inicializado correctamente (Algoritmo: {})", CIPHER_TRANSFORMATION);
    }

    /**
     * Cifra una cadena de texto en claro utilizando la configuración AES/CBC/PKCS5Padding de PuntoRed.
     *
     * @param plainText Texto en claro a cifrar
     * @return Cadena cifrada codificada en Base64
     * @throws PuntoRedIntegracionException Si ocurre un error durante el cifrado o si el texto es nulo
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            throw new PuntoRedIntegracionException("El texto en claro a cifrar no puede ser nulo");
        }
        return encryptInternal(plainText, this.secretKey, this.ivSpec);
    }

    /**
     * Descifra una cadena cifrada en Base64 utilizando la configuración AES/CBC/PKCS5Padding de PuntoRed.
     *
     * @param cipherTextBase64 Cadena cifrada en Base64
     * @return Texto en claro descifrado codificado en UTF-8
     * @throws PuntoRedIntegracionException Si ocurre un error durante el descifrado o si el texto es nulo
     */
    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null) {
            throw new PuntoRedIntegracionException("El texto cifrado a descifrar no puede ser nulo");
        }
        return decryptInternal(cipherTextBase64, this.secretKey, this.ivSpec);
    }

    
    public String encrypt(String plainText, String keyBase64, String ivBase64) {
        if (plainText == null) {
            throw new PuntoRedIntegracionException("El texto en claro a cifrar no puede ser nulo");
        }
        ParametrosCifrado params = construirParametros(keyBase64, ivBase64);
        return encryptInternal(plainText, params.secretKey(), params.ivSpec());
    }

    
    public String decrypt(String cipherTextBase64, String keyBase64, String ivBase64) {
        if (cipherTextBase64 == null) {
            throw new PuntoRedIntegracionException("El texto cifrado a descifrar no puede ser nulo");
        }
        ParametrosCifrado params = construirParametros(keyBase64, ivBase64);
        return decryptInternal(cipherTextBase64, params.secretKey(), params.ivSpec());
    }

    private String encryptInternal(String plainText, SecretKey key, IvParameterSpec iv) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipherBytes);
        } catch (Exception e) {
            throw new PuntoRedIntegracionException("Error durante el cifrado AES de PuntoRed: " + e.getMessage(), e);
        }
    }

    private String decryptInternal(String cipherTextBase64, SecretKey key, IvParameterSpec iv) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(cipherTextBase64.trim());
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new PuntoRedIntegracionException("Error durante el descifrado AES de PuntoRed: " + e.getMessage(), e);
        }
    }

    public ParametrosCifrado construirParametros(String keyBase64, String ivBase64) {
        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new PuntoRedIntegracionException("La llave AES (Base64) es requerida y no puede estar vacía");
        }
        if (ivBase64 == null || ivBase64.isBlank()) {
            throw new PuntoRedIntegracionException("El vector de inicialización IV (Base64) es requerido y no puede estar vacío");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new PuntoRedIntegracionException("La llave AES no tiene un formato Base64 válido", e);
        }

        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new PuntoRedIntegracionException(
                    "Longitud de llave AES inválida (" + keyBytes.length + " bytes). Debe ser exactamente de 16, 24 o 32 bytes (128, 192 o 256 bits)"
            );
        }

        byte[] ivBytes;
        try {
            ivBytes = Base64.getDecoder().decode(ivBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new PuntoRedIntegracionException("El vector de inicialización (IV) no tiene un formato Base64 válido", e);
        }

        if (ivBytes.length != 16) {
            throw new PuntoRedIntegracionException(
                    "Longitud de vector de inicialización (IV) inválida (" + ivBytes.length + " bytes). Debe ser exactamente de 16 bytes (128 bits)"
            );
        }

        return new ParametrosCifrado(new SecretKeySpec(keyBytes, KEY_ALGORITHM), new IvParameterSpec(ivBytes));
    }

    private void validarYConfigurarCredenciales(String keyBase64, String ivBase64) {
        ParametrosCifrado params = construirParametros(keyBase64, ivBase64);
        this.secretKey = params.secretKey();
        this.ivSpec = params.ivSpec();
    }

    public record ParametrosCifrado(SecretKey secretKey, IvParameterSpec ivSpec) { }
}
