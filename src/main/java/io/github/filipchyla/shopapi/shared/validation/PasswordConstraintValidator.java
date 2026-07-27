package io.github.filipchyla.shopapi.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return false;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;

        if (password.length() < 8) {
            context.buildConstraintViolationWithTemplate("Password must be at least 8 characters")
                    .addConstraintViolation();
            valid = false;
        }
        if (!password.matches(".*[A-Z].*")) {
            context.buildConstraintViolationWithTemplate("Password must contain an uppercase letter")
                    .addConstraintViolation();
            valid = false;
        }
        if (!password.matches(".*[a-z].*")) {
            context.buildConstraintViolationWithTemplate("Password must contain a lowercase letter")
                    .addConstraintViolation();
            valid = false;
        }
        if (!password.matches(".*\\d.*")) {
            context.buildConstraintViolationWithTemplate("Password must contain a digit")
                    .addConstraintViolation();
            valid = false;
        }
        if (!password.matches(".*[@#$%^&+=!].*")) {
            context.buildConstraintViolationWithTemplate("Password must contain a special character")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
