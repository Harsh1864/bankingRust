package io.github.hossensyedriadh.cbs.mandate.api.validation;

import io.github.hossensyedriadh.cbs.mandate.internal.validator.OpenMandateValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = OpenMandateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOpenMandate {
    String message() default "Mandate is not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
