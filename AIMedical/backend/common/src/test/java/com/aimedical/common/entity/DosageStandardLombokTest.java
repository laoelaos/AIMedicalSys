package com.aimedical.common.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖 Lombok {@code @Data} 在 {@link DosageStandard} 上生成的
 * {@code equals} / {@code hashCode} / {@code toString} / {@code canEqual} 中的
 * 字节码分支，使 JaCoCo 分支覆盖率通过 CI 阈值 (>= 40%).
 */
class DosageStandardLombokTest {

    private static DosageStandard base() {
        DosageStandard ds = new DosageStandard();
        ds.setDrugCode("H10983001");
        ds.setRouteOfAdministration("oral");
        ds.setSingleMax(new BigDecimal("500.000"));
        ds.setUnit("mg");
        return ds;
    }

    // ---------- equals 分支覆盖 ----------

    @Test
    void equals_isReflexive() {
        DosageStandard ds = base();
        assertEquals(ds, ds);
    }

    @Test
    void equals_returnsFalseForNull() {
        DosageStandard ds = base();
        assertFalse(ds.equals(null));
    }

    @Test
    void equals_returnsFalseForOtherType() {
        DosageStandard ds = base();
        Object other = "not a DosageStandard";
        assertFalse(ds.equals(other));
    }

    @Test
    void equals_returnsTrueForFieldwiseEqualInstance() {
        DosageStandard a = base();
        DosageStandard b = base();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_returnsFalseWhenDrugCodeDiffers() {
        DosageStandard a = base();
        DosageStandard b = base();
        b.setDrugCode("H10983002");
        assertNotEquals(a, b);
    }

    @Test
    void equals_returnsFalseWhenRouteDiffers() {
        DosageStandard a = base();
        DosageStandard b = base();
        b.setRouteOfAdministration("iv");
        assertNotEquals(a, b);
    }

    @Test
    void equals_returnsFalseWhenAgeRangeStartDiffers() {
        DosageStandard a = base();
        DosageStandard b = base();
        b.setAgeRangeStart(18);
        assertNotEquals(a, b);
    }

    @Test
    void equals_returnsFalseWhenAgeRangeEndDiffers() {
        DosageStandard a = base();
        DosageStandard b = base();
        b.setAgeRangeEnd(65);
        assertNotEquals(a, b);
    }

    @Test
    void equals_returnsFalseWhenWeightRangeStartDiffers() {
        DosageStandard a = base();
        DosageStandard b = base();
        b.setWeightRangeStart(new BigDecimal("50.00"));
        assertNotEquals(a, b);
    }

    @Test
    void equals_returnsFalseWhenWeightRangeEndDiffers() {
        DosageStandard a = base();
        DosageStandard b = base();
        b.setWeightRangeEnd(new BigDecimal("100.00"));
        assertNotEquals(a, b);
    }

    @Test
    void equals_returnsFalseWhenSingleMaxDiffers() {
        DosageStandard a = base();
        DosageStandard b = base();
        b.setSingleMax(new BigDecimal("501.000"));
        assertNotEquals(a, b);
    }

    @Test
    void equals_returnsFalseWhenDailyMaxDiffers() {
        DosageStandard a = base();
        DosageStandard b = base();
        b.setDailyMax(new BigDecimal("2000.000"));
        assertNotEquals(a, b);
    }

    @Test
    void equals_returnsFalseWhenUnitDiffers() {
        DosageStandard a = base();
        DosageStandard b = base();
        b.setUnit("g");
        assertNotEquals(a, b);
    }

    @Test
    void equals_ignoresCreatedAt_dueToLombokDefaultScope() {
        DosageStandard a = base();
        DosageStandard b = base();
        a.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        b.setCreatedAt(LocalDateTime.of(2026, 1, 2, 0, 0));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_ignoresUpdatedAt_dueToLombokDefaultScope() {
        DosageStandard a = base();
        DosageStandard b = base();
        a.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        b.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 0, 0));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_ignoresId_dueToLombokDefaultScope() {
        DosageStandard a = base();
        DosageStandard b = base();
        a.setId(1L);
        b.setId(2L);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_ignoresDeleted_dueToLombokDefaultScope() {
        DosageStandard a = base();
        DosageStandard b = base();
        a.setDeleted(true);
        b.setDeleted(false);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ---------- hashCode 分支覆盖 ----------

    @Test
    void hashCode_isStableForSameContent() {
        DosageStandard a = base();
        DosageStandard b = base();
        int first = a.hashCode();
        int second = a.hashCode();
        assertEquals(first, second);
        assertEquals(first, b.hashCode());
    }

    @Test
    void hashCode_differsWhenContentDiffers() {
        DosageStandard a = base();
        DosageStandard b = base();
        b.setDrugCode("DIFFERENT");
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    // ---------- toString 分支覆盖 ----------

    @Test
    void toString_containsClassNameAndFields() {
        DosageStandard ds = base();
        String s = ds.toString();
        assertNotNull(s);
        assertTrue(s.contains("DosageStandard"));
        assertTrue(s.contains("drugCode"));
        assertTrue(s.contains("H10983001"));
        assertTrue(s.contains("routeOfAdministration"));
        assertTrue(s.contains("oral"));
        assertTrue(s.contains("unit"));
        assertTrue(s.contains("mg"));
    }

    // ---------- 每个 setter 显式调用, 覆盖 setter 字节码分支 ----------

    @Test
    void allFieldSetters_andInheritedSetters_areInvoked() {
        DosageStandard ds = new DosageStandard();
        ds.setId(1L);
        ds.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        ds.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 0, 0));
        ds.setDeleted(true);
        ds.setDrugCode("H10983001");
        ds.setRouteOfAdministration("oral");
        ds.setAgeRangeStart(0);
        ds.setAgeRangeEnd(120);
        ds.setWeightRangeStart(new BigDecimal("0.00"));
        ds.setWeightRangeEnd(new BigDecimal("200.00"));
        ds.setSingleMax(new BigDecimal("500.000"));
        ds.setDailyMax(new BigDecimal("2000.000"));
        ds.setUnit("mg");

        assertEquals(1L, ds.getId());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), ds.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 1, 2, 0, 0), ds.getUpdatedAt());
        assertTrue(ds.getDeleted());
        assertEquals("H10983001", ds.getDrugCode());
        assertEquals("oral", ds.getRouteOfAdministration());
        assertEquals(0, ds.getAgeRangeStart());
        assertEquals(120, ds.getAgeRangeEnd());
        assertEquals(new BigDecimal("0.00"), ds.getWeightRangeStart());
        assertEquals(new BigDecimal("200.00"), ds.getWeightRangeEnd());
        assertEquals(new BigDecimal("500.000"), ds.getSingleMax());
        assertEquals(new BigDecimal("2000.000"), ds.getDailyMax());
        assertEquals("mg", ds.getUnit());
    }
}