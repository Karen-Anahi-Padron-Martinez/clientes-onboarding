package com.kapm.onboarding_clientes.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    public static class FlexibleLocalDateDeserializer extends StdDeserializer<LocalDate> {

        private static final DateTimeFormatter FORMATTER_ISO   = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final DateTimeFormatter FORMATTER_SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        public FlexibleLocalDateDeserializer() {
            super(LocalDate.class);
        }

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateStr = p.getText().trim();
            try {
                return LocalDate.parse(dateStr, FORMATTER_ISO);
            } catch (DateTimeParseException ignored) { }
            try {
                return LocalDate.parse(dateStr, FORMATTER_SLASH);
            } catch (DateTimeParseException e) {
                throw new IOException(
                    "Formato de fecha inválido: '" + dateStr + "'. " +
                    "Use el formato yyyy-MM-dd (Ej: 2005-05-24) o yyyy/MM/dd (Ej: 2005/05/24)"
                );
            }
        }
    }

    public static class BigDecimalTwoDecimalsSerializer extends StdSerializer<BigDecimal> {

        public BigDecimalTwoDecimalsSerializer() {
            super(BigDecimal.class);
        }

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
              
                gen.writeRawValue(value.setScale(2, RoundingMode.HALF_UP).toPlainString());
            }
        }
    }

   
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.deserializerByType(LocalDate.class, new FlexibleLocalDateDeserializer());
            builder.serializerByType(BigDecimal.class, new BigDecimalTwoDecimalsSerializer());
        };
    }
}
