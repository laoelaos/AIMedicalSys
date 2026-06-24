# 任务指令（v3）

## 动作
RETRY

## 任务描述
修复 EntityMappingIT.java 中 v1 引入的 2 个失败测试方法：`user_shouldPersistWithPassword` 和 `user_shouldRejectNullPassword`。无需改动其他文件。

## 选择理由
v2 完成了全部 4 个 Issue 的代码修复（H2 依赖、deleted NOT NULL、Java 默认值、9 个新集成测试），代码/设计/测试审查全部 APPROVED。但验证阶段 2 个失败均来自 v1 遗留的测试缺陷，非 v2 变更导致。v3 仅修复这 2 个缺陷，即可使全部 21 个测试通过。

## 任务上下文
### 失败 1：user_shouldPersistWithPassword（第 250-260 行）
```java
@Test
void user_shouldPersistWithPassword() {
    User user = new User();
    user.setUsername("test_user_password");
    user.setPassword("pwd123");

    entityManager.persist(user);
    entityManager.flush();
    ...
}
```
**根因**：User.userType 列标注了 `@Column(nullable = false)`，但测试未设置 userType。在 H2（v2 修复前测试因缺少 H2 依赖从未执行过）下，Hibernate 自动生成的 DDL 包含 userType NOT NULL 约束，persist + flush 时抛出 `ConstraintViolationException`。
**修正**：在第 253 行后添加 `user.setUserType(UserType.ADMIN);`

### 失败 2：user_shouldRejectNullPassword（第 262-271 行）
```java
@Test
void user_shouldRejectNullPassword() {
    User user = new User();
    user.setUsername("test_user_null_pwd");

    assertThrows(DataIntegrityViolationException.class, () -> {
        entityManager.persist(user);
        entityManager.flush();
    });
}
```
**根因**（双重问题）：
1. 同样未设置 userType，抛出的异常实际是 userType NOT NULL 约束违例，而非 password NOT NULL
2. 使用 `EntityManager` 直接操作时（非 Spring Data Repository），Hibernate NOT NULL 约束违例抛出 `org.hibernate.exception.ConstraintViolationException`，而非 `org.springframework.dao.DataIntegrityViolationException`
**修正**：
1. 添加 `user.setUserType(UserType.PATIENT)` 以正确触发 password NOT NULL 约束
2. 将 `assertThrows` 的预期异常类型从 `DataIntegrityViolationException.class` 改为 `ConstraintViolationException.class`

## 已有代码上下文
- **文件**：`AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java`
- **已存在的 import**：第 21 行 `import org.hibernate.exception.ConstraintViolationException;` 已存在；第 7 行 `import com.aimedical.modules.commonmodule.api.UserType;` 已存在。**无需新增 import。**
- **测试框架**：`@SpringBootTest + @AutoConfigureTestDatabase + @Transactional`，使用 `entityManager.persist + flush + find` 模式，与现有测试一致
- **User.userType 注解**：`@Column(nullable = false)`，JPA 枚举存储为 `@Enumerated(EnumType.STRING)` → VARCHAR(20)
- **User.password 注解**：已添加 `@Column(nullable = false)`（v1 变更）

## RETRY 说明
### 失败原因摘要
v2 验证报告显示 2 个失败：
```
[INFO]   EntityMappingIT.user_shouldRejectNullPassword:267 
    Unexpected exception type thrown, expected: 
    <org.springframework.dao.DataIntegrityViolationException> 
    but was: <org.hibernate.exception.ConstraintViolationException>

[INFO]   EntityMappingIT.user_shouldPersistWithPassword:255 
    ? ConstraintViolation could not execute statement 
    [NULL not allowed for column "USER_TYPE"]
```

### 修正方向
| 测试方法 | 问题 | 修改 |
|---------|------|------|
| `user_shouldPersistWithPassword` | 未设置 userType | 添加 `user.setUserType(UserType.ADMIN)` |
| `user_shouldRejectNullPassword` | 未设置 userType + 异常类型错误 | 添加 `user.setUserType(UserType.PATIENT)` + s/DataIntegrityViolationException/ConstraintViolationException/ |

### 验证方式
```bash
mvn test -pl integration -am
```
期待结果：全部 21 个测试通过（原有 10 个 + v1 修复 2 个 + v2 新增 9 个），零失败。
