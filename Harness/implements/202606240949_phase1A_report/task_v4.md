# 任务指令（v4）

## 动作
NEW

## 任务描述
修复 `UserRepositoryTest.java` 中 2 个集成测试失败的缺陷：

1. **`shouldRejectNullPassword`**（第 47-52 行）：  
   - 当前：`assertThrows(DataIntegrityViolationException.class, () -> em.persistAndFlush(user));`  
   - 根因：Hibernate 对非空属性为 null 直接抛出 `PropertyValueException`，而非 Spring 包装的 `DataIntegrityViolationException`  
   - 修改：`DataIntegrityViolationException.class` → `PropertyValueException.class`  
   - 添加 import：`import org.hibernate.PropertyValueException;`

2. **`shouldHaveNotNullConstraintOnPasswordColumn`**（第 65-71 行）：  
   - 当前：`WHERE TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'password'`  
   - 根因：H2 INFORMATION_SCHEMA.COLUMNS 以大写存储标识符（`SYS_USER`、`PASSWORD`），小写字符串比较不匹配 → `EmptyResultDataAccessException`  
   - 修改：`UPPER(TABLE_NAME) = 'SYS_USER' AND UPPER(COLUMN_NAME) = 'PASSWORD'`

## 选择理由
这是 reactor 构建链中 **上游模块（common-module-impl）** 的 2 个测试失败。它们阻断了下游 integration 模块的执行。v3 虽然正确修复了 EntityMappingIT 的 2 个测试，但上游模块未通过 → integration 模块从未被运行。必须优先修复上游缺陷以打通全链路。

## 任务上下文
- 文件：`AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/permission/UserRepositoryTest.java`
- 2 个测试方法已在 Issue 2 修复时被正确添加了 `user.setUserType(UserType.ADMIN)`——仅异常类型断言和 INFORMATION_SCHEMA 查询大小写需要修正
- 无需修改其他文件
- 修改后验证命令：`mvn test -pl common-module-impl,integration -am`

## 已有代码上下文
```java
// UserRepositoryTest.java 关键代码
// 第 7 行：已有 import org.springframework.dao.DataIntegrityViolationException;（其他测试仍使用，不可删除）
// 需要新增 import org.hibernate.PropertyValueException;

// 第 47-52 行：
@Test
void shouldRejectNullPassword() {
    User user = new User();
    user.setUsername("testuser_null_pwd");
    user.setUserType(UserType.ADMIN);  // 已正确设置
    assertThrows(DataIntegrityViolationException.class, () -> em.persistAndFlush(user));
    // → 改为 PropertyValueException.class
}

// 第 65-71 行：
@Test
void shouldHaveNotNullConstraintOnPasswordColumn() {
    String sql = "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                 "WHERE TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'password'";
    // → 改为 UPPER(TABLE_NAME) = 'SYS_USER' AND UPPER(COLUMN_NAME) = 'PASSWORD'
    String nullable = jdbcTemplate.queryForObject(sql, String.class);
    assertEquals("NO", nullable);
}
```
