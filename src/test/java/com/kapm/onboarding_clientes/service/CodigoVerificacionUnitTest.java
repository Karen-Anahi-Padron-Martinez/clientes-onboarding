package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.brevo.BrevoEmailClient;
import com.kapm.onboarding_clientes.dto.request.EmitirCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.request.ValidarCodigo2FARequest;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.CodigoVerificacionPago;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.CodigoVerificacionPagoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CodigoVerificacionServiceImpl (unitarias): validaciones de entrada y casos límite")
class CodigoVerificacionUnitTest {

    private static final String CUENTA = "4819204812";
    private static final String CURP = "MEHC920824HDFRMN09";

    @Mock CodigoVerificacionPagoRepository codigoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock BrevoEmailClient brevoEmailClient;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks CodigoVerificacionServiceImpl servicio;

    private static EmitirCodigo2FARequest emitir(Consumer<EmitirCodigo2FARequest> cambio) {
        EmitirCodigo2FARequest r = EmitirCodigo2FARequest.builder().numeroCuenta(CUENTA).curp(CURP)
                .idProducto(55).idServicio(2).telefono("5512345678").monto(new BigDecimal("100.00")).build();
        cambio.accept(r);
        return r;
    }

    private static ValidarCodigo2FARequest validar(Consumer<ValidarCodigo2FARequest> cambio) {
        ValidarCodigo2FARequest r = ValidarCodigo2FARequest.builder().numeroCuenta(CUENTA).codigo("123456")
                .idProducto(55).idServicio(2).telefono("5512345678").monto(new BigDecimal("100.00")).build();
        cambio.accept(r);
        return r;
    }

    @Test
    @DisplayName("Emitir: solicitud, cuenta y monto obligatorios")
    void emitirEntradasInvalidas() {
        assertThatThrownBy(() -> servicio.emitirCodigo(null)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.emitirCodigo(emitir(r -> r.setNumeroCuenta(null)))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.emitirCodigo(emitir(r -> r.setNumeroCuenta(" ")))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.emitirCodigo(emitir(r -> r.setMonto(null)))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.emitirCodigo(emitir(r -> r.setMonto(BigDecimal.ZERO)))).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Emitir: busca por CURP si la cuenta no tiene cliente y exige correo registrado")
    void emitirBusquedaCliente() {
        when(clienteRepository.findByCuentaNumeroCuenta(CUENTA)).thenReturn(Optional.empty());
        when(clienteRepository.findByCurp(CURP)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.emitirCodigo(emitir(r -> r.setCurp(" " + CURP + " "))))
                .hasMessageContaining("CURP indicada");
        assertThatThrownBy(() -> servicio.emitirCodigo(emitir(r -> r.setCurp(null)))).hasMessageContaining("asociado a la cuenta");
        assertThatThrownBy(() -> servicio.emitirCodigo(emitir(r -> r.setCurp(" ")))).hasMessageContaining("asociado a la cuenta");

        when(clienteRepository.findByCurp(CURP)).thenReturn(Optional.of(Cliente.builder().correo(null).build()));
        assertThatThrownBy(() -> servicio.emitirCodigo(emitir(r -> { }))).hasMessageContaining("correo");
        when(clienteRepository.findByCurp(CURP)).thenReturn(Optional.of(Cliente.builder().correo(" ").build()));
        assertThatThrownBy(() -> servicio.emitirCodigo(emitir(r -> { }))).hasMessageContaining("correo");
    }

    @Test
    @DisplayName("Emitir: invalida códigos anteriores y saluda genéricamente si no hay nombre")
    void emitirInvalidaAnteriores() {
        Cliente cliente = Cliente.builder().curp(CURP).correo("cliente@correo.com").build();
        CodigoVerificacionPago anterior = CodigoVerificacionPago.builder().usado(false).build();
        when(clienteRepository.findByCuentaNumeroCuenta(CUENTA)).thenReturn(Optional.of(cliente));
        when(codigoRepository.findByNumeroCuentaAndUsadoFalse(CUENTA)).thenReturn(List.of(anterior));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        assertThat(servicio.emitirCodigo(emitir(r -> { })).isEnviado()).isTrue();

        assertThat(anterior.getUsado()).isTrue();
        verify(codigoRepository).saveAll(List.of(anterior));
        verify(brevoEmailClient).enviarCorreo(eq("cliente@correo.com"), anyString(), contains("Estimado cliente"));
    }

    @Test
    @DisplayName("Validar: solicitud, cuenta y formato del código")
    void validarEntradasInvalidas() {
        assertThatThrownBy(() -> servicio.validarCodigo(null)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.validarCodigo(validar(r -> r.setNumeroCuenta(null)))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.validarCodigo(validar(r -> r.setNumeroCuenta(" ")))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.validarCodigo(validar(r -> r.setCodigo(null)))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.validarCodigo(validar(r -> r.setCodigo("12345")))).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Validar: sin código emitido o con intentos agotados")
    void validarSinCodigoOAgotado() {
        when(codigoRepository.findTopByNumeroCuentaOrderByCreadoEnDesc(CUENTA)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.validarCodigo(validar(r -> { })))
                .hasMessage(ErrorMessage.CODIGO_2FA_NO_ENCONTRADO.getMensaje());

        CodigoVerificacionPago agotado = CodigoVerificacionPago.builder().usado(false).intentos(3)
                .expiraEn(LocalDateTime.now().plusMinutes(5)).build();
        when(codigoRepository.findTopByNumeroCuentaOrderByCreadoEnDesc(CUENTA)).thenReturn(Optional.of(agotado));
        assertThatThrownBy(() -> servicio.validarCodigo(validar(r -> { })))
                .hasMessage(ErrorMessage.CODIGO_2FA_INTENTOS_AGOTADOS.getMensaje());
        assertThat(agotado.getUsado()).isTrue();
        verify(codigoRepository).save(agotado);
    }

    @Test
    @DisplayName("Hash de operación: usa referencia o teléfono y tolera campos nulos")
    void hashOperacion() {
        String porReferencia = servicio.calcularHashOperacion(CUENTA, 1, 2, " REF ", "5512345678", BigDecimal.TEN);
        assertThat(porReferencia).isEqualTo(servicio.calcularHashOperacion(CUENTA, 1, 2, "REF", null, new BigDecimal("10.00")));
        assertThat(servicio.calcularHashOperacion(CUENTA, 1, 2, " ", "5512345678", BigDecimal.TEN))
                .isEqualTo(servicio.calcularHashOperacion(CUENTA, 1, 2, null, " 5512345678 ", BigDecimal.TEN));
        String vacio = servicio.calcularHashOperacion(null, null, null, null, null, null);
        assertThat(vacio).hasSize(64).isEqualTo(servicio.calcularHashOperacion("", 0, 0, "", "", BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Sin SHA-256 disponible se reporta un error de estado")
    void sinAlgoritmo() {
        try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(new NoSuchAlgorithmException("x"));
            assertThatThrownBy(() -> servicio.calcularHashOperacion(CUENTA, 1, 2, "R", null, BigDecimal.ONE))
                    .isInstanceOf(IllegalStateException.class);
        }
        verify(codigoRepository, never()).save(any());
    }
}
