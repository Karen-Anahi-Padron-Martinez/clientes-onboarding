package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.brevo.BrevoEmailClient;
import com.kapm.onboarding_clientes.dto.response.ConfigurarGoogle2FAResponse;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.DatosSeguridadBiometria;
import com.kapm.onboarding_clientes.entity.EstatusPago;
import com.kapm.onboarding_clientes.entity.PagoServicio;
import com.kapm.onboarding_clientes.entity.catalogo.CatalogoProductoPuntoRed;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.CatalogoProductoPuntoRedRepository;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Google 2FA (TOTP + QR) y comprobante de pago por correo")
class TotpAndTicketTest {

    @Test
    @DisplayName("TotpService genera secreto Base32, código de 6 dígitos válido, QR PNG y bloquea reutilización")
    void totpFlujoCompleto() {
        TotpService totp = new TotpService();
        String secreto = totp.generarSecretoBase32();
        assertThat(secreto).matches("^[A-Z2-7]{32}$");

        Instant ahora = Instant.ofEpochSecond(1_700_000_000L);
        long paso = Math.floorDiv(ahora.getEpochSecond(), 30);
        String codigo = totp.calcularCodigo(secreto, paso);
        assertThat(codigo).matches("^\\d{6}$");

        Optional<Long> validado = totp.validarCodigo(secreto, codigo, null, ahora);
        assertThat(validado).contains(paso);

        // Anti-replay: el mismo paso ya usado no se vuelve a aceptar
        assertThat(totp.validarCodigo(secreto, codigo, paso, ahora)).isEmpty();
        assertThat(totp.validarCodigo(secreto, "000000", null, ahora)).isEmpty();

        String uri = totp.construirOtpAuthUri("GestoPago", "karen@correo.com", secreto);
        assertThat(uri).startsWith("otpauth://totp/GestoPago:karen%40correo.com?secret=" + secreto);

        String qr = totp.generarQrDataUrl(uri);
        assertThat(qr).startsWith("data:image/png;base64,");
        byte[] pngBytes = Base64.getDecoder().decode(qr.substring("data:image/png;base64,".length()));
        // Firma mágica PNG: 89 50 4E 47
        assertThat(pngBytes).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }

    @Test
    @DisplayName("Google2FAService inicia, activa, valida y desactiva TOTP correctamente")
    void google2faCicloVida() {
        DatosSeguridadBiometriaRepository segRepo = mock(DatosSeguridadBiometriaRepository.class);
        ClienteRepository cliRepo = mock(ClienteRepository.class);
        SeguridadBiometriaService biometriaService = mock(SeguridadBiometriaService.class);
        TotpService totp = new TotpService();
        Google2FAService service = new Google2FAService(segRepo, cliRepo, biometriaService, totp);

        DatosSeguridadBiometria seg = DatosSeguridadBiometria.builder()
                .username("karen")
                .passwordHash("hash")
                .totpHabilitado(false)
                .build();
        when(segRepo.findByUsername("karen")).thenReturn(Optional.of(seg));
        when(cliRepo.findByDatosSeguridadUsername("karen")).thenReturn(Optional.of(Cliente.builder().correo("karen@test.com").build()));
        when(biometriaService.validarPassword("Secret1!", "hash")).thenReturn(true);

        ConfigurarGoogle2FAResponse setup = service.iniciarConfiguracion("karen");
        assertThat(setup.isHabilitado()).isFalse();
        assertThat(setup.getSecreto()).isEqualTo(seg.getTotpSecretPendiente());
        assertThat(setup.getQrCodeDataUrl()).startsWith("data:image/png;base64,");

        long pasoActual = Math.floorDiv(Instant.now().getEpochSecond(), 30);
        String codigoValido = totp.calcularCodigo(setup.getSecreto(), pasoActual);

        service.activar("karen", codigoValido);
        assertThat(seg.getTotpHabilitado()).isTrue();
        assertThat(seg.getTotpSecret()).isEqualTo(setup.getSecreto());
        assertThat(seg.getTotpSecretPendiente()).isNull();
        assertThat(service.estaHabilitado("karen")).isTrue();

        // Código de paso anterior ya consumido falla
        assertThatThrownBy(() -> service.validarCodigoActivo("karen", codigoValido))
                .isInstanceOf(ReglaNegocioException.class);

        // Con el siguiente paso (tolerancia +1) se puede desactivar
        String siguienteCodigo = totp.calcularCodigo(setup.getSecreto(), pasoActual + 1);
        service.desactivar("karen", "Secret1!", siguienteCodigo);
        assertThat(seg.getTotpHabilitado()).isFalse();
        assertThat(seg.getTotpSecret()).isNull();
    }

    @Test
    @DisplayName("TicketCorreoService envía comprobante HTML con leyenda del producto y sin comisión")
    void ticketCorreoIncluyeLeyendaSinComision() {
        BrevoEmailClient brevo = mock(BrevoEmailClient.class);
        ClienteRepository cliRepo = mock(ClienteRepository.class);
        CatalogoProductoPuntoRedRepository catRepo = mock(CatalogoProductoPuntoRedRepository.class);
        TicketCorreoService service = new TicketCorreoService(brevo, cliRepo, catRepo);

        PagoServicio pago = PagoServicio.builder()
                .upc("UPC123456")
                .numeroCuenta("4819204812")
                .idProducto(14302)
                .idServicio(2284)
                .telefono("5512345678")
                .monto(new BigDecimal("100.00"))
                .comision(new BigDecimal("12.00"))
                .estatus(EstatusPago.EXITOSO)
                .numAutorizacion("AUT-99")
                .idTxProveedor("TX-77")
                .textoRespuesta("TRANSACCION APROBADA")
                .fechaCreacion(LocalDateTime.of(2026, 11, 27, 18, 30, 0))
                .build();

        CatalogoProductoPuntoRed producto = CatalogoProductoPuntoRed.builder()
                .idProducto(14302)
                .idServicio(2284)
                .nombreProducto("ABIB 100")
                .nombreServicio("ABIB")
                .descripcion("Recibe soporte las 24h marcando al *787. Conserva tu comprobante de pago.")
                .build();

        service.enviarComprobante(pago, "karen@test.com", "Karen", producto);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(brevo).enviarCorreo(eq("karen@test.com"), any(), htmlCaptor.capture());

        String html = htmlCaptor.getValue();
        assertThat(html).contains("ABIB 100");
        assertThat(html).contains("UPC123456");
        assertThat(html).contains("$100.00 MXN");
        assertThat(html).contains("Recibe soporte las 24h marcando al *787. Conserva tu comprobante de pago.");
        assertThat(html).doesNotContainIgnoringCase("Comisión");
    }
}
