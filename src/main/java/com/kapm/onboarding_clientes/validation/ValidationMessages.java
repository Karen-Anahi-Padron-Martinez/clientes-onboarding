package com.kapm.onboarding_clientes.validation;

/**
 * Constantes de mensajes de validación para evitar cadenas literales en los DTOs y validadores.
 */
public final class ValidationMessages {

    private ValidationMessages() {}

    // Constantes referenciando las llaves del ResourceBundle
    public static final String CLIENTE_NOMBRE_OBLIGATORIO = "{cliente.nombre.obligatorio}";
    public static final String CLIENTE_NOMBRE_SIZE = "{cliente.nombre.size}";
    public static final String CLIENTE_NOMBRE_PATTERN = "{cliente.nombre.pattern}";
    public static final String CLIENTE_SEGUNDO_NOMBRE_SIZE = "{cliente.segundoNombre.size}";
    public static final String CLIENTE_SEGUNDO_NOMBRE_PATTERN = "{cliente.segundoNombre.pattern}";
    public static final String CLIENTE_APELLIDO_PATERNO_OBLIGATORIO = "{cliente.apellidoPaterno.obligatorio}";
    public static final String CLIENTE_APELLIDO_PATERNO_SIZE = "{cliente.apellidoPaterno.size}";
    public static final String CLIENTE_APELLIDO_PATERNO_PATTERN = "{cliente.apellidoPaterno.pattern}";
    public static final String CLIENTE_APELLIDO_MATERNO_OBLIGATORIO = "{cliente.apellidoMaterno.obligatorio}";
    public static final String CLIENTE_APELLIDO_MATERNO_SIZE = "{cliente.apellidoMaterno.size}";
    public static final String CLIENTE_APELLIDO_MATERNO_PATTERN = "{cliente.apellidoMaterno.pattern}";
    public static final String CLIENTE_FECHA_NACIMIENTO_OBLIGATORIO = "{cliente.fechaNacimiento.obligatorio}";
    public static final String CLIENTE_FECHA_NACIMIENTO_PAST = "{cliente.fechaNacimiento.past}";
    public static final String CLIENTE_CURP_OBLIGATORIO = "{cliente.curp.obligatorio}";
    public static final String CLIENTE_CURP_PATTERN = "{cliente.curp.pattern}";
    public static final String CLIENTE_RFC_OBLIGATORIO = "{cliente.rfc.obligatorio}";
    public static final String CLIENTE_RFC_PATTERN = "{cliente.rfc.pattern}";
    public static final String CLIENTE_SEXO_OBLIGATORIO = "{cliente.sexo.obligatorio}";
    public static final String CLIENTE_NACIONALIDAD_OBLIGATORIO = "{cliente.nacionalidad.obligatorio}";
    public static final String CLIENTE_NACIONALIDAD_SIZE = "{cliente.nacionalidad.size}";
    public static final String CLIENTE_ESTADO_CIVIL_OBLIGATORIO = "{cliente.estadoCivil.obligatorio}";
    public static final String CLIENTE_CORREO_OBLIGATORIO = "{cliente.correo.obligatorio}";
    public static final String CLIENTE_CORREO_EMAIL = "{cliente.correo.email}";
    public static final String CLIENTE_CORREO_SIZE = "{cliente.correo.size}";
    public static final String CLIENTE_TELEFONO_MOVIL_OBLIGATORIO = "{cliente.telefonoMovil.obligatorio}";
    public static final String CLIENTE_TELEFONO_MOVIL_PATTERN = "{cliente.telefonoMovil.pattern}";
    public static final String CLIENTE_TELEFONO_ALT_PATTERN = "{cliente.telefonoAlternativo.pattern}";
    public static final String CLIENTE_DOMICILIO_OBLIGATORIO = "{cliente.domicilio.obligatorio}";
    public static final String CLIENTE_LABORAL_OBLIGATORIO = "{cliente.laboral.obligatorio}";
    public static final String CLIENTE_SALDO_MIN = "{cliente.saldoInicial.min}";
    public static final String CLIENTE_SALDO_MONTO = "{cliente.saldoInicial.monto}";

    public static final String DOMICILIO_CALLE_OBLIGATORIO = "{domicilio.calle.obligatorio}";
    public static final String DOMICILIO_CALLE_SIZE = "{domicilio.calle.size}";
    public static final String DOMICILIO_NUM_EXT_OBLIGATORIO = "{domicilio.numeroExterior.obligatorio}";
    public static final String DOMICILIO_NUM_EXT_SIZE = "{domicilio.numeroExterior.size}";
    public static final String DOMICILIO_NUM_INT_SIZE = "{domicilio.numeroInterior.size}";
    public static final String DOMICILIO_COLONIA_OBLIGATORIO = "{domicilio.colonia.obligatorio}";
    public static final String DOMICILIO_COLONIA_SIZE = "{domicilio.colonia.size}";
    public static final String DOMICILIO_MUNICIPIO_OBLIGATORIO = "{domicilio.municipio.obligatorio}";
    public static final String DOMICILIO_MUNICIPIO_SIZE = "{domicilio.municipio.size}";
    public static final String DOMICILIO_ESTADO_OBLIGATORIO = "{domicilio.estado.obligatorio}";
    public static final String DOMICILIO_ESTADO_SIZE = "{domicilio.estado.size}";
    public static final String DOMICILIO_CP_OBLIGATORIO = "{domicilio.codigoPostal.obligatorio}";
    public static final String DOMICILIO_CP_PATTERN = "{domicilio.codigoPostal.pattern}";
    public static final String DOMICILIO_PAIS_OBLIGATORIO = "{domicilio.pais.obligatorio}";
    public static final String DOMICILIO_PAIS_SIZE = "{domicilio.pais.size}";

    public static final String LABORAL_OCUPACION_OBLIGATORIO = "{laboral.ocupacion.obligatorio}";
    public static final String LABORAL_OCUPACION_SIZE = "{laboral.ocupacion.size}";
    public static final String LABORAL_EMPRESA_OBLIGATORIO = "{laboral.empresa.obligatorio}";
    public static final String LABORAL_EMPRESA_SIZE = "{laboral.empresa.size}";
    public static final String LABORAL_INGRESO_OBLIGATORIO = "{laboral.ingresoMensual.obligatorio}";
    public static final String LABORAL_INGRESO_MIN = "{laboral.ingresoMensual.min}";
    public static final String LABORAL_INGRESO_MONTO = "{laboral.ingresoMensual.monto}";

    public static final String SEGURIDAD_USERNAME_OBLIGATORIO = "{seguridad.username.obligatorio}";
    public static final String SEGURIDAD_USERNAME_SIZE = "{seguridad.username.size}";
    public static final String SEGURIDAD_PASSWORD_OBLIGATORIO = "{seguridad.password.obligatorio}";
    public static final String SEGURIDAD_PASSWORD_SIZE = "{seguridad.password.size}";
    public static final String SEGURIDAD_TIPO_BIO_OBLIGATORIO = "{seguridad.tipoBiometria.obligatorio}";
    public static final String SEGURIDAD_DATOS_BIO_OBLIGATORIO = "{seguridad.datosBiometricos.obligatorio}";

    public static final String CONSULTA_CUENTA_OBLIGATORIO = "{consulta.cuenta.obligatorio}";
    public static final String CONSULTA_CUENTA_PATTERN = "{consulta.cuenta.pattern}";
    public static final String BUSQUEDA_CRITERIO_PRESENTE = "{busqueda.criterio.presente}";
    public static final String BUSQUEDA_PAGINA_MIN = "{busqueda.pagina.min}";
    public static final String BUSQUEDA_TAMANIO_MIN = "{busqueda.tamanio.min}";
    public static final String BUSQUEDA_TAMANIO_MAX = "{busqueda.tamanio.max}";
    public static final String VALIDAR_ID_PRESENTE = "{validar.identificador.presente}";
}
