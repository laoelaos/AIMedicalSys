package com.aimedical.common.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseEnumTest {

    private enum TestStatus implements BaseEnum {
        ACTIVE("1", "启用"),
        INACTIVE("0", "禁用");

        private final String code;
        private final String desc;

        TestStatus(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getDesc() {
            return desc;
        }
    }

    @Test
    void shouldReturnCode() {
        assertEquals("1", TestStatus.ACTIVE.getCode());
        assertEquals("0", TestStatus.INACTIVE.getCode());
    }

    @Test
    void shouldReturnDesc() {
        assertEquals("启用", TestStatus.ACTIVE.getDesc());
        assertEquals("禁用", TestStatus.INACTIVE.getDesc());
    }
}
