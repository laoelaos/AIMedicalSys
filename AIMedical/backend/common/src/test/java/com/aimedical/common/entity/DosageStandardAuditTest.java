package com.aimedical.common.entity;

import com.aimedical.common.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
class DosageStandardAuditTest {

    @SpringBootApplication
    static class TestConfig {}

    @Autowired
    private TestEntityManager em;

    @Test
    void shouldPersistAndAutoFillTimestamps() {
        DosageStandard ds = new DosageStandard();
        ds.setDrugCode("H10983001");
        ds.setRouteOfAdministration("oral");
        ds.setSingleMax(new BigDecimal("500.000"));
        ds.setUnit("mg");

        em.persistAndFlush(ds);

        assertNotNull(ds.getId());
        assertNotNull(ds.getCreatedAt());
        assertNotNull(ds.getUpdatedAt());
    }

    @Test
    void shouldRoundTripAllFields() {
        DosageStandard ds = new DosageStandard();
        ds.setDrugCode("H10983001");
        ds.setRouteOfAdministration("iv");
        ds.setAgeRangeStart(18);
        ds.setAgeRangeEnd(65);
        ds.setWeightRangeStart(new BigDecimal("50.00"));
        ds.setWeightRangeEnd(new BigDecimal("100.00"));
        ds.setSingleMax(new BigDecimal("1000.000"));
        ds.setDailyMax(new BigDecimal("4000.000"));
        ds.setUnit("mg");

        em.persistAndFlush(ds);
        DosageStandard found = em.find(DosageStandard.class, ds.getId());

        assertEquals("H10983001", found.getDrugCode());
        assertEquals("iv", found.getRouteOfAdministration());
        assertEquals(18, found.getAgeRangeStart());
        assertEquals(65, found.getAgeRangeEnd());
        assertEquals(0, new BigDecimal("50.00").compareTo(found.getWeightRangeStart()));
        assertEquals(0, new BigDecimal("100.00").compareTo(found.getWeightRangeEnd()));
        assertEquals(0, new BigDecimal("1000.000").compareTo(found.getSingleMax()));
        assertEquals(0, new BigDecimal("4000.000").compareTo(found.getDailyMax()));
        assertEquals("mg", found.getUnit());
    }

    @Test
    void shouldPersistWithNullOptionalFields() {
        DosageStandard ds = new DosageStandard();
        ds.setDrugCode("H10983002");
        ds.setRouteOfAdministration("oral");
        ds.setSingleMax(new BigDecimal("250.000"));
        ds.setUnit("mg");

        em.persistAndFlush(ds);
        DosageStandard found = em.find(DosageStandard.class, ds.getId());

        assertNull(found.getAgeRangeStart());
        assertNull(found.getAgeRangeEnd());
        assertNull(found.getWeightRangeStart());
        assertNull(found.getWeightRangeEnd());
        assertNull(found.getDailyMax());
    }

    @Test
    void shouldUpdateUpdatedAtOnModification() {
        DosageStandard ds = new DosageStandard();
        ds.setDrugCode("H10983001");
        ds.setRouteOfAdministration("oral");
        ds.setSingleMax(new BigDecimal("500.000"));
        ds.setUnit("mg");

        em.persistAndFlush(ds);
        LocalDateTime initialUpdatedAt = ds.getUpdatedAt();

        ds.setSingleMax(new BigDecimal("600.000"));
        em.persistAndFlush(ds);

        assertNotNull(ds.getUpdatedAt());
    }

    @Test
    void shouldAssignDistinctIds() {
        DosageStandard ds1 = new DosageStandard();
        ds1.setDrugCode("H10983001");
        ds1.setRouteOfAdministration("oral");
        ds1.setSingleMax(new BigDecimal("500.000"));
        ds1.setUnit("mg");

        DosageStandard ds2 = new DosageStandard();
        ds2.setDrugCode("H10983001");
        ds2.setRouteOfAdministration("iv");
        ds2.setSingleMax(new BigDecimal("1000.000"));
        ds2.setUnit("mg");

        em.persistAndFlush(ds1);
        em.persistAndFlush(ds2);

        assertNotNull(ds1.getId());
        assertNotNull(ds2.getId());
        assertNotEquals(ds1.getId(), ds2.getId());
    }

    @Test
    void shouldRejectPersistWithMissingRequiredFields() {
        DosageStandard ds = new DosageStandard();
        assertThrows(Exception.class, () -> em.persistAndFlush(ds));
    }
}
