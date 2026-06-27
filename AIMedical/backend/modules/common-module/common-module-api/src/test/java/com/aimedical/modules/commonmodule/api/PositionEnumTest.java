package com.aimedical.modules.commonmodule.api;

import com.aimedical.common.base.BaseEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionEnumTest {

    @Test
    void shouldDefineFivePositions() {
        assertEquals(5, PositionEnum.values().length);
        assertNotNull(PositionEnum.valueOf("OUTPATIENT"));
        assertNotNull(PositionEnum.valueOf("EXAMINATION"));
        assertNotNull(PositionEnum.valueOf("LABTEST"));
        assertNotNull(PositionEnum.valueOf("PHARMACY"));
        assertNotNull(PositionEnum.valueOf("RECEPTION"));
    }

    @Test
    void shouldReturnCodeForOutpatient() {
        assertEquals("OUTPATIENT", PositionEnum.OUTPATIENT.getCode());
    }

    @Test
    void shouldReturnCodeForExamination() {
        assertEquals("EXAMINATION", PositionEnum.EXAMINATION.getCode());
    }

    @Test
    void shouldReturnCodeForLabTest() {
        assertEquals("LABTEST", PositionEnum.LABTEST.getCode());
    }

    @Test
    void shouldReturnCodeForPharmacy() {
        assertEquals("PHARMACY", PositionEnum.PHARMACY.getCode());
    }

    @Test
    void shouldReturnCodeForReception() {
        assertEquals("RECEPTION", PositionEnum.RECEPTION.getCode());
    }

    @Test
    void shouldReturnDescForOutpatient() {
        assertEquals("门诊医生", PositionEnum.OUTPATIENT.getDesc());
    }

    @Test
    void shouldReturnDescForExamination() {
        assertEquals("检查医生", PositionEnum.EXAMINATION.getDesc());
    }

    @Test
    void shouldReturnDescForLabTest() {
        assertEquals("检验医生", PositionEnum.LABTEST.getDesc());
    }

    @Test
    void shouldReturnDescForPharmacy() {
        assertEquals("药房医生", PositionEnum.PHARMACY.getDesc());
    }

    @Test
    void shouldReturnDescForReception() {
        assertEquals("线下接诊医生", PositionEnum.RECEPTION.getDesc());
    }

    @Test
    void shouldImplementBaseEnum() {
        for (PositionEnum position : PositionEnum.values()) {
            assertInstanceOf(BaseEnum.class, position);
        }
    }
}