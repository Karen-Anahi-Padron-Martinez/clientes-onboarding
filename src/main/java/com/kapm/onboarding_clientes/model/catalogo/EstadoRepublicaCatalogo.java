package com.kapm.onboarding_clientes.model.catalogo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum EstadoRepublicaCatalogo {
    AGUASCALIENTES("AGUASCALIENTES", "Aguascalientes"),
    BAJA_CALIFORNIA("BAJA_CALIFORNIA", "Baja California"),
    BAJA_CALIFORNIA_SUR("BAJA_CALIFORNIA_SUR", "Baja California Sur"),
    CAMPECHE("CAMPECHE", "Campeche"),
    CHIAPAS("CHIAPAS", "Chiapas"),
    CHIHUAHUA("CHIHUAHUA", "Chihuahua"),
    CIUDAD_DE_MEXICO("CIUDAD_DE_MEXICO", "Ciudad de México"),
    COAHUILA("COAHUILA", "Coahuila"),
    COLIMA("COLIMA", "Colima"),
    DURANGO("DURANGO", "Durango"),
    ESTADO_DE_MEXICO("ESTADO_DE_MEXICO", "Estado de México"),
    GUANAJUATO("GUANAJUATO", "Guanajuato"),
    GUERRERO("GUERRERO", "Guerrero"),
    HIDALGO("HIDALGO", "Hidalgo"),
    JALISCO("JALISCO", "Jalisco"),
    MICHOACAN("MICHOACAN", "Michoacán"),
    MORELOS("MORELOS", "Morelos"),
    NAYARIT("NAYARIT", "Nayarit"),
    NUEVO_LEON("NUEVO_LEON", "Nuevo León"),
    OAXACA("OAXACA", "Oaxaca"),
    PUEBLA("PUEBLA", "Puebla"),
    QUERETARO("QUERETARO", "Querétaro"),
    QUINTANA_ROO("QUINTANA_ROO", "Quintana Roo"),
    SAN_LUIS_POTOSI("SAN_LUIS_POTOSI", "San Luis Potosí"),
    SINALOA("SINALOA", "Sinaloa"),
    SONORA("SONORA", "Sonora"),
    TABASCO("TABASCO", "Tabasco"),
    TAMAULIPAS("TAMAULIPAS", "Tamaulipas"),
    TLAXCALA("TLAXCALA", "Tlaxcala"),
    VERACRUZ("VERACRUZ", "Veracruz"),
    YUCATAN("YUCATAN", "Yucatán"),
    ZACATECAS("ZACATECAS", "Zacatecas");

    private final String codigo;
    private final String descripcion;

    public static boolean esValido(String valor) {
        if (valor == null || valor.isBlank()) return false;
        String v = valor.trim().toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace(" ", "_");

        for (EstadoRepublicaCatalogo e : values()) {
            String normName = e.name().replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U");
            if (normName.equalsIgnoreCase(v) || e.codigo.equalsIgnoreCase(v) || e.descripcion.equalsIgnoreCase(valor.trim())) {
                return true;
            }
        }
        return false;
    }
}
