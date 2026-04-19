package br.com.dealership.clientapi.dto.request;

import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.InvalidStateException;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCpf.Validator.class)
@Documented
public @interface ValidCpf {

    String message() default "invalid CPF";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidCpf, String> {

        private CPFValidator cpfValidator;

        @Override
        public void initialize(ValidCpf annotation) {
            cpfValidator = new CPFValidator(error -> null, true, false);
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true; // @NotBlank handles null/blank
            }
            try {
                cpfValidator.assertValid(value);
                return true;
            } catch (InvalidStateException _) {
                return false;
            }
        }
    }
}
