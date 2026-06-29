package com.aimedical.modules.admin.entity.dict;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DictTypeTest {

    @Test
    void shouldHandleAllFieldsViaGettersAndSetters() {
        DictType entity = new DictType();

        entity.setDictName("User Sex");
        entity.setDictType("sys_user_sex");
        entity.setStatus(true);
        entity.setDictDataList(List.of(new DictData()));
        entity.setRemark("user sex dictionary");

        assertEquals("User Sex", entity.getDictName());
        assertEquals("sys_user_sex", entity.getDictType());
        assertTrue(entity.getStatus());
        assertEquals(1, entity.getDictDataList().size());
        assertEquals("user sex dictionary", entity.getRemark());
    }

    @Test
    void shouldProvideDefaultDictDataList() {
        DictType entity = new DictType();
        assertNotNull(entity.getDictDataList());
        assertTrue(entity.getDictDataList().isEmpty());
    }

    @Test
    void shouldSupportEqualsAcrossAllBranches() {
        DictType a = new DictType();
        a.setDictType("dict1");

        DictType same = new DictType();
        same.setDictType("dict1");

        DictType different = new DictType();
        different.setDictType("dict2");

        assertTrue(a.equals(a));
        assertTrue(a.equals(same));
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(different));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a DictType"));
    }

    @Test
    void shouldGenerateNonEmptyToString() {
        DictType entity = new DictType();
        entity.setDictName("My Dictionary");
        assertTrue(entity.toString().contains("My Dictionary"));
    }

    @Test
    void shouldInvokeHashCodeWithAllFieldsNull() {
        DictType entity = new DictType();
        entity.hashCode();
    }

    @Test
    void shouldInvokeHashCodeWithAllFieldsSet() {
        DictType entity = new DictType();
        entity.setDictName("X");
        entity.setDictType("t");
        entity.setStatus(true);
        entity.setDictDataList(List.of());
        entity.setRemark("r");
        entity.hashCode();
    }
}