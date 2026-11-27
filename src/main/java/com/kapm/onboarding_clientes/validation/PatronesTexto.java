package com.kapm.onboarding_clientes.validation;

/**
 * Expresiones regulares de los campos de texto libre. La web y la app móvil aplican las mismas reglas antes de enviar.
 */
public final class PatronesTexto {

    private PatronesTexto() {
    }

    /** Nombres y apellidos: letras (con acentos) y espacios. */
    public static final String NOMBRE = "^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\\s]+$";

    /** Calle, colonia, municipio y números de domicilio: letras, dígitos, espacios y . , # ' / ° - */
    public static final String DOMICILIO = "^[\\p{L}\\p{N}\\s.,#'/°-]+$";
    public static final String DOMICILIO_OPCIONAL = "^[\\p{L}\\p{N}\\s.,#'/°-]*$";

    /** Empresa (opcional): letras, dígitos, espacios y . , & ' ( ) - */
    public static final String EMPRESA = "^[\\p{L}\\p{N}\\s.,&'()-]*$";

    /** Usuario: 4 a 50 caracteres entre letras sin acento, dígitos, punto, guion y guion bajo. */
    public static final String USERNAME = "^[a-zA-Z0-9._-]{4,50}$";

    /** Contraseña nueva: 8 a 72 caracteres sin espacios, con al menos una minúscula, una mayúscula y un dígito. */
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,72}$";

    public static final String MENSAJE_PASSWORD =
            "La contraseña debe tener de 8 a 72 caracteres, sin espacios, con al menos una mayúscula, una minúscula, un número y un carácter especial";
    public static final String MENSAJE_USERNAME =
            "El usuario solo admite letras sin acentos, números, punto, guion y guion bajo (4 a 50 caracteres)";
    public static final String MENSAJE_DOMICILIO = "Solo se permiten letras, números, espacios y . , # ' / ° -";
    public static final String MENSAJE_EMPRESA = "La empresa solo admite letras, números, espacios y . , & ' ( ) -";
}
