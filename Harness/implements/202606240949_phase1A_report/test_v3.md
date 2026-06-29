# 测试验证报告（v3）

## 验证结论

通过。实现与详细设计完全一致，无需额外修改。

## 逐项核对

### 失败 1：`user_shouldPersistWithPassword`

| 项目 | 设计要求 | 实现 | 状态 |
|------|---------|------|------|
| 插入 `user.setUserType(UserType.ADMIN)` | 第 253 行之后 | 第 254 行 | ✓ |

### 失败 2：`user_shouldRejectNullPassword`

| 项目 | 设计要求 | 实现 | 状态 |
|------|---------|------|------|
| 插入 `user.setUserType(UserType.PATIENT)` | 第 265 行之后 | 第 267 行 | ✓ |
| `DataIntegrityViolationException` → `ConstraintViolationException` | 第 267 行 | 第 269 行 | ✓ |

## 行为契约覆盖

| 行为契约 | 测试方法 | 覆盖状态 |
|---------|---------|---------|
| 正确持久化含 userType 的 User，断言 password 字段映射正确 | `user_shouldPersistWithPassword` | ✓ |
| 设置 userType 后 password 为 null，persist + flush 抛出 ConstraintViolationException | `user_shouldRejectNullPassword` | ✓ |

## 测试文件

- `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` — 19 个测试方法，2 个已按设计修复
- 无新增测试文件，无新增测试方法

## 验证命令

```bash
mvn test -pl integration -am
```

预期结果：EntityMappingIT 全部 19 个测试方法通过，integration 模块零失败。
