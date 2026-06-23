package com.aimedical.modules.commonmodule.permission;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    @Test
    void shouldBeInterface() {
        assertTrue(UserRepository.class.isInterface());
    }

    @Test
    void shouldExtendJpaRepository() {
        assertTrue(JpaRepository.class.isAssignableFrom(UserRepository.class));
    }

    @Test
    void shouldBeAnnotatedWithRepository() {
        assertNotNull(UserRepository.class.getAnnotation(Repository.class));
    }
}
