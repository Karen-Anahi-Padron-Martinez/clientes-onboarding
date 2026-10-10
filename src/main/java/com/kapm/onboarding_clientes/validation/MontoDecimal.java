package com.kapm.onboarding_clientes.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;


@Documented
@Constraint(validatedBy = MontoDecimalValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MontoDecimal {

    String message() default "El monto debe tener exactamente 2 decimales (Ej: 56.00)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
