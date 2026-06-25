package com.aimedical.modules.commonmodule.permission;

import com.aimedical.common.config.JpaConfig;
import com.aimedical.modules.commonmodule.api.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
class UserRepositoryTest {

    @SpringBootApplication
    static class TestConfig {}

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void shouldRejectNullPassword() {
        User user = new User();
        user.setUsername("testuser_null_pwd");
        user.setNickname("测试用户");
        user.setUserType(UserType.ADMIN);
        assertThrows(ConstraintViolationException.class, () -> em.persistAndFlush(user));
    }

    @Test
    void shouldPersistWithValidPassword() {
        User user = new User();
        user.setUsername("testuser_valid_pwd");
        user.setPassword("pwd123");
        user.setNickname("测试用户");
        user.setUserType(UserType.ADMIN);
        User saved = em.persistAndFlush(user);
        assertNotNull(saved.getId());
        assertEquals("pwd123", saved.getPassword());
    }

    @Test
    void shouldHaveNotNullConstraintOnPasswordColumn() {
        String sql = "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE UPPER(TABLE_NAME) = 'SYS_USER' AND UPPER(COLUMN_NAME) = 'PASSWORD'";
        String nullable = jdbcTemplate.queryForObject(sql, String.class);
        assertEquals("NO", nullable);
    }
}
