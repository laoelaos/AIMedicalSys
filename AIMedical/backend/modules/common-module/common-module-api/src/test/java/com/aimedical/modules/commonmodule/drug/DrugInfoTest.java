package com.aimedical.modules.commonmodule.drug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrugInfoTest {

    @Test
    void shouldExposeDrugCode() {
        DrugInfo info = new DrugInfo("D001", "Paracetamol", "500mg", "Tablet", "Bayer", "Box");
        assertEquals("D001", info.drugCode());
    }

    @Test
    void shouldExposeDrugName() {
        DrugInfo info = new DrugInfo("D001", "Paracetamol", "500mg", "Tablet", "Bayer", "Box");
        assertEquals("Paracetamol", info.drugName());
    }

    @Test
    void shouldExposeSpecification() {
        DrugInfo info = new DrugInfo("D001", "Paracetamol", "500mg", "Tablet", "Bayer", "Box");
        assertEquals("500mg", info.specification());
    }

    @Test
    void shouldExposeDosageForm() {
        DrugInfo info = new DrugInfo("D001", "Paracetamol", "500mg", "Tablet", "Bayer", "Box");
        assertEquals("Tablet", info.dosageForm());
    }

    @Test
    void shouldExposeManufacturer() {
        DrugInfo info = new DrugInfo("D001", "Paracetamol", "500mg", "Tablet", "Bayer", "Box");
        assertEquals("Bayer", info.manufacturer());
    }

    @Test
    void shouldExposePackageUnit() {
        DrugInfo info = new DrugInfo("D001", "Paracetamol", "500mg", "Tablet", "Bayer", "Box");
        assertEquals("Box", info.packageUnit());
    }

    @Test
    void shouldAllowNullFields() {
        DrugInfo info = new DrugInfo(null, null, null, null, null, null);
        assertNull(info.drugCode());
        assertNull(info.drugName());
        assertNull(info.specification());
        assertNull(info.dosageForm());
        assertNull(info.manufacturer());
        assertNull(info.packageUnit());
    }

    @Test
    void shouldImplementEquality() {
        DrugInfo d1 = new DrugInfo("D001", "Paracetamol", "500mg", "Tablet", "Bayer", "Box");
        DrugInfo d2 = new DrugInfo("D001", "Paracetamol", "500mg", "Tablet", "Bayer", "Box");
        assertEquals(d1, d2);
    }

    @Test
    void shouldImplementInequality() {
        DrugInfo d1 = new DrugInfo("D001", "Paracetamol", "500mg", "Tablet", "Bayer", "Box");
        DrugInfo d2 = new DrugInfo("D002", "Ibuprofen", "200mg", "Capsule", "Pfizer", "Strip");
        assertNotEquals(d1, d2);
    }
}
