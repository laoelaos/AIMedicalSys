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
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.lang.reflect.Method;

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

    @Autowired
    private UserRepository userRepository;

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

    @Test
    void shouldFindByUsernameReturnOptional() throws NoSuchMethodException {
        Method method = UserRepository.class.getMethod("findByUsername", String.class);
        assertEquals(Optional.class, method.getReturnType());
    }

    @Test
    void shouldFindByUsernameReturnUserWhenExists() {
        User user = new User();
        user.setUsername("finduser");
        user.setPassword("pwd123");
        user.setNickname("查找用户");
        user.setUserType(UserType.ADMIN);
        em.persistAndFlush(user);

        Optional<User> found = userRepository.findByUsername("finduser");
        assertTrue(found.isPresent());
        assertEquals("finduser", found.get().getUsername());
    }

    @Test
    void shouldFindByUsernameReturnEmptyWhenNotFound() {
        Optional<User> found = userRepository.findByUsername("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void shouldHaveFindWithDetailsForMenuByIdMethod() throws NoSuchMethodException {
        Method method = UserRepository.class.getMethod("findWithDetailsForMenuById", Long.class);
        assertEquals(Optional.class, method.getReturnType());
    }

    @Test
    void shouldHaveEntityGraphAnnotationOnFindWithDetailsForMenuById() throws NoSuchMethodException {
        Method method = UserRepository.class.getMethod("findWithDetailsForMenuById", Long.class);
        EntityGraph annotation = method.getAnnotation(EntityGraph.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{"roles", "posts", "posts.functions"}, annotation.attributePaths());
    }

    @Test
    void shouldFindWithDetailsForMenuByIdReturnUserWhenExists() {
        User user = new User();
        user.setUsername("menufinduser");
        user.setPassword("pwd123");
        user.setNickname("菜单查找用户");
        user.setUserType(UserType.ADMIN);
        em.persistAndFlush(user);

        Optional<User> found = userRepository.findWithDetailsForMenuById(user.getId());
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    void shouldFindWithDetailsForMenuByIdReturnEmptyWhenNotFound() {
        Optional<User> found = userRepository.findWithDetailsForMenuById(99999L);
        assertFalse(found.isPresent());
    }
}
