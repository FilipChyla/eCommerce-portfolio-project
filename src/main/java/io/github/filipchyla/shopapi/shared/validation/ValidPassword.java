package io.github.filipchyla.shopapi.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordConstraintValidator.class)
public @interface ValidPassword {

    String message() default "Password must be at least 8 characters long and contain " +
            "an uppercase letter, a lowercase letter, a digit and a special character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
