package io.event.ems.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.event.ems.validation.validator.UppercaseValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UppercaseValidator.class)
@Documented
public @interface Uppercase {

    String message() default "Must be uppercase";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
