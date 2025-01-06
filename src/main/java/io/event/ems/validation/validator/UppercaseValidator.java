package io.event.ems.validation.validator;

import io.event.ems.validation.annotation.Uppercase;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UppercaseValidator implements ConstraintValidator<Uppercase, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null){
            return true;
        }
        return value.equals(value.toUpperCase());
    }

}
