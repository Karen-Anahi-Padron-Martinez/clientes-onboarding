package com.kapm.onboarding_clientes.util;

import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Reglas de formato de la referencia (servicios) o el teléfono (recargas) de un producto del catálogo.
 * La comparten la validación previa y el flujo de pago para que ambas acepten exactamente lo mismo.
 */
@Slf4j
public final class ReferenciaUtil {

    private ReferenciaUtil() {
    }

    /** Se captura teléfono en lugar de referencia: tipoFront 0, o tipoReferencia "a" (número telefónico). */
    public static boolean esRecarga(CatalogoProductoPuntoRed producto) {
        return (producto.getTipoFront() != null && producto.getTipoFront() == 0)
                || "a".equalsIgnoreCase(producto.getTipoReferencia());
    }

    /**
     * Verifica el valor contra la expresión regular configurada en el producto. Sin expresión configurada
     * se acepta; una expresión mal escrita en BD se ignora (y se registra) para no bloquear la venta.
     */
    public static boolean cumpleRegex(CatalogoProductoPuntoRed producto, String valor) {
        String regex = producto.getRegexReferencia();
        if (regex == null || regex.isBlank()) {
            return true;
        }
        try {
            return Pattern.compile(regex).matcher(valor).matches();
        } catch (PatternSyntaxException e) {
            log.warn("regex_referencia inválida en catálogo [idProducto: {}]; se omite", producto.getIdProducto());
            return true;
        }
    }
}
