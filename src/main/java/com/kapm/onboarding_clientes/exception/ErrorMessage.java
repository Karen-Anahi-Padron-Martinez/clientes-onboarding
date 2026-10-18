package com.kapm.onboarding_clientes.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catálogo centralizado de errores con código numérico (int) y mensaje descriptivo (String).
 * Permite estandarizar respuestas de error con formato estricto: {"codigo": int, "mensaje": String}.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorMessage {

    // =========================================================================
    // 1. DATOS NO VÁLIDOS / VALIDACIÓN (Código: 1)
    // =========================================================================
    DATOS_INVALIDOS(1, "Datos no válidos"),
    PARAMETROS_INVALIDOS(1, "La solicitud contiene parámetros inválidos o incompletos"),
    TIPO_PARAMETRO_INVALIDO(1, "El tipo de dato de uno o más parámetros es incorrecto"),
    VALIDACION_FALLIDA(1, "Error de validación en los datos de entrada"),

    // =========================================================================
    // 2. JSON MALFORMADO / ILEGIBLE (Código: 2)
    // =========================================================================
    JSON_MALFORMADO(2, "Cuerpo de solicitud mal formado o ilegible"),
    CUERPO_PETICION_ILEGIBLE(2, "El cuerpo de la solicitud (Request Body) está ausente o mal formado"),
    CAMPO_NO_MODIFICABLE(2, "La solicitud contiene campos que no se pueden modificar o no están permitidos"),

    // =========================================================================
    // 3. RECURSO NO ENCONTRADO (Código: 3)
    // =========================================================================
    RECURSO_NO_ENCONTRADO(3, "Recurso no encontrado"),
    CLIENTE_NO_ENCONTRADO(3, "Cliente no encontrado"),
    CLIENTE_NO_ENCONTRADO_BUSQUEDA(3, "No se encontraron clientes que coincidan con los criterios de búsqueda especificados"),
    CLIENTE_SIN_CUENTA(3, "El cliente no tiene una cuenta bancaria asociada"),
    CUENTA_NO_ENCONTRADA(3, "No se encontró la cuenta bancaria especificada"),
    CUENTA_NO_ASOCIADA_CLIENTE(3, "La cuenta no se encuentra asociada a ningún cliente"),
    AUTH_USUARIO_NO_ENCONTRADO(3, "Usuario no encontrado en el sistema de seguridad"),
    CATALOGO_NO_ENCONTRADO(3, "Valor fuera de catálogo o no reconocido"),
    RECURSO_RUTA_NO_ENCONTRADA(3, "Ruta no encontrada en el servidor"),

    // =========================================================================
    // 4. RECURSO DUPLICADO (Código: 4)
    // =========================================================================
    RECURSO_DUPLICADO(4, "Recurso duplicado"),
    CLIENTE_YA_REGISTRADO(4, "El cliente ya se encuentra registrado"),
    CURP_DUPLICADA(4, "Ya existe un cliente registrado con la CURP proporcionada"),
    RFC_DUPLICADO(4, "Ya existe un cliente registrado con el RFC proporcionado"),
    CORREO_DUPLICADO(4, "Ya existe un cliente registrado con el correo electrónico proporcionado"),
    AUTH_USUARIO_DUPLICADO(4, "El nombre de usuario ya está registrado"),

    // =========================================================================
    // 5. REGLAS DE NEGOCIO (Código: 5)
    // =========================================================================
    REGLA_NEGOCIO(5, "Violación de regla de negocio"),
    CURP_FORMATO_INVALIDO(5, "El formato de la CURP es inválido (debe cumplir los 18 caracteres oficiales)"),
    RFC_FORMATO_INVALIDO(5, "El formato del RFC es inválido (debe contener 12 o 13 caracteres)"),
    CLIENTE_INACTIVO(5, "No se puede realizar la operación porque el cliente se encuentra inactivo"),
    CLIENTE_YA_ACTIVO(5, "El cliente ya se encuentra activo en el sistema"),
    CLIENTE_YA_INACTIVO(5, "El cliente ya se encuentra inactivo en el sistema"),
    IDENTIFICADOR_REQUERIDO(5, "Debe proporcionar la CURP o el RFC dentro del cuerpo JSON (Request Body)"),
    FECHA_NACIMIENTO_REQUERIDA(5, "La fecha de nacimiento es obligatoria"),
    FECHA_NACIMIENTO_FUTURA(5, "La fecha de nacimiento no puede ser una fecha futura"),
    CLIENTE_MENOR_DE_EDAD(5, "El cliente debe ser mayor de edad (18 años o más)"),
    SALDO_INICIAL_NEGATIVO(5, "El saldo inicial de la cuenta no puede ser negativo"),
    AUTH_CREDENCIALES_INVALIDAS(5, "Credenciales inválidas o datos biométricos incorrectos"),
    AUTH_SESION_EXPIRADA(5, "Sesión expirada por inactividad"),
    AUTH_PASSWORD_VACIA(5, "La contraseña no puede estar vacía"),
    AUTH_BIOMETRIA_VACIA(5, "La plantilla biométrica no puede estar vacía"),
    AUTH_HASH_ERROR(5, "Error al generar hash criptográfico para biometría"),
    AUTH_CLIENTE_INACTIVO(5, "El cliente asociado a este usuario está inactivo"),
    SEXO_INVALIDO(5, "El sexo proporcionado no existe en el catálogo oficial"),
    ESTADO_CIVIL_INVALIDO(5, "El estado civil proporcionado no existe en el catálogo oficial"),
    ESTADO_REPUBLICA_INVALIDO(5, "El estado de la república proporcionado no existe en el catálogo oficial"),
    PAIS_INVALIDO(5, "El país proporcionado no existe en el catálogo oficial"),
    NACIONALIDAD_INVALIDA(5, "La nacionalidad proporcionada no existe en el catálogo oficial"),
    OCUPACION_INVALIDA(5, "La ocupación proporcionada no existe en el catálogo oficial"),

    // =========================================================================
    // 6. CONFLICTO DE INTEGRIDAD (Código: 6)
    // =========================================================================
    CONFLICTO_INTEGRIDAD(6, "Conflicto de integridad de datos en el sistema"),
    INTEGRIDAD_DATOS_VIOLADA(6, "Conflicto de integridad de datos en el sistema"),

    // =========================================================================
    // 7. MÉTODO NO PERMITIDO (Código: 7)
    // =========================================================================
    METODO_NO_PERMITIDO(7, "Método HTTP no soportado para este endpoint"),
    METODO_NO_SOPORTADO(7, "El método HTTP solicitado no está soportado para este endpoint"),

    // =========================================================================
    // 8. TIPO MULTIMEDIA NO SOPORTADO (Código: 8)
    // =========================================================================
    MEDIA_TYPE_NO_SOPORTADO(8, "Tipo de contenido multimedia no soportado"),

    // =========================================================================
    // 99. ERROR INTERNO DEL SERVIDOR (Código: 99)
    // =========================================================================
    ERROR_INTERNO(99, "Ha ocurrido un error inesperado en el servidor"),
    ERROR_INTERNO_SERVIDOR(99, "Ocurrió un error interno en el servidor al procesar la solicitud");

    private final int codigo;
    private final String mensaje;
}
