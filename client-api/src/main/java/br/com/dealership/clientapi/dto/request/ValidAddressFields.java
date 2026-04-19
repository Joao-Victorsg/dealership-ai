package br.com.dealership.clientapi.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidAddressFields.Validator.class)
@Documented
public @interface ValidAddressFields {

    String message() default "postcode and streetNumber must both be provided or both be absent";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidAddressFields, UpdateClientRequest> {

        @Override
        public boolean isValid(UpdateClientRequest request, ConstraintValidatorContext context) {
            if (request == null) {
                return true;
            }
            boolean hasPostcode = request.postcode() != null && !request.postcode().isBlank();
            boolean hasStreetNumber = request.streetNumber() != null && !request.streetNumber().isBlank();
            return hasPostcode == hasStreetNumber;
        }
    }
}
