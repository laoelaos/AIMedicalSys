package com.aimedical.modules.commonmodule.permission;

import com.aimedical.common.base.BaseEntity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FunctionTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        Function function = new Function();
        assertNotNull(function);
        assertInstanceOf(BaseEntity.class, function);
    }

    @Test
    void shouldSetAndGetCode() {
        Function function = new Function();
        function.setCode("FUNC_CREATE");
        assertEquals("FUNC_CREATE", function.getCode());
    }

    @Test
    void shouldSetAndGetName() {
        Function function = new Function();
        function.setName("创建");
        assertEquals("创建", function.getName());
    }

    @Test
    void shouldSetAndGetDescription() {
        Function function = new Function();
        function.setDescription("创建操作");
        assertEquals("创建操作", function.getDescription());
    }

    @Test
    void shouldSetAndGetEnabled() {
        Function function = new Function();
        function.setEnabled(true);
        assertTrue(function.getEnabled());
        function.setEnabled(false);
        assertFalse(function.getEnabled());
    }

    @Test
    void shouldSetAndGetPosts() {
        Function function = new Function();
        Set<Post> posts = new HashSet<>();
        posts.add(new Post());
        function.setPosts(posts);
        assertEquals(1, function.getPosts().size());
    }
}
