package com.aimedical.modules.commonmodule.auth;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserInfoResponseTest {

    @Test
    void shouldExposeId() {
        UserInfoResponse response = new UserInfoResponse(1L, "alice", "Alice", "13800000000",
                "alice@example.com", "DOCTOR", "OUTPATIENT", Set.of("patient:read"));
        assertEquals(1L, response.id());
    }

    @Test
    void shouldExposeUsername() {
        UserInfoResponse response = new UserInfoResponse(1L, "alice", "Alice", "13800000000",
                "alice@example.com", "DOCTOR", "OUTPATIENT", Set.of("patient:read"));
        assertEquals("alice", response.username());
    }

    @Test
    void shouldExposeRealName() {
        UserInfoResponse response = new UserInfoResponse(1L, "alice", "Alice", "13800000000",
                "alice@example.com", "DOCTOR", "OUTPATIENT", Set.of("patient:read"));
        assertEquals("Alice", response.realName());
    }

    @Test
    void shouldExposePhone() {
        UserInfoResponse response = new UserInfoResponse(1L, "alice", "Alice", "13800000000",
                "alice@example.com", "DOCTOR", "OUTPATIENT", Set.of("patient:read"));
        assertEquals("13800000000", response.phone());
    }

    @Test
    void shouldExposeEmail() {
        UserInfoResponse response = new UserInfoResponse(1L, "alice", "Alice", "13800000000",
                "alice@example.com", "DOCTOR", "OUTPATIENT", Set.of("patient:read"));
        assertEquals("alice@example.com", response.email());
    }

    @Test
    void shouldExposeRole() {
        UserInfoResponse response = new UserInfoResponse(1L, "alice", "Alice", "13800000000",
                "alice@example.com", "DOCTOR", "OUTPATIENT", Set.of("patient:read"));
        assertEquals("DOCTOR", response.role());
    }

    @Test
    void shouldExposePosition() {
        UserInfoResponse response = new UserInfoResponse(1L, "alice", "Alice", "13800000000",
                "alice@example.com", "DOCTOR", "OUTPATIENT", Set.of("patient:read"));
        assertEquals("OUTPATIENT", response.position());
    }

    @Test
    void shouldExposePermissions() {
        Set<String> permissions = Set.of("patient:read", "patient:write");
        UserInfoResponse response = new UserInfoResponse(1L, "alice", "Alice", "13800000000",
                "alice@example.com", "DOCTOR", "OUTPATIENT", permissions);
        assertEquals(permissions, response.permissions());
    }

    @Test
    void shouldAllowNullComponents() {
        UserInfoResponse response = new UserInfoResponse(null, null, null, null, null, null, null, null);
        assertNull(response.id());
        assertNull(response.username());
        assertNull(response.realName());
        assertNull(response.phone());
        assertNull(response.email());
        assertNull(response.role());
        assertNull(response.position());
        assertNull(response.permissions());
    }

    @Test
    void shouldSupportEmptyPermissions() {
        UserInfoResponse response = new UserInfoResponse(2L, "bob", null, null, null, "PATIENT", null, Set.of());
        assertEquals(Set.of(), response.permissions());
    }
}