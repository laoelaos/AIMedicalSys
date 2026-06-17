package com.aimedical.modules.commonmodule.api;

import com.aimedical.common.base.BaseEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTypeTest {

    @Test
    void shouldDefineThreeConstants() {
        assertEquals(3, UserType.values().length);
        assertNotNull(UserType.valueOf("DOCTOR"));
        assertNotNull(UserType.valueOf("PATIENT"));
        assertNotNull(UserType.valueOf("ADMIN"));
    }

    @Test
    void shouldReturnCodeForDoctor() {
        assertEquals("DOCTOR", UserType.DOCTOR.getCode());
    }

    @Test
    void shouldReturnDescForDoctor() {
        assertEquals("医生", UserType.DOCTOR.getDesc());
    }

    @Test
    void shouldReturnCodeForPatient() {
        assertEquals("PATIENT", UserType.PATIENT.getCode());
    }

    @Test
    void shouldReturnDescForPatient() {
        assertEquals("患者", UserType.PATIENT.getDesc());
    }

    @Test
    void shouldReturnCodeForAdmin() {
        assertEquals("ADMIN", UserType.ADMIN.getCode());
    }

    @Test
    void shouldReturnDescForAdmin() {
        assertEquals("管理员", UserType.ADMIN.getDesc());
    }

    @Test
    void shouldImplementBaseEnum() {
        assertInstanceOf(BaseEnum.class, UserType.DOCTOR);
        assertInstanceOf(BaseEnum.class, UserType.PATIENT);
        assertInstanceOf(BaseEnum.class, UserType.ADMIN);
    }
}
