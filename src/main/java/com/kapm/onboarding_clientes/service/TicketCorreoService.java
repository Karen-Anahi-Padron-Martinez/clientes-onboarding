package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.brevo.BrevoEmailClient;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.util.DatosSensibles;
import com.kapm.onboarding_clientes.util.MensajeProveedor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Envía el comprobante (ticket) de una transacción exitosa al correo registrado del cliente.
 * Nunca propaga excepciones: una falla de correo jamás revierte ni interrumpe un pago aplicado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCorreoService {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.of("es", "MX"));

    private final BrevoEmailClient brevoEmailClient;
    private final ClienteRepository clienteRepository;
    private final CatalogoProductoPuntoRedRepository catalogoProductoRepository;

    public void enviarComprobante(PagoServicio pago, String correo, String nombreCliente, CatalogoProductoPuntoRed producto) {
        if (pago == null || correo == null || correo.isBlank()) {
            return;
        }
        try {
            CatalogoProductoPuntoRed prod = producto;
            if (prod == null && pago.getIdProducto() != null) {
                prod = catalogoProductoRepository.findByIdProducto(pago.getIdProducto()).orElse(null);
            }
            String servicio = prod != null && prod.getNombreProducto() != null
                    ? prod.getNombreProducto()
                    : (prod != null && prod.getNombreServicio() != null ? prod.getNombreServicio() : "Servicio");
            boolean rechazado = pago.getEstatus() == EstatusPago.RECHAZADO || pago.getEstatus() == EstatusPago.REVERSADO;
            String prefijoAsunto = rechazado ? "Comprobante de pago rechazado " : "Comprobante de pago ";
            String asunto = prefijoAsunto + servicio + " (Folio " + valorSeguro(pago.getUpc()) + ") - GestoPago";
            String html = construirHtmlTicket(pago, nombreCliente, prod);
            brevoEmailClient.enviarCorreo(correo, asunto, html);
            log.info("Comprobante de pago ({}) enviado por correo [upc: {}]", pago.getEstatus(), pago.getUpc());
        } catch (Exception e) {
            log.warn("No se pudo enviar el comprobante por correo [upc: {}]: {}", pago.getUpc(), e.getMessage());
        }
    }

    public void enviarComprobantePorCurp(PagoServicio pago) {
        if (pago == null || pago.getCurp() == null || pago.getCurp().isBlank()) {
            return;
        }
        try {
            Cliente cliente = clienteRepository.findByCurp(pago.getCurp()).orElse(null);
            if (cliente == null || cliente.getCorreo() == null || cliente.getCorreo().isBlank()) {
                return;
            }
            CatalogoProductoPuntoRed producto = pago.getIdProducto() != null
                    ? catalogoProductoRepository.findByIdProducto(pago.getIdProducto()).orElse(null)
                    : null;
            enviarComprobante(pago, cliente.getCorreo(), cliente.getNombre(), producto);
        } catch (Exception e) {
            log.warn("No se pudo consultar cliente para enviar comprobante [upc: {}]: {}", pago.getUpc(), e.getMessage());
        }
    }

    String construirHtmlTicket(PagoServicio pago, String nombreCliente, CatalogoProductoPuntoRed producto) {
        String nombre = (nombreCliente != null && !nombreCliente.isBlank()) ? nombreCliente.trim() : "Estimado cliente";
        String nombreProducto = producto != null && producto.getNombreProducto() != null
                ? producto.getNombreProducto()
                : (producto != null && producto.getNombreServicio() != null ? producto.getNombreServicio() : "Servicio");
        String referencia = (pago.getReferencia() != null && !pago.getReferencia().isBlank())
                ? pago.getReferencia()
                : pago.getTelefono();
        String monto = formatearMonto(pago.getMonto());
        String cuenta = DatosSensibles.enmascarar(pago.getNumeroCuenta());
        String respuestaProveedor = MensajeProveedor.limpiar(pago.getTextoRespuesta());
        String fecha = pago.getFechaCreacion() != null
                ? pago.getFechaCreacion().format(FECHA_FMT)
                : LocalDateTime.now().format(FECHA_FMT);
        String leyenda = producto != null && producto.getDescripcion() != null && !producto.getDescripcion().isBlank()
                ? producto.getDescripcion().trim()
                : null;

        boolean rechazado = pago.getEstatus() == EstatusPago.RECHAZADO || pago.getEstatus() == EstatusPago.REVERSADO;
        String estatusTexto = pago.getEstatus() != null ? pago.getEstatus().name() : "EXITOSO";
        String badgeBg = rechazado ? "#fee2e2" : "#dcfce7";
        String badgeColor = rechazado ? "#991b1b" : "#166534";
        String introTexto = rechazado
                ? "tu operación fue rechazada por el proveedor. No se realizó ningún cargo a tu cuenta. Aquí tienes el ticket de tu transacción:"
                : "tu operación se realizó con éxito. Aquí tienes el ticket de tu transacción:";

        StringBuilder filas = new StringBuilder();
        agregarFila(filas, "Estado", "<span style='display:inline-block;background:" + badgeBg + ";color:" + badgeColor + ";padding:2px 10px;border-radius:999px;font-weight:bold;font-size:12px;'>"
                + escapeHtml(estatusTexto) + "</span>", true);
        agregarFila(filas, "Folio (UPC)", escapeHtml(valorSeguro(pago.getUpc())), true);
        if (pago.getNumAutorizacion() != null && !pago.getNumAutorizacion().isBlank()) {
            agregarFila(filas, "Autorización", escapeHtml(pago.getNumAutorizacion()), true);
        }
        if (pago.getIdTxProveedor() != null && !pago.getIdTxProveedor().isBlank()) {
            agregarFila(filas, "ID de transacción", escapeHtml(pago.getIdTxProveedor()), true);
        }
        agregarFila(filas, "Servicio", escapeHtml(nombreProducto), true);
        if (referencia != null && !referencia.isBlank()) {
            agregarFila(filas, "Referencia", escapeHtml(referencia), true);
        }
        agregarFila(filas, "Cuenta de cargo", escapeHtml(cuenta), true);
        agregarFila(filas, "Total", "<strong>" + escapeHtml(monto) + "</strong>", true);
        if (respuestaProveedor != null && !respuestaProveedor.isBlank()) {
            agregarFila(filas, "Respuesta del proveedor", escapeHtml(respuestaProveedor), true);
        }
        agregarFila(filas, "Fecha", escapeHtml(fecha), true);

        String bloqueLeyenda = leyenda != null
                ? "<div style='margin-top:20px;padding:14px;background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:6px;font-size:12px;line-height:1.5;color:#475569;'>"
                  + "<strong style='display:block;color:#1e293b;margin-bottom:4px;'>Información del servicio</strong>"
                  + escapeHtml(leyenda)
                  + "</div>"
                : "";

        return "<div style='font-family:Arial,sans-serif;max-width:540px;margin:0 auto;padding:24px;border:1px solid #e2e8f0;border-radius:10px;color:#1e293b;background:#ffffff;'>"
                + "<h2 style='color:#1a365d;text-align:center;margin:0 0 8px 0;'>Comprobante de Pago</h2>"
                + "<p style='text-align:center;color:#64748b;font-size:13px;margin:0 0 20px 0;'>GestoPago · Banco Digital</p>"
                + "<p style='margin:0 0 16px 0;'>Hola <strong>" + escapeHtml(nombre) + "</strong>, " + introTexto + "</p>"
                + "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                + filas
                + "</table>"
                + bloqueLeyenda
                + "<hr style='border:none;border-top:1px solid #e2e8f0;margin:20px 0;'/>"
                + "<p style='font-size:12px;color:#94a3b8;text-align:center;margin:0;'>Conserva este comprobante para cualquier aclaración. Si no reconoces esta operación, contáctanos de inmediato.</p>"
                + "</div>";
    }

    private static void agregarFila(StringBuilder sb, String etiqueta, String valorHtml, boolean conBorde) {
        String borde = conBorde ? "border-bottom:1px solid #f1f5f9;" : "";
        sb.append("<tr>")
                .append("<td style='padding:8px 0;color:#64748b;").append(borde).append("'>").append(escapeHtml(etiqueta)).append("</td>")
                .append("<td style='padding:8px 0;text-align:right;color:#0f172a;").append(borde).append("'>").append(valorHtml).append("</td>")
                .append("</tr>");
    }

    private static String formatearMonto(BigDecimal monto) {
        if (monto == null) {
            return "$0.00 MXN";
        }
        return "$" + monto.setScale(2, RoundingMode.HALF_UP).toPlainString() + " MXN";
    }

    private static String valorSeguro(String s) {
        return s != null && !s.isBlank() ? s.trim() : "—";
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
