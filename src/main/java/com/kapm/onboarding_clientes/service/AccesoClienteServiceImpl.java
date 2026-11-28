package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.request.LoginRequest;
import com.kapm.onboarding_clientes.dto.request.RestablecerPasswordRequest;
import com.kapm.onboarding_clientes.dto.request.SolicitarRecuperacionRequest;
import com.kapm.onboarding_clientes.dto.request.VerificarLoginRequest;
import com.kapm.onboarding_clientes.dto.response.CodigoVerificacionResponse;
import com.kapm.onboarding_clientes.dto.response.LoginDesafioResponse;
import com.kapm.onboarding_clientes.dto.response.LoginResponse;
import com.kapm.onboarding_clientes.entity.Cliente;
import com.kapm.onboarding_clientes.entity.DatosSeguridadBiometria;
import com.kapm.onboarding_clientes.entity.PropositoCodigoAcceso;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import com.kapm.onboarding_clientes.repository.ClienteRepository;
import com.kapm.onboarding_clientes.repository.DatosSeguridadBiometriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Sin @Transactional a nivel de método: la validación del código corre en su propia transacción para que
 * los intentos fallidos se guarden aunque la petición termine en error.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccesoClienteServiceImpl implements AccesoClienteService {

    private final ClienteService clienteService;
    private final CodigoAccesoService codigoAccesoService;
    private final SeguridadBiometriaService seguridadBiometriaService;
    private final DatosSeguridadBiometriaRepository seguridadRepository;
    private final ClienteRepository clienteRepository;
    private final Google2FAService google2FAService;

    @Override
    public LoginDesafioResponse iniciarLogin(LoginRequest request) {
        Cliente cliente = clienteService.verificarCredenciales(request);
        if (cliente == null) {
            throw new ReglaNegocioException(ErrorMessage.AUTH_SIN_CORREO_2FA);
        }
        String username = request.getUsername().trim();
        if (google2FAService != null && google2FAService.estaHabilitado(username)) {
            return LoginDesafioResponse.builder()
                    .requiereCodigo(true)
                    .username(username)
                    .metodo2fa(Google2FAService.METODO_GOOGLE)
                    .mensaje("Ingresa el código de 6 dígitos de tu app Google Authenticator")
                    .build();
        }
        CodigoVerificacionResponse envio = codigoAccesoService.emitir(username, cliente, PropositoCodigoAcceso.LOGIN);

        return LoginDesafioResponse.builder()
                .requiereCodigo(true)
                .username(username)
                .metodo2fa(Google2FAService.METODO_CORREO)
                .correoEnmascarado(envio.getCorreoEnmascarado())
                .codigoExpiraEn(envio.getExpiraEn())
                .mensaje("Te enviamos un código de verificación a " + envio.getCorreoEnmascarado())
                .build();
    }

    @Override
    public LoginResponse verificarLogin(VerificarLoginRequest request) {
        String username = request.getUsername().trim();
        if (google2FAService != null && google2FAService.estaHabilitado(username)) {
            google2FAService.validarCodigoActivo(username, request.getCodigo());
        } else {
            codigoAccesoService.validar(username, PropositoCodigoAcceso.LOGIN, request.getCodigo());
        }
        return clienteService.abrirSesion(username);
    }

    @Override
    public void solicitarRecuperacion(SolicitarRecuperacionRequest request) {
        Optional<Cliente> cliente = buscarPorIdentificador(request.getIdentificador())
                .filter(c -> Boolean.TRUE.equals(c.getActivo()) && c.getDatosSeguridad() != null);
        if (cliente.isEmpty()) {
            log.info("Solicitud de recuperación para un identificador sin cuenta activa");
            return;
        }
        codigoAccesoService.emitir(cliente.get().getDatosSeguridad().getUsername(), cliente.get(),
                PropositoCodigoAcceso.RECUPERAR_PASSWORD);
    }

    @Override
    public void restablecerPassword(RestablecerPasswordRequest request) {
        DatosSeguridadBiometria seguridad = buscarPorIdentificador(request.getIdentificador())
                .map(Cliente::getDatosSeguridad)
                .orElseThrow(() -> new ReglaNegocioException(ErrorMessage.CODIGO_2FA_NO_ENCONTRADO));

        codigoAccesoService.validar(seguridad.getUsername(), PropositoCodigoAcceso.RECUPERAR_PASSWORD, request.getCodigo());

        seguridad.setPasswordHash(seguridadBiometriaService.encriptarPassword(request.getNuevaPassword()));
        // Cualquier token emitido antes deja de servir
        seguridad.setLoggeado(false);
        seguridad.setSesionId(null);
        seguridadRepository.save(seguridad);
        log.info("Contraseña restablecida mediante código enviado al correo");
    }

    private Optional<Cliente> buscarPorIdentificador(String identificador) {
        String valor = identificador == null ? "" : identificador.trim();
        if (valor.isEmpty()) {
            return Optional.empty();
        }
        return valor.contains("@")
                ? clienteRepository.findByCorreo(valor.toLowerCase())
                : clienteRepository.findByDatosSeguridadUsername(valor);
    }
}
