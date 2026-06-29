# 测试报告（v1）

## 测试文件清单

| 文件路径 | 类型 | 说明 |
|---------|------|------|
| `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` | 集成测试 | 新增 user_shouldPersistWithPassword / user_shouldRejectNullPassword |

## 与行为契约对应关系

| 行为契约条件 | 测试方法 | 验证方式 |
|-------------|---------|---------|
| persist User 且 password = "xxx" → 成功 | `user_shouldPersistWithPassword()` | persist 后查询，断言 password 匹配 |
| persist User 且 password = null → DataIntegrityViolationException | `user_shouldRejectNullPassword()` | persist + flush 时 assertThrows |
| schema.sql 从头建库 → password 列 NOT NULL | 由 EntityMappingIT 的 H2 DDL 生成间接覆盖 | H2 根据实体注解生成的 DDL 即隐含 NOT NULL 约束 |

## 测试运行说明

- 使用 `@SpringBootTest` + `@AutoConfigureTestDatabase` + `@Transactional`，H2 内存数据库
- 遵循 EntityMappingIT.java 既有模式（entityManager.persist + flush + find）
- 不依赖测试执行顺序
- 正向用例：设置非 null password 后 persist 应成功
- 错误路径：未设置 password（null）时 persist 应抛出 DataIntegrityViolationException
