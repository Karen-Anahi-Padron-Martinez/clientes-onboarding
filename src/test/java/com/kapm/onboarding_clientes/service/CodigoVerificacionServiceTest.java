package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.client.brevo.BrevoEmailClient;
import com.kapm.onboarding_clientes.dto.request.EmitirCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.request.ValidarCodigo2FARequest;
import com.kapm.onboarding_clientes.dto.response.CodigoVerificacionResponse;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.CodigoVerificacionPago;
import com.kapm.onboarding_clientes.entity.Cuenta;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.CodigoVerificacionPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - CodigoVerificacionService (2FA por Correo)")
class CodigoVerificacionServiceTest {

    @Mock
    private CodigoVerificacionPagoRepository codigoVerificacionRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private BrevoEmailClient brevoEmailClient;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private CodigoVerificacionServiceImpl codigoVerificacionService;

    private Cliente clientePrueba;
    private final String NUMERO_CUENTA = "4819204812";
    private final String CORREO_CLIENTE = "juan.perez@dominio.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(codigoVerificacionService, "expiracionMinutos", 5);
        ReflectionTestUtils.setField(codigoVerificacionService, "maxIntentos", 3);
        ReflectionTestUtils.setField(codigoVerificacionService, "maxSolicitudes10Min", 3);

        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .build();

        clientePrueba = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("López")
                .curp("MEHC920824HDFRMN09")
                .correo(CORREO_CLIENTE)
                .cuenta(cuenta)
                .build();
    }

    @Test
    @DisplayName("1. Emisión y validación exitosa: Código correcto consume el código (usado = true)")
    void testCodigoCorrectoValidaYConsume() {
        when(clienteRepository.findByCuentaNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(clientePrueba));
        when(codigoVerificacionRepository.countByNumeroCuentaAndCreadoEnAfter(eq(NUMERO_CUENTA), any(LocalDateTime.class))).thenReturn(0L);
        when(codigoVerificacionRepository.findByNumeroCuentaAndUsadoFalse(NUMERO_CUENTA)).thenReturn(new ArrayList<>());

        ArgumentCaptor<CodigoVerificacionPago> entityCaptor = ArgumentCaptor.forClass(CodigoVerificacionPago.class);
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);

        when(codigoVerificacionRepository.save(entityCaptor.capture())).thenAnswer(i -> i.getArgument(0));
        when(brevoEmailClient.enviarCorreo(eq(CORREO_CLIENTE), anyString(), htmlCaptor.capture())).thenReturn("<msg-123>");

        EmitirCodigo2FARequest emitirReq = EmitirCodigo2FARequest.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .idProducto(55)
                .idServicio(2)
                .referencia("REC-CFE-001")
                .monto(new BigDecimal("150.00"))
                .build();

        // 1. Emitir código
        CodigoVerificacionResponse resp = codigoVerificacionService.emitirCodigo(emitirReq);
        assertThat(resp.isEnviado()).isTrue();
        assertThat(resp.getCorreoEnmascarado()).isEqualTo("j***@dominio.com");

        CodigoVerificacionPago guardado = entityCaptor.getValue();
        assertThat(guardado.getNumeroCuenta()).isEqualTo(NUMERO_CUENTA);
        assertThat(guardado.getUsado()).isFalse();
        assertThat(guardado.getIntentos()).isEqualTo(0);
        // Validar que en la base de datos se guardó el hash BCrypt y NUNCA el código en claro
        assertThat(guardado.getHashCodigo()).startsWith("$2a$");

        // Extraer el código enviado en el correo para la prueba
        String codigoEnviado = extraerCodigoDeHtml(htmlCaptor.getValue());
        assertThat(codigoEnviado).matches("^\\d{6}$");

        // 2. Simular validación con código correcto
        when(codigoVerificacionRepository.findTopByNumeroCuentaOrderByCreadoEnDesc(NUMERO_CUENTA))
                .thenReturn(Optional.of(guardado));

        ValidarCodigo2FARequest validarReq = ValidarCodigo2FARequest.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .codigo(codigoEnviado)
                .idProducto(55)
                .idServicio(2)
                .referencia("REC-CFE-001")
                .monto(new BigDecimal("150.00"))
                .build();

        boolean valido = codigoVerificacionService.validarCodigo(validarReq);
        assertThat(valido).isTrue();
        assertThat(guardado.getUsado()).isTrue(); // Consumido exitosamente
    }

    @Test
    @DisplayName("2. Código incorrecto: incrementa intentos y lanza CODIGO_2FA_INVALIDO (código: 5)")
    void testCodigoIncorrectoIncrementaIntentos() {
        String codigoReal = "123456";
        String hashReal = passwordEncoder.encode(codigoReal);

        String hashOperacion = codigoVerificacionService.calcularHashOperacion(
                NUMERO_CUENTA, 55, 2, "REC-CFE-001", null, new BigDecimal("150.00"));

        CodigoVerificacionPago entity = CodigoVerificacionPago.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .hashCodigo(hashReal)
                .hashOperacion(hashOperacion)
                .expiraEn(LocalDateTime.now().plusMinutes(5))
                .intentos(0)
                .usado(false)
                .build();

        when(codigoVerificacionRepository.findTopByNumeroCuentaOrderByCreadoEnDesc(NUMERO_CUENTA))
                .thenReturn(Optional.of(entity));

        ValidarCodigo2FARequest validarReq = ValidarCodigo2FARequest.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .codigo("999999") // Código incorrecto
                .idProducto(55)
                .idServicio(2)
                .referencia("REC-CFE-001")
                .monto(new BigDecimal("150.00"))
                .build();

        assertThatThrownBy(() -> codigoVerificacionService.validarCodigo(validarReq))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> {
                    ReglaNegocioException rne = (ReglaNegocioException) ex;
                    assertThat(rne.getCodigo()).isEqualTo(5);
                    assertThat(rne.getMessage()).contains("Código incorrecto");
                });

        assertThat(entity.getIntentos()).isEqualTo(1);
        assertThat(entity.getUsado()).isFalse();
        verify(codigoVerificacionRepository).save(entity);
    }

    @Test
    @DisplayName("3. Código expirado: lanza CODIGO_2FA_EXPIRADO (código: 5) e invalida")
    void testCodigoExpiradoLanzaExcepcion() {
        String codigoReal = "123456";
        String hashReal = passwordEncoder.encode(codigoReal);

        String hashOperacion = codigoVerificacionService.calcularHashOperacion(
                NUMERO_CUENTA, 55, 2, "REC-CFE-001", null, new BigDecimal("150.00"));

        CodigoVerificacionPago entity = CodigoVerificacionPago.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .hashCodigo(hashReal)
                .hashOperacion(hashOperacion)
                .expiraEn(LocalDateTime.now().minusMinutes(1)) // Expirado hace 1 minuto
                .intentos(0)
                .usado(false)
                .build();

        when(codigoVerificacionRepository.findTopByNumeroCuentaOrderByCreadoEnDesc(NUMERO_CUENTA))
                .thenReturn(Optional.of(entity));

        ValidarCodigo2FARequest validarReq = ValidarCodigo2FARequest.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .codigo(codigoReal)
                .idProducto(55)
                .idServicio(2)
                .referencia("REC-CFE-001")
                .monto(new BigDecimal("150.00"))
                .build();

        assertThatThrownBy(() -> codigoVerificacionService.validarCodigo(validarReq))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> {
                    ReglaNegocioException rne = (ReglaNegocioException) ex;
                    assertThat(rne.getCodigo()).isEqualTo(5);
                    assertThat(rne.getErrorMessage()).isEqualTo(ErrorMessage.CODIGO_2FA_EXPIRADO);
                });

        assertThat(entity.getUsado()).isTrue();
    }

    @Test
    @DisplayName("4. Código reutilizado: si ya fue usado lanza CODIGO_2FA_YA_USADO (código: 5)")
    void testCodigoReutilizadoLanzaExcepcion() {
        String codigoReal = "123456";
        String hashReal = passwordEncoder.encode(codigoReal);

        String hashOperacion = codigoVerificacionService.calcularHashOperacion(
                NUMERO_CUENTA, 55, 2, "REC-CFE-001", null, new BigDecimal("150.00"));

        CodigoVerificacionPago entity = CodigoVerificacionPago.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .hashCodigo(hashReal)
                .hashOperacion(hashOperacion)
                .expiraEn(LocalDateTime.now().plusMinutes(5))
                .intentos(0)
                .usado(true) // Ya fue usado
                .build();

        when(codigoVerificacionRepository.findTopByNumeroCuentaOrderByCreadoEnDesc(NUMERO_CUENTA))
                .thenReturn(Optional.of(entity));

        ValidarCodigo2FARequest validarReq = ValidarCodigo2FARequest.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .codigo(codigoReal)
                .idProducto(55)
                .idServicio(2)
                .referencia("REC-CFE-001")
                .monto(new BigDecimal("150.00"))
                .build();

        assertThatThrownBy(() -> codigoVerificacionService.validarCodigo(validarReq))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> {
                    ReglaNegocioException rne = (ReglaNegocioException) ex;
                    assertThat(rne.getCodigo()).isEqualTo(5);
                    assertThat(rne.getErrorMessage()).isEqualTo(ErrorMessage.CODIGO_2FA_YA_USADO);
                });
    }

    @Test
    @DisplayName("5. Operación distinta: monto o datos cambiados lanza CODIGO_2FA_OPERACION_DISTINTA (código: 5)")
    void testOperacionDistintaLanzaExcepcion() {
        String codigoReal = "123456";
        String hashReal = passwordEncoder.encode(codigoReal);

        // Emitido para monto $150.00
        String hashOperacionOriginal = codigoVerificacionService.calcularHashOperacion(
                NUMERO_CUENTA, 55, 2, "REC-CFE-001", null, new BigDecimal("150.00"));

        CodigoVerificacionPago entity = CodigoVerificacionPago.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .hashCodigo(hashReal)
                .hashOperacion(hashOperacionOriginal)
                .expiraEn(LocalDateTime.now().plusMinutes(5))
                .intentos(0)
                .usado(false)
                .build();

        when(codigoVerificacionRepository.findTopByNumeroCuentaOrderByCreadoEnDesc(NUMERO_CUENTA))
                .thenReturn(Optional.of(entity));

        // Intento de validar con monto cambiado a $500.00
        ValidarCodigo2FARequest validarReq = ValidarCodigo2FARequest.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .codigo(codigoReal)
                .idProducto(55)
                .idServicio(2)
                .referencia("REC-CFE-001")
                .monto(new BigDecimal("500.00")) // Monto alterado
                .build();

        assertThatThrownBy(() -> codigoVerificacionService.validarCodigo(validarReq))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> {
                    ReglaNegocioException rne = (ReglaNegocioException) ex;
                    assertThat(rne.getCodigo()).isEqualTo(5);
                    assertThat(rne.getErrorMessage()).isEqualTo(ErrorMessage.CODIGO_2FA_OPERACION_DISTINTA);
                });
    }

    @Test
    @DisplayName("6. Límite de intentos: Al 3er intento fallido se invalida y lanza CODIGO_2FA_INTENTOS_AGOTADOS")
    void testLimiteDeIntentosInvalidaCodigo() {
        String codigoReal = "123456";
        String hashReal = passwordEncoder.encode(codigoReal);

        String hashOperacion = codigoVerificacionService.calcularHashOperacion(
                NUMERO_CUENTA, 55, 2, "REC-CFE-001", null, new BigDecimal("150.00"));

        CodigoVerificacionPago entity = CodigoVerificacionPago.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .hashCodigo(hashReal)
                .hashOperacion(hashOperacion)
                .expiraEn(LocalDateTime.now().plusMinutes(5))
                .intentos(2) // Ya llevaba 2 intentos previos
                .usado(false)
                .build();

        when(codigoVerificacionRepository.findTopByNumeroCuentaOrderByCreadoEnDesc(NUMERO_CUENTA))
                .thenReturn(Optional.of(entity));

        ValidarCodigo2FARequest validarReq = ValidarCodigo2FARequest.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .codigo("000000") // 3er intento fallido
                .idProducto(55)
                .idServicio(2)
                .referencia("REC-CFE-001")
                .monto(new BigDecimal("150.00"))
                .build();

        assertThatThrownBy(() -> codigoVerificacionService.validarCodigo(validarReq))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> {
                    ReglaNegocioException rne = (ReglaNegocioException) ex;
                    assertThat(rne.getCodigo()).isEqualTo(5);
                    assertThat(rne.getErrorMessage()).isEqualTo(ErrorMessage.CODIGO_2FA_INTENTOS_AGOTADOS);
                });

        assertThat(entity.getIntentos()).isEqualTo(3);
        assertThat(entity.getUsado()).isTrue(); // Invalidado definitivamente
    }

    @Test
    @DisplayName("7. Límite de solicitudes: Máximo 3 códigos por 10 minutos por cuenta lanza CODIGO_2FA_LIMITE_SOLICITUDES")
    void testLimiteDeSolicitudesPorCuenta() {
        when(clienteRepository.findByCuentaNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(clientePrueba));
        // Ya se solicitaron 3 códigos en los últimos 10 minutos
        when(codigoVerificacionRepository.countByNumeroCuentaAndCreadoEnAfter(eq(NUMERO_CUENTA), any(LocalDateTime.class)))
                .thenReturn(3L);

        EmitirCodigo2FARequest emitirReq = EmitirCodigo2FARequest.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .idProducto(55)
                .idServicio(2)
                .referencia("REC-CFE-001")
                .monto(new BigDecimal("150.00"))
                .build();

        assertThatThrownBy(() -> codigoVerificacionService.emitirCodigo(emitirReq))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(ex -> {
                    ReglaNegocioException rne = (ReglaNegocioException) ex;
                    assertThat(rne.getCodigo()).isEqualTo(5);
                    assertThat(rne.getErrorMessage()).isEqualTo(ErrorMessage.CODIGO_2FA_LIMITE_SOLICITUDES);
                });

        verify(brevoEmailClient, never()).enviarCorreo(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("8. Invalidación de anteriores: Al emitir uno nuevo se invalidan los anteriores no usados")
    void testEmitirNuevoInvalidaAnteriores() {
        when(clienteRepository.findByCuentaNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(clientePrueba));
        when(codigoVerificacionRepository.countByNumeroCuentaAndCreadoEnAfter(eq(NUMERO_CUENTA), any(LocalDateTime.class))).thenReturn(1L);

        CodigoVerificacionPago previo1 = CodigoVerificacionPago.builder().numeroCuenta(NUMERO_CUENTA).usado(false).build();
        CodigoVerificacionPago previo2 = CodigoVerificacionPago.builder().numeroCuenta(NUMERO_CUENTA).usado(false).build();
        when(codigoVerificacionRepository.findByNumeroCuentaAndUsadoFalse(NUMERO_CUENTA))
                .thenReturn(List.of(previo1, previo2));

        EmitirCodigo2FARequest emitirReq = EmitirCodigo2FARequest.builder()
                .numeroCuenta(NUMERO_CUENTA)
                .idProducto(55)
                .idServicio(2)
                .referencia("REC-CFE-001")
                .monto(new BigDecimal("150.00"))
                .build();

        codigoVerificacionService.emitirCodigo(emitirReq);

        assertThat(previo1.getUsado()).isTrue();
        assertThat(previo2.getUsado()).isTrue();
        verify(codigoVerificacionRepository).saveAll(List.of(previo1, previo2));
    }

    private String extraerCodigoDeHtml(String html) {
        Pattern pattern = Pattern.compile(">(\\d{6})<");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("No se encontró el código numérico de 6 dígitos en el HTML enviado");
    }
}
