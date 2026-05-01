package br.com.dealership.dealershibff.validation;

import br.com.dealership.dealershibff.web.InputSanitizationFilter;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<ValidCpf, String> {

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null handled by @NotBlank
        }
        final String digits = value.replaceAll("[^0-9]", "");
        return InputSanitizationFilter.isValidCpf(digits);
    }
}
