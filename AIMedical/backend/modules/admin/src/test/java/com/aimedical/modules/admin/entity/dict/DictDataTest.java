package com.aimedical.modules.admin.entity.dict;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DictDataTest {

    @Test
    void shouldHandleAllFieldsViaGettersAndSetters() {
        DictData entity = new DictData();
        DictType type = new DictType();
        type.setDictType("sys_user_sex");

        entity.setDictSort(1);
        entity.setDictLabel("Male");
        entity.setDictValue("1");
        entity.setDictType(type);
        entity.setCssClass("tag-primary");
        entity.setListClass("default");
        entity.setIsDefault(true);
        entity.setStatus(true);
        entity.setRemark("default male option");

        assertEquals(1, entity.getDictSort());
        assertEquals("Male", entity.getDictLabel());
        assertEquals("1", entity.getDictValue());
        assertSame(type, entity.getDictType());
        assertEquals("tag-primary", entity.getCssClass());
        assertEquals("default", entity.getListClass());
        assertTrue(entity.getIsDefault());
        assertTrue(entity.getStatus());
        assertEquals("default male option", entity.getRemark());
    }

    @Test
    void shouldSupportEqualsAcrossAllBranches() {
        DictData a = new DictData();
        a.setDictLabel("Active");

        DictData same = new DictData();
        same.setDictLabel("Active");

        DictData different = new DictData();
        different.setDictLabel("Inactive");

        assertTrue(a.equals(a));
        assertTrue(a.equals(same));
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(different));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a DictData"));
    }

    @Test
    void shouldGenerateNonEmptyToString() {
        DictData entity = new DictData();
        entity.setDictLabel("Female");
        assertTrue(entity.toString().contains("Female"));
    }

    @Test
    void shouldInvokeHashCodeWithAllFieldsNull() {
        DictData entity = new DictData();
        entity.hashCode();
    }

    @Test
    void shouldInvokeHashCodeWithAllFieldsSet() {
        DictData entity = new DictData();
        entity.setDictSort(1);
        entity.setDictLabel("X");
        entity.setDictValue("x");
        entity.setDictType(new DictType());
        entity.setCssClass("c");
        entity.setListClass("l");
        entity.setIsDefault(false);
        entity.setStatus(true);
        entity.setRemark("r");
        entity.hashCode();
    }
}