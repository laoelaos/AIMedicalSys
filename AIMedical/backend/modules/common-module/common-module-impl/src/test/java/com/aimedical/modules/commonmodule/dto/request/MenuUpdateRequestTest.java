package com.aimedical.modules.commonmodule.dto.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MenuUpdateRequest")
class MenuUpdateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("getter/setter 正常读写")
    void shouldReadWriteViaGetterSetter() {
        var request = new MenuUpdateRequest();
        request.setId(1L);
        request.setName("test");
        request.setPermission("menu:test");
        request.setParentId(2L);
        request.setPath("/test");
        request.setComponent("TestComp");
        request.setIcon("icon");
        request.setSort(1);
        request.setVisible(true);

        assertEquals(1L, request.getId());
        assertEquals("test", request.getName());
        assertEquals("menu:test", request.getPermission());
        assertEquals(2L, request.getParentId());
        assertEquals("/test", request.getPath());
        assertEquals("TestComp", request.getComponent());
        assertEquals("icon", request.getIcon());
        assertEquals(1, request.getSort());
        assertTrue(request.getVisible());
    }

    @Test
    @DisplayName("新建对象所有字段为 null")
    void shouldHaveAllNullFieldsByDefault() {
        var request = new MenuUpdateRequest();
        assertNull(request.getId());
        assertNull(request.getName());
        assertNull(request.getPermission());
        assertNull(request.getParentId());
        assertNull(request.getPath());
        assertNull(request.getComponent());
        assertNull(request.getIcon());
        assertNull(request.getSort());
        assertNull(request.getVisible());
    }

    @Test
    @DisplayName("JSON 序列化时排除 null 字段")
    void shouldExcludeNullFieldsInJson() throws JsonProcessingException {
        var request = new MenuUpdateRequest();
        request.setName("test");

        String json = objectMapper.writeValueAsString(request);
        assertTrue(json.contains("\"name\""));
        assertFalse(json.contains("\"id\""));
        assertFalse(json.contains("\"permission\""));
        assertFalse(json.contains("\"path\""));
    }

    @Test
    @DisplayName("JSON 序列化包含有值的字段")
    void shouldIncludeNonNullFieldsInJson() throws JsonProcessingException {
        var request = new MenuUpdateRequest();
        request.setName("test");
        request.setSort(5);
        request.setVisible(false);

        String json = objectMapper.writeValueAsString(request);
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"sort\""));
        assertTrue(json.contains("\"visible\""));
    }
}
