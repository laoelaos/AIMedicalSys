package com.aimedical.modules.commonmodule.api.dto;

import com.aimedical.modules.commonmodule.api.UserDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDtoTest {

    @Test
    void shouldCreateViaNoArgConstructor() {
        UserDto dto = new UserDto();
        assertNotNull(dto);
    }

    @Test
    void shouldCreateViaAllArgsConstructor() {
        UserDto dto = new UserDto(1L, "alice", "Alice", "13800000000",
                "alice@example.com", "女", 30);
        assertEquals(1L, dto.getId());
        assertEquals("alice", dto.getUsername());
        assertEquals("Alice", dto.getNickname());
        assertEquals("13800000000", dto.getPhone());
        assertEquals("alice@example.com", dto.getEmail());
        assertEquals("女", dto.getGender());
        assertEquals(30, dto.getAge());
    }

    @Test
    void shouldSetAndGetId() {
        UserDto dto = new UserDto();
        dto.setId(42L);
        assertEquals(42L, dto.getId());
    }

    @Test
    void shouldSetAndGetUsername() {
        UserDto dto = new UserDto();
        dto.setUsername("bob");
        assertEquals("bob", dto.getUsername());
    }

    @Test
    void shouldSetAndGetNickname() {
        UserDto dto = new UserDto();
        dto.setNickname("Bob");
        assertEquals("Bob", dto.getNickname());
    }

    @Test
    void shouldSetAndGetPhone() {
        UserDto dto = new UserDto();
        dto.setPhone("13900000001");
        assertEquals("13900000001", dto.getPhone());
    }

    @Test
    void shouldSetAndGetEmail() {
        UserDto dto = new UserDto();
        dto.setEmail("bob@example.com");
        assertEquals("bob@example.com", dto.getEmail());
    }

    @Test
    void shouldSetAndGetGender() {
        UserDto dto = new UserDto();
        dto.setGender("男");
        assertEquals("男", dto.getGender());
    }

    @Test
    void shouldSetAndGetAge() {
        UserDto dto = new UserDto();
        dto.setAge(25);
        assertEquals(25, dto.getAge());
    }
}
