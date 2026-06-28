package com.aimedical.modules.doctor.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DoctorEntityTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DoctorEntity entity = new DoctorEntity();
        assertNotNull(entity);
    }

    @Test
    void shouldExtendBaseEntity() {
        DoctorEntity entity = new DoctorEntity();
        assertNull(entity.getId());
        assertFalse(entity.getDeleted());
    }

    @Test
    void shouldHandleAllFieldsViaGettersAndSetters() {
        DoctorEntity entity = new DoctorEntity();
        BigDecimal fee = new BigDecimal("100.00");

        entity.setUserId(2001L);
        entity.setRealName("Dr. Wang");
        entity.setTitle("Chief Physician");
        entity.setDepartment("Cardiology");
        entity.setConsultationFee(fee);

        assertEquals(2001L, entity.getUserId());
        assertEquals("Dr. Wang", entity.getRealName());
        assertEquals("Chief Physician", entity.getTitle());
        assertEquals("Cardiology", entity.getDepartment());
        assertEquals(fee, entity.getConsultationFee());
    }

    @Test
    void shouldSupportEqualsAcrossAllBranches() {
        DoctorEntity a = new DoctorEntity();
        a.setUserId(1L);
        a.setRealName("Dr. A");

        DoctorEntity same = new DoctorEntity();
        same.setUserId(1L);
        same.setRealName("Dr. A");

        DoctorEntity different = new DoctorEntity();
        different.setUserId(2L);
        different.setRealName("Dr. B");

        assertTrue(a.equals(a));
        assertTrue(a.equals(same));
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(different));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a DoctorEntity"));
    }

    @Test
    void shouldComputeHashCodeWithAllFieldsNull() {
        DoctorEntity entity = new DoctorEntity();
        entity.hashCode();
    }

    @Test
    void shouldComputeHashCodeWithAllFieldsSet() {
        DoctorEntity entity = new DoctorEntity();
        entity.setUserId(1L);
        entity.setRealName("Dr. Wang");
        entity.setTitle("Chief");
        entity.setDepartment("Cardiology");
        entity.setConsultationFee(new BigDecimal("100.00"));
        entity.hashCode();
    }

    @Test
    void shouldComputeHashCodeWithMixedNullAndSetFields() {
        DoctorEntity a = new DoctorEntity();
        a.setUserId(1L);
        a.hashCode();

        DoctorEntity b = new DoctorEntity();
        b.setRealName("Dr. Liu");
        b.hashCode();

        DoctorEntity c = new DoctorEntity();
        c.setTitle("Attending");
        c.setDepartment("Surgery");
        c.hashCode();

        DoctorEntity d = new DoctorEntity();
        d.setConsultationFee(new BigDecimal("50.00"));
        d.hashCode();
    }

    @Test
    void shouldInvokeCanEqualForAllBranches() {
        DoctorEntity entity = new DoctorEntity();
        entity.canEqual(entity);
        entity.canEqual("not a DoctorEntity");
        entity.canEqual(null);
    }

    @Test
    void shouldGenerateNonEmptyToString() {
        DoctorEntity entity = new DoctorEntity();
        entity.setRealName("Dr. Liu");
        assertTrue(entity.toString().contains("Dr. Liu"));
    }
}