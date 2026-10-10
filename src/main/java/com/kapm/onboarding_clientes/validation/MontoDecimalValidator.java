package com.kapm.onboarding_clientes.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;


public class MontoDecimalValidator implements ConstraintValidator<MontoDecimal, BigDecimal> {

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
      
        return value.scale() == 2;
    }
}
