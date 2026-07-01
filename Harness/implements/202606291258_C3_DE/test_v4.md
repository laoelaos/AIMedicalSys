# 测试报告（v4）

## 测试文件

| 文件 | 类型 | 路径 |
|------|------|------|
| DosageStandardTest | 单元测试（无Spring上下文） | `common/src/test/java/com/aimedical/common/entity/DosageStandardTest.java` |
| DosageStandardAuditTest | 集成测试（@DataJpaTest + H2） | `common/src/test/java/com/aimedical/common/entity/DosageStandardAuditTest.java` |

## 覆盖率维度

### 正常路径
- 默认字段值（所有字段为null，继承的deleted=false）
- 各字段 getter/setter 读写
- 仅含必填字段时通过 Bean Validation
- 包含全部字段时通过 Bean Validation
- 完整字段数据库持久化并回读
- 数据库自动生成 ID 和审计时间戳

### 边界条件
- ageRangeStart/ageRangeEnd 允许 null（无边界）
- weightRangeStart/weightRangeEnd 允许 null（无边界）
- dailyMax 允许 null（未配置日剂量上限）
- BigDecimal 精度/标度保持（compareTo 比较）

### 错误路径
- drugCode 为空/空白 → validation violation
- routeOfAdministration 为空/空白 → validation violation
- singleMax 为 null → validation violation（@NotNull）
- singleMax 为零/负数 → validation violation（@Positive）
- unit 为空白 → validation violation
- 所有必填字段同时缺失 → validation violation（4项）
- 数据库中插入缺失必填字段的记录 → 异常

### 状态交互
- 持久化后审计字段自动填充（createdAt, updatedAt）
- 修改后 updatedAt 更新
- 多次持久化分配不同 ID

## 测试规范遵循情况

- 基于行为契约设计：所有用例面向公开接口行为，不测实现细节
- 正向用例：每个行为契约至少一个
- 用例独立：不依赖执行顺序
- 不修改编码agent源码
