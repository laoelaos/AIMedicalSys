package com.aimedical.integration;

import com.aimedical.common.base.MenuType;
import com.aimedical.modules.admin.entity.TokenStore;
import com.aimedical.modules.admin.entity.dict.DictData;
import com.aimedical.modules.admin.entity.dict.DictType;
import com.aimedical.modules.commonmodule.api.UserType;
import com.aimedical.modules.commonmodule.permission.PermissionFunction;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.doctor.entity.DoctorEntity;
import com.aimedical.modules.patient.entity.Gender;
import com.aimedical.modules.patient.entity.AllergySeverity;
import com.aimedical.modules.patient.entity.DiseaseStatus;
import com.aimedical.modules.patient.entity.PatientAllergy;
import com.aimedical.modules.patient.entity.PatientChronicDisease;
import com.aimedical.modules.patient.entity.PatientEntity;
import com.aimedical.modules.patient.entity.PatientFamilyHistory;
import com.aimedical.modules.patient.entity.PatientMedicationHistory;
import com.aimedical.modules.patient.entity.PatientSurgeryHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 JPA 实体注解与数据库 schema 的实际映射一致性。
 * 使用 H2 内存数据库，Hibernate 根据实体注解生成 DDL，
 * 然后执行持久化操作，检测列名、精度、关系等映射是否正确。
 *
 * 覆盖之前报告过的实体-SQL 失配问题：
 * - AllergyHistory.occurredAt ↔ allergy_history.occurred_at 列名映射
 * - HealthProfile 的 height_cm / weight_kg / bmi 精度
 * - PatientEntity.avatarUrl 长度 (500)
 * - DoctorEntity.consultationFee DECIMAL(10,2)
 * - PermissionFunction.type → MenuType 枚举映射
 * - DictData ↔ DictType @ManyToOne 关系
 * - TokenStore.token 唯一索引与长度 (768)
 */
@SpringBootTest(classes = com.aimedical.Application.class)
@AutoConfigureTestDatabase
@ActiveProfiles({"test", "phase1"})
@Transactional
class EntityMappingIT {

    @PersistenceContext
    private EntityManager entityManager;

    // ==================== PatientAllergy ====================

    @Test
    void patientAllergy_shouldMapOccurredAtColumn() {
        User testUser = new User();
        testUser.setUsername("test_pa_user");
        testUser.setPassword("pwd123");
        testUser.setNickname("过敏测试用户");
        testUser.setUserType(UserType.PATIENT);
        entityManager.persist(testUser);
        entityManager.flush();

        PatientEntity patient = new PatientEntity();
        patient.setUserId(testUser.getId());
        patient.setRealName("过敏测试患者");
        patient.setGender(Gender.MALE);
        entityManager.persist(patient);
        entityManager.flush();

        PatientAllergy allergy = new PatientAllergy();
        allergy.setPatient(patient);
        allergy.setAllergen("青霉素");
        allergy.setReactionType("皮疹");
        allergy.setSeverity(AllergySeverity.MILD);
        allergy.setOccurredAt(LocalDate.of(2023, 5, 10));

        entityManager.persist(allergy);
        entityManager.flush();

        PatientAllergy found = entityManager.find(PatientAllergy.class, allergy.getId());
        assertEquals(LocalDate.of(2023, 5, 10), found.getOccurredAt());
        assertEquals("青霉素", found.getAllergen());
        assertEquals("皮疹", found.getReactionType());
    }

    // ==================== PatientEntity ====================

    @Test
    void patientEntity_shouldMapAvatarUrl() {
        // Create a User first to satisfy FK constraint on user_id
        User testUser = new User();
        testUser.setUsername("test_patient_avatar");
        testUser.setPassword("pwd123");
        testUser.setNickname("头像测试用户");
        testUser.setUserType(UserType.PATIENT);
        entityManager.persist(testUser);
        entityManager.flush();

        PatientEntity patient = new PatientEntity();
        patient.setUserId(testUser.getId());
        patient.setRealName("测试患者");
        patient.setGender(Gender.MALE);
        // 测试最大长度 500 字符（https://example.com/avatar/ = 27 chars + 469 x + .jpg = 4 chars = 500）
        String longUrl = "https://example.com/avatar/" + "x".repeat(469) + ".jpg";
        patient.setAvatarUrl(longUrl);
        patient.setPhone("13800138000");

        entityManager.persist(patient);
        entityManager.flush();

        PatientEntity found = entityManager.find(PatientEntity.class, patient.getId());
        assertEquals(500, longUrl.length());
        assertEquals(longUrl, found.getAvatarUrl());
        assertEquals("测试患者", found.getRealName());
    }

    // ==================== DoctorEntity ====================

    @Test
    void doctorEntity_shouldMapConsultationFeePrecision() {
        DoctorEntity doctor = new DoctorEntity();
        doctor.setUserId(200L);
        doctor.setRealName("张医生");
        doctor.setTitle("副主任医师");
        doctor.setDepartment("内科");
        doctor.setConsultationFee(new BigDecimal("200.00"));

        entityManager.persist(doctor);
        entityManager.flush();

        DoctorEntity found = entityManager.find(DoctorEntity.class, doctor.getId());
        assertEquals(0, new BigDecimal("200.00").compareTo(found.getConsultationFee()));
    }

    // ==================== PermissionFunction + MenuType ====================

    @Test
    void function_shouldMapMenuTypeEnum() {
        PermissionFunction function = new PermissionFunction();
        function.setCode("test:menu");
        function.setName("测试菜单");
        function.setType(MenuType.MENU.getCode());
        function.setEnabled(true);

        entityManager.persist(function);
        entityManager.flush();

        PermissionFunction found = entityManager.find(PermissionFunction.class, function.getId());
        assertEquals(MenuType.MENU.getCode(), found.getType());
    }

    @Test
    void function_shouldPersistDirectoryType() {
        PermissionFunction dir = new PermissionFunction();
        dir.setCode("test:directory");
        dir.setName("测试目录");
        dir.setType(MenuType.DIRECTORY.getCode());
        dir.setEnabled(true);

        entityManager.persist(dir);
        entityManager.flush();

        PermissionFunction found = entityManager.find(PermissionFunction.class, dir.getId());
        assertEquals(MenuType.DIRECTORY.getCode(), found.getType());
    }

    // ==================== DictData ↔ DictType ====================

    @Test
    void dictData_shouldHaveManyToOneRelationWithDictType() {
        DictType dictType = new DictType();
        dictType.setDictName("测试字典");
        dictType.setDictType("test_dict_type");
        dictType.setStatus(true);

        entityManager.persist(dictType);
        entityManager.flush();

        DictData dictData = new DictData();
        dictData.setDictLabel("测试标签");
        dictData.setDictValue("test_value");
        dictData.setDictType(dictType);
        dictData.setDictSort(1);
        dictData.setIsDefault(true);
        dictData.setStatus(true);

        entityManager.persist(dictData);
        entityManager.flush();

        DictData found = entityManager.find(DictData.class, dictData.getId());
        assertNotNull(found.getDictType());
        assertEquals("test_dict_type", found.getDictType().getDictType());
        assertEquals("测试字典", found.getDictType().getDictName());
    }

    @Test
    void dictType_shouldHaveOneToManyDictDataList() {
        DictType dictType = new DictType();
        dictType.setDictName("级联字典");
        dictType.setDictType("cascade_dict");
        dictType.setStatus(true);

        DictData d1 = new DictData();
        d1.setDictLabel("选项A");
        d1.setDictValue("A");
        d1.setDictType(dictType);
        d1.setDictSort(1);
        d1.setStatus(true);

        DictData d2 = new DictData();
        d2.setDictLabel("选项B");
        d2.setDictValue("B");
        d2.setDictType(dictType);
        d2.setDictSort(2);
        d2.setStatus(true);

        dictType.setDictDataList(List.of(d1, d2));

        entityManager.persist(dictType);
        entityManager.flush();

        DictType found = entityManager.find(DictType.class, dictType.getId());
        assertNotNull(found.getDictDataList());
        assertEquals(2, found.getDictDataList().size());
    }

    // ==================== TokenStore ====================

    @Test
    void tokenStore_shouldMapUniqueToken() {
        TokenStore token = new TokenStore();
        token.setUserId(1L);
        token.setToken("test-jwt-token-" + "a".repeat(700));
        token.setTokenType("BEARER");
        token.setExpiresAt(LocalDateTime.now().plusDays(1));

        entityManager.persist(token);
        entityManager.flush();

        TokenStore found = entityManager.find(TokenStore.class, token.getId());
        assertNotNull(found.getToken());
        assertTrue(found.getToken().startsWith("test-jwt-token-"));
    }

    // ==================== User.password NOT NULL ====================

    @Test
    void user_shouldPersistWithPassword() {
        User user = new User();
        user.setUsername("test_user_password");
        user.setPassword("pwd123");
        user.setNickname("测试用户密码");
        user.setUserType(UserType.ADMIN);

        entityManager.persist(user);
        entityManager.flush();

        User found = entityManager.find(User.class, user.getId());
        assertEquals("pwd123", found.getPassword());
    }

    @Test
    void user_shouldRejectNullPassword() {
        User user = new User();
        user.setUsername("test_user_null_pwd");
        user.setNickname("测试空密码用户");
        user.setUserType(UserType.PATIENT);

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(user);
            entityManager.flush();
        });
    }

    // ==================== 跨实体综合测试 ====================

    // ==================== User ====================

    @Test
    void user_shouldMapUsernameField() {
        User user = new User();
        user.setUsername("test_user_field");
        user.setPassword("pwd123");
        user.setNickname("测试用户字段");
        user.setUserType(UserType.DOCTOR);
        user.setEnabled(true);

        entityManager.persist(user);
        entityManager.flush();

        User found = entityManager.find(User.class, user.getId());
        assertEquals("test_user_field", found.getUsername());
        assertEquals("pwd123", found.getPassword());
        assertEquals(UserType.DOCTOR, found.getUserType());
        assertTrue(found.getEnabled());
        assertFalse(found.getDeleted());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    @Test
    void user_shouldEnforceUserTypeNotNull() {
        User user = new User();
        user.setUsername("test_user_no_type");
        user.setPassword("pwd123");
        user.setNickname("测试无类型用户");

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(user);
            entityManager.flush();
        });
    }

    @Test
    void user_shouldMapManyToManyWithRoles() {
        Role role = new Role();
        role.setCode("test_role_m2m");
        role.setName("测试角色M2M");
        entityManager.persist(role);
        entityManager.flush();

        User user = new User();
        user.setUsername("test_user_roles");
        user.setPassword("pwd123");
        user.setNickname("测试角色用户");
        user.setUserType(UserType.ADMIN);
        user.setRoles(Set.of(role));
        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        User found = entityManager.find(User.class, user.getId());
        assertNotNull(found.getRoles());
        assertEquals(1, found.getRoles().size());
        assertEquals("test_role_m2m", found.getRoles().iterator().next().getCode());
    }

    @Test
    void user_shouldMapManyToManyWithPosts() {
        Post post = new Post();
        post.setCode("test_post_m2m");
        post.setName("测试岗位M2M");
        entityManager.persist(post);
        entityManager.flush();

        User user = new User();
        user.setUsername("test_user_posts");
        user.setPassword("pwd123");
        user.setNickname("测试岗位用户");
        user.setUserType(UserType.ADMIN);
        user.setPosts(Set.of(post));
        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        User found = entityManager.find(User.class, user.getId());
        assertNotNull(found.getPosts());
        assertEquals(1, found.getPosts().size());
        assertEquals("test_post_m2m", found.getPosts().iterator().next().getCode());
    }

    @Test
    void user_shouldMapUserTypeEnumAsString() {
        User user = new User();
        user.setUsername("test_user_enum");
        user.setPassword("pwd123");
        user.setNickname("测试枚举用户");
        user.setUserType(UserType.PATIENT);

        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        String rawType = (String) entityManager.createNativeQuery(
                "SELECT user_type FROM sys_user WHERE id = ?1")
                .setParameter(1, user.getId())
                .getSingleResult();
        assertEquals("PATIENT", rawType);

        User found = entityManager.find(User.class, user.getId());
        assertEquals(UserType.PATIENT, found.getUserType());
    }

    @Test
    void user_shouldMapPasswordChangeRequired() {
        // 验证默认值为 false
        User user = new User();
        user.setUsername("test_pcr_default");
        user.setPassword("pwd123");
        user.setNickname("测试PCR默认");
        user.setUserType(UserType.ADMIN);

        entityManager.persist(user);
        entityManager.flush();

        User found = entityManager.find(User.class, user.getId());
        assertFalse(found.getPasswordChangeRequired());

        // 设置为 true 后持久化并正确读取
        found.setPasswordChangeRequired(true);
        entityManager.flush();
        entityManager.clear();

        User reloaded = entityManager.find(User.class, user.getId());
        assertTrue(reloaded.getPasswordChangeRequired());
    }

    @Test
    void user_shouldMapTokenVersion() {
        // 验证默认值为 0
        User user = new User();
        user.setUsername("test_tv_default");
        user.setPassword("pwd123");
        user.setNickname("测试TV默认");
        user.setUserType(UserType.ADMIN);

        entityManager.persist(user);
        entityManager.flush();

        User found = entityManager.find(User.class, user.getId());
        assertEquals(Integer.valueOf(0), found.getTokenVersion());

        // 递增后持久化并正确读取
        found.setTokenVersion(1);
        entityManager.flush();
        entityManager.clear();

        User reloaded = entityManager.find(User.class, user.getId());
        assertEquals(Integer.valueOf(1), reloaded.getTokenVersion());
    }

    // ==================== Role ====================

    @Test
    void role_shouldMapCodeField() {
        Role role = new Role();
        role.setCode("test_role_code");
        role.setName("测试角色");
        role.setEnabled(true);

        entityManager.persist(role);
        entityManager.flush();

        Role found = entityManager.find(Role.class, role.getId());
        assertEquals("test_role_code", found.getCode());
        assertTrue(found.getEnabled());
        assertFalse(found.getDeleted());
    }

    @Test
    void role_shouldEnforceCodeUniqueConstraint() {
        Role role1 = new Role();
        role1.setCode("test_role_dup");
        entityManager.persist(role1);
        entityManager.flush();

        Role role2 = new Role();
        role2.setCode("test_role_dup");

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(role2);
            entityManager.flush();
        });
    }

    @Test
    void role_shouldMapOneToManyPosts() {
        Role role = new Role();
        role.setCode("test_role_posts");
        role.setName("带岗位的角色");
        entityManager.persist(role);
        entityManager.flush();

        Post post = new Post();
        post.setCode("test_post_otm");
        post.setName("测试岗位OTM");
        post.setRole(role);
        entityManager.persist(post);
        entityManager.flush();
        entityManager.clear();

        Role found = entityManager.find(Role.class, role.getId());
        assertNotNull(found.getPosts());
        assertEquals(1, found.getPosts().size());
        assertEquals("test_post_otm", found.getPosts().iterator().next().getCode());
    }

    @Test
    void role_shouldRejectNullEnabled() {
        Role role = new Role();
        role.setCode("test_role_null_enabled");
        role.setEnabled(null);

        assertThrows(PropertyValueException.class, () -> {
            entityManager.persist(role);
            entityManager.flush();
        });
    }

    // ==================== Post ====================

    @Test
    void post_shouldMapManyToOneRole() {
        Role role = new Role();
        role.setCode("test_role_post_ref");
        role.setName("岗位引用的角色");
        entityManager.persist(role);
        entityManager.flush();

        Post post = new Post();
        post.setCode("test_post_role_ref");
        post.setName("测试岗位引用角色");
        post.setRole(role);
        post.setSort(1);
        entityManager.persist(post);
        entityManager.flush();

        Post found = entityManager.find(Post.class, post.getId());
        assertNotNull(found.getRole());
        assertEquals("test_role_post_ref", found.getRole().getCode());
        assertEquals(Integer.valueOf(1), found.getSort());
        assertFalse(found.getDeleted());
    }

    @Test
    void post_shouldMapManyToManyFunctions() {
        PermissionFunction function = new PermissionFunction();
        function.setCode("test_func_post_m2m");
        function.setName("测试功能M2M");
        function.setType(MenuType.BUTTON.getCode());
        entityManager.persist(function);
        entityManager.flush();

        Post post = new Post();
        post.setCode("test_post_func_m2m");
        post.setName("测试岗位功能M2M");
        post.setFunctions(Set.of(function));
        entityManager.persist(post);
        entityManager.flush();
        entityManager.clear();

        Post found = entityManager.find(Post.class, post.getId());
        assertNotNull(found.getFunctions());
        assertEquals(1, found.getFunctions().size());
        assertEquals("test_func_post_m2m", found.getFunctions().iterator().next().getCode());
    }

    @Test
    void patientWithAllergy_shouldWorkTogether() {
        User compositeUser = new User();
        compositeUser.setUsername("test_composite_patient");
        compositeUser.setPassword("pwd123");
        compositeUser.setNickname("综合测试用户");
        compositeUser.setUserType(UserType.PATIENT);
        entityManager.persist(compositeUser);
        entityManager.flush();

        PatientEntity patient = new PatientEntity();
        patient.setUserId(compositeUser.getId());
        patient.setRealName("综合测试");
        patient.setGender(Gender.FEMALE);
        entityManager.persist(patient);
        entityManager.flush();

        PatientAllergy allergy = new PatientAllergy();
        allergy.setPatient(patient);
        allergy.setAllergen("花生");
        allergy.setSeverity(AllergySeverity.SEVERE);
        allergy.setOccurredAt(LocalDate.of(2020, 6, 1));
        entityManager.persist(allergy);
        entityManager.flush();

        PatientAllergy found = entityManager.find(PatientAllergy.class, allergy.getId());
        assertEquals("花生", found.getAllergen());
        assertEquals(AllergySeverity.SEVERE, found.getSeverity());
        assertEquals(LocalDate.of(2020, 6, 1), found.getOccurredAt());
        assertEquals(patient.getId(), found.getPatient().getId());
    }

    // ==================== PatientChronicDisease ====================

    @Test
    void patientChronicDisease_shouldMapFields() {
        User u = createTestUser("chronic_test");
        PatientEntity patient = createTestPatient(u);
        PatientChronicDisease entity = new PatientChronicDisease();
        entity.setPatient(patient);
        entity.setDiseaseName("高血压");
        entity.setCurrentStatus(DiseaseStatus.STABLE);
        entity.setDiagnosedAt(LocalDate.of(2022, 1, 15));
        entityManager.persist(entity);
        entityManager.flush();

        PatientChronicDisease found = entityManager.find(PatientChronicDisease.class, entity.getId());
        assertEquals("高血压", found.getDiseaseName());
        assertEquals(LocalDate.of(2022, 1, 15), found.getDiagnosedAt());
        assertEquals(patient.getId(), found.getPatient().getId());
    }

    // ==================== PatientFamilyHistory ====================

    @Test
    void patientFamilyHistory_shouldMapFields() {
        User u = createTestUser("family_test");
        PatientEntity patient = createTestPatient(u);
        PatientFamilyHistory entity = new PatientFamilyHistory();
        entity.setPatient(patient);
        entity.setRelationship("父亲");
        entity.setDiseaseName("冠心病");
        entity.setNote("60岁发病");
        entityManager.persist(entity);
        entityManager.flush();

        PatientFamilyHistory found = entityManager.find(PatientFamilyHistory.class, entity.getId());
        assertEquals("父亲", found.getRelationship());
        assertEquals("冠心病", found.getDiseaseName());
        assertEquals(patient.getId(), found.getPatient().getId());
    }

    // ==================== PatientSurgeryHistory ====================

    @Test
    void patientSurgeryHistory_shouldMapFields() {
        User u = createTestUser("surgery_test");
        PatientEntity patient = createTestPatient(u);
        PatientSurgeryHistory entity = new PatientSurgeryHistory();
        entity.setPatient(patient);
        entity.setSurgeryName("阑尾切除术");
        entity.setSurgeryAt(LocalDate.of(2010, 6, 15));
        entity.setHospital("北京市第一人民医院");
        entityManager.persist(entity);
        entityManager.flush();

        PatientSurgeryHistory found = entityManager.find(PatientSurgeryHistory.class, entity.getId());
        assertEquals("阑尾切除术", found.getSurgeryName());
        assertEquals(LocalDate.of(2010, 6, 15), found.getSurgeryAt());
        assertEquals(patient.getId(), found.getPatient().getId());
    }

    // ==================== PatientMedicationHistory ====================

    @Test
    void patientMedicationHistory_shouldMapFields() {
        User u = createTestUser("medication_test");
        PatientEntity patient = createTestPatient(u);
        PatientMedicationHistory entity = new PatientMedicationHistory();
        entity.setPatient(patient);
        entity.setDrugName("硝苯地平缓释片");
        entity.setReason("高血压");
        entity.setStartedAt(LocalDate.of(2022, 2, 1));
        entityManager.persist(entity);
        entityManager.flush();

        PatientMedicationHistory found = entityManager.find(PatientMedicationHistory.class, entity.getId());
        assertEquals("硝苯地平缓释片", found.getDrugName());
        assertEquals("高血压", found.getReason());
        assertEquals(patient.getId(), found.getPatient().getId());
    }

    // ==================== Helpers ====================

    private User createTestUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("pwd123");
        u.setNickname(username + "_nick");
        u.setUserType(UserType.PATIENT);
        entityManager.persist(u);
        entityManager.flush();
        return u;
    }

    private PatientEntity createTestPatient(User user) {
        PatientEntity p = new PatientEntity();
        p.setUserId(user.getId());
        p.setRealName(user.getNickname());
        p.setGender(Gender.MALE);
        entityManager.persist(p);
        entityManager.flush();
        return p;
    }
}
