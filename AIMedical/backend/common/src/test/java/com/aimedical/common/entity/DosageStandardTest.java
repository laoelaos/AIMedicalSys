package com.aimedical.common.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DosageStandardTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void shouldCreateWithDefaultValues() {
        DosageStandard ds = new DosageStandard();
        assertNull(ds.getId());
        assertNull(ds.getCreatedAt());
        assertNull(ds.getUpdatedAt());
        assertFalse(ds.getDeleted());
        assertNull(ds.getDrugCode());
        assertNull(ds.getRouteOfAdministration());
        assertNull(ds.getAgeRangeStart());
        assertNull(ds.getAgeRangeEnd());
        assertNull(ds.getWeightRangeStart());
        assertNull(ds.getWeightRangeEnd());
        assertNull(ds.getSingleMax());
        assertNull(ds.getDailyMax());
        assertNull(ds.getUnit());
    }

    @Test
    void shouldSetAndGetDrugCode() {
        DosageStandard ds = new DosageStandard();
        ds.setDrugCode("H10983001");
        assertEquals("H10983001", ds.getDrugCode());
    }

    @Test
    void shouldSetAndGetRouteOfAdministration() {
        DosageStandard ds = new DosageStandard();
        ds.setRouteOfAdministration("oral");
        assertEquals("oral", ds.getRouteOfAdministration());
    }

    @Test
    void shouldSetAndGetAgeRangeStart() {
        DosageStandard ds = new DosageStandard();
        ds.setAgeRangeStart(18);
        assertEquals(18, ds.getAgeRangeStart());
    }

    @Test
    void shouldSetAndGetAgeRangeEnd() {
        DosageStandard ds = new DosageStandard();
        ds.setAgeRangeEnd(65);
        assertEquals(65, ds.getAgeRangeEnd());
    }

    @Test
    void shouldSetAndGetWeightRangeStart() {
        DosageStandard ds = new DosageStandard();
        ds.setWeightRangeStart(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("50.00"), ds.getWeightRangeStart());
    }

    @Test
    void shouldSetAndGetWeightRangeEnd() {
        DosageStandard ds = new DosageStandard();
        ds.setWeightRangeEnd(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), ds.getWeightRangeEnd());
    }

    @Test
    void shouldSetAndGetSingleMax() {
        DosageStandard ds = new DosageStandard();
        ds.setSingleMax(new BigDecimal("500.000"));
        assertEquals(new BigDecimal("500.000"), ds.getSingleMax());
    }

    @Test
    void shouldSetAndGetDailyMax() {
        DosageStandard ds = new DosageStandard();
        ds.setDailyMax(new BigDecimal("2000.000"));
        assertEquals(new BigDecimal("2000.000"), ds.getDailyMax());
    }

    @Test
    void shouldSetAndGetUnit() {
        DosageStandard ds = new DosageStandard();
        ds.setUnit("mg");
        assertEquals("mg", ds.getUnit());
    }

    @Test
    void shouldDefaultAgeRangeToNull() {
        DosageStandard ds = new DosageStandard();
        assertNull(ds.getAgeRangeStart());
        assertNull(ds.getAgeRangeEnd());
    }

    @Test
    void shouldDefaultWeightRangeToNull() {
        DosageStandard ds = new DosageStandard();
        assertNull(ds.getWeightRangeStart());
        assertNull(ds.getWeightRangeEnd());
    }

    @Test
    void shouldDefaultDailyMaxToNull() {
        DosageStandard ds = new DosageStandard();
        assertNull(ds.getDailyMax());
    }

    @Test
    void shouldRejectBlankDrugCode() {
        DosageStandard ds = validDosageStandard();
        ds.setDrugCode("");
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("drugCode")));
    }

    @Test
    void shouldRejectBlankRouteOfAdministration() {
        DosageStandard ds = validDosageStandard();
        ds.setRouteOfAdministration(" ");
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("routeOfAdministration")));
    }

    @Test
    void shouldRejectNullSingleMax() {
        DosageStandard ds = validDosageStandard();
        ds.setSingleMax(null);
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("singleMax")));
    }

    @Test
    void shouldRejectZeroSingleMax() {
        DosageStandard ds = validDosageStandard();
        ds.setSingleMax(BigDecimal.ZERO);
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("singleMax")));
    }

    @Test
    void shouldRejectNegativeSingleMax() {
        DosageStandard ds = validDosageStandard();
        ds.setSingleMax(new BigDecimal("-1.000"));
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("singleMax")));
    }

    @Test
    void shouldRejectBlankUnit() {
        DosageStandard ds = validDosageStandard();
        ds.setUnit(" ");
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("unit")));
    }

    @Test
    void shouldRejectAllRequiredFieldsMissing() {
        DosageStandard ds = new DosageStandard();
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("drugCode")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("routeOfAdministration")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("singleMax")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("unit")));
    }

    @Test
    void shouldPassValidationWithRequiredFieldsOnly() {
        DosageStandard ds = validDosageStandard();
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldPassValidationWithAllFields() {
        DosageStandard ds = validDosageStandard();
        ds.setAgeRangeStart(18);
        ds.setAgeRangeEnd(65);
        ds.setWeightRangeStart(new BigDecimal("50.00"));
        ds.setWeightRangeEnd(new BigDecimal("100.00"));
        ds.setDailyMax(new BigDecimal("2000.000"));
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldAcceptNullDailyMaxAsValid() {
        DosageStandard ds = validDosageStandard();
        ds.setDailyMax(null);
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldAcceptNullAgeRangeAsValid() {
        DosageStandard ds = validDosageStandard();
        ds.setAgeRangeStart(null);
        ds.setAgeRangeEnd(null);
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldAcceptNullWeightRangeAsValid() {
        DosageStandard ds = validDosageStandard();
        ds.setWeightRangeStart(null);
        ds.setWeightRangeEnd(null);
        Set<ConstraintViolation<DosageStandard>> violations = validator.validate(ds);
        assertTrue(violations.isEmpty());
    }

    private static DosageStandard validDosageStandard() {
        DosageStandard ds = new DosageStandard();
        ds.setDrugCode("H10983001");
        ds.setRouteOfAdministration("oral");
        ds.setSingleMax(new BigDecimal("500.000"));
        ds.setUnit("mg");
        return ds;
    }
}
