package br.com.dealership.dealershibff.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfValidatorTest {

    private CpfValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CpfValidator();
    }

    @Test
    void shouldAcceptNullValueAsValid() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void shouldAcceptKnownValidCpf() {
        assertTrue(validator.isValid("529.982.247-25", null));
    }

    @Test
    void shouldRejectInvalidCpf() {
        assertFalse(validator.isValid("000.000.000-00", null));
    }
}
