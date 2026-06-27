package com.aimedical.modules.commonmodule.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MenuResponse")
class MenuResponseTest {

    @Test
    @DisplayName("构造并访问字段")
    void shouldConstructAndAccessFields() {
        var response = new MenuResponse(1L, "仪表盘", "/dashboard", "DashboardComp",
                "dashboard-icon", "menu:dashboard", 0, null);

        assertEquals(1L, response.id());
        assertEquals("仪表盘", response.name());
        assertEquals("/dashboard", response.path());
        assertEquals("DashboardComp", response.component());
        assertEquals("dashboard-icon", response.icon());
        assertEquals("menu:dashboard", response.permission());
        assertEquals(0, response.sort());
        assertNull(response.children());
    }

    @Test
    @DisplayName("children 可为 null（叶子节点）")
    void shouldAcceptNullChildren() {
        var response = new MenuResponse(1L, "n", "/p", null, null, "p", 0, null);
        assertNull(response.children());
    }

    @Test
    @DisplayName("children 可为非空列表（父节点）")
    void shouldAcceptNonEmptyChildren() {
        var child = new MenuResponse(2L, "子菜单", "/child", null, null, "p", 0, null);
        var parent = new MenuResponse(1L, "父菜单", "/parent", null, null, "p", 0, List.of(child));

        assertNotNull(parent.children());
        assertEquals(1, parent.children().size());
        assertEquals("子菜单", parent.children().get(0).name());
    }

    @Test
    @DisplayName("withChildren 返回新实例，原实例不变")
    void withChildrenShouldReturnNewInstance() {
        var original = new MenuResponse(1L, "菜单", "/menu", null, null, "perm", 0, null);
        var child = new MenuResponse(2L, "子菜单", "/child", null, null, "perm", 0, null);

        var updated = original.withChildren(List.of(child));

        assertNull(original.children());
        assertNotNull(updated.children());
        assertEquals(1, updated.children().size());
        assertNotSame(original, updated);
        assertEquals(original.id(), updated.id());
        assertEquals(original.name(), updated.name());
    }

    @Test
    @DisplayName("withChildren 传入空列表")
    void withChildrenShouldHandleEmptyList() {
        var original = new MenuResponse(1L, "菜单", "/menu", null, null, "perm", 0, List.of());
        var updated = original.withChildren(List.of());

        assertNotNull(updated.children());
        assertTrue(updated.children().isEmpty());
    }
}
