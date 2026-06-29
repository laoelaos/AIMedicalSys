# 任务指令（v12）

## 动作
NEW

## 任务描述
合并修复所有剩余的测试相关待办项（来自 phase1B_report.md），共 16 项：

| 编号 | 类型 | 描述 | 目标文件 | 预期变更 |
|------|------|------|---------|---------|
| T3 | 文档对齐 | 修正审查范围描述中三条测试文件路径（application/ → 实际模块路径） | `Harness/reviews/202606270204_fix_phase1B_code_review/review_v2_D.md` | 路径文本修正 |
| T4 | 测试增强 | 新增 principal=null 和 principal=非Long 两个测试场景 | `PasswordChangeCheckFilterTest.java` | 新增 2 个测试方法 |
| T5 | 测试重构 | UserFacadeImplTest 解 mock UserConverter，注入真实 Converter 验证转换逻辑 | `UserFacadeImplTest.java` | 修改测试架构，新增真实转换验证 |
| T7 | 测试增强 | 新增 LoggingSecurityAuditLogger 写入失败（IOException）降级路径测试 | `LoggingSecurityAuditLoggerTest.java` | 新增 1 个测试方法 |
| T20 | 测试增强 | 新增 3 个插值回退路径测试：args=null+有占位符、无占位符+非空 args、占位符>args 数量 | `SimpleMessageInterpolatorTest.java` | 新增 3 个测试方法 |
| T24 | 测试增强 | SlidingWindowCounterTest 并发测试从 `<=limit` 改为精确验证并发下严格限流 | `SlidingWindowCounterTest.java` | 修改并发测试断言 |
| T25 | 测试修正 | SlidingWindowCounterTest 移除锁反射测试（T13 已改 ConcurrentHashMap.compute，验证方式改为检查 tryAcquire 返回值语义） | `SlidingWindowCounterTest.java` | 移除/重写锁测试 |
| T26 | 测试增强 | PasswordPolicyImplTest 新增全字符集(4/4类型)边界测试 | `PasswordPolicyImplTest.java` | 新增 1 个测试方法 |
| T27 | 测试增强 | UserConverterTest 新增 sort=null + enabled=false 组合场景 | `UserConverterTest.java` | 新增 1 个测试方法 |
| T28 | 测试增强 | UserFacadeImplTest 新增 Repository 抛出 DataAccessException 场景 | `UserFacadeImplTest.java` | 新增 1 个测试方法 |
| T29 | 测试增强 | CurrentUserImplTest 新增 principal 类型非 Long 测试 | `CurrentUserImplTest.java` | 新增 1 个测试方法 |
| T30 | 测试修正 | SecurityConfigPhase1Test filter 顺序测试改为非反射方式 | `SecurityConfigPhase1Test.java` | 重写 filter 顺序验证 |
| T31 | 测试增强 | EntityMappingIT 新增 Role.enabled NOT NULL 约束验证 | `EntityMappingIT.java` | 新增 1 个集成测试方法 |
| T32 | 测试增强 | PasswordChangeRequestTest 新增 oldPassword 1 字符最小长度边界 | `PasswordChangeRequestTest.java` | 新增 1 个测试方法 |
| T33 | 测试修正 | MenuServiceTest 重命名 shouldNotFilterDeletedInJavaLayer → 更语义化的名称 | `MenuServiceTest.java` | 重命名测试方法 |
| T34 | 测试增强 | RoleTest 新增 sort 字段 NOT NULL 约束验证 | `RoleTest.java` | 新增 1 个测试方法 |

注意：以下已在先前轮次完成，不纳入 R9：
- T6（AuthServiceTest 异常刷新检测测试）：已在 R2 伴随 T2 代码修复完成
- T21（AuthServiceTest 冗余 mock 清理）：已在 R5 完成
- T23（AuthControllerTest SecurityContext 空路径）：已在 R4 完成
- T22：误报，无需修复

## 选择理由
这是全部 34 个待办事项中最后一组未完成的测试增强任务。合并为一个大轮次完成所有剩余测试工作，避免分散成多个小轮次。所有测试文件均为独立修改，无交叉依赖，可并行实施。

## 任务上下文
### 已完成的编码/文档修复（作为测试增强的背景）
- T1: JwtTokenProvider 补加 type claim
- T2: AuthServiceImpl 异常刷新检测改为拒绝请求
- T9+T10+T18: AuthServiceImpl logout 审计日志/二次解析/memory leak
- T11: getCurrentUser 改用 SecurityContext
- T12: JwtTokenProvider URL-safe Base64
- T13: SlidingWindowCounter 锁粒度改为 ConcurrentHashMap.compute
- T14: JwtUtil 遗留 claims 清理
- T15: LoginAttemptTracker record* 窗口过期防御
- T16: SimpleMessageInterpolator 命名占位符优化
- T17: MessageInterpolator 组件抽取
- T19: MenuController CurrentUser 注入
- T8+T2-OOD+T12-OOD+T13-OOD+T17-OOD: OOD 文档更新

### 模块结构参考
- 测试文件在 modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/ 下
- 集成测试在 integration/src/test/java/com/aimedical/integration/ 下
- 审查文档在 Harness/reviews/202606270204_fix_phase1B_code_review/review_v2_D.md

### 验证方式
执行全量测试：
```
mvn test -pl common,modules/common-module -am
```
以及 integration 模块（如果 EntityMappingIT 在集成测试中）：
```
mvn test -pl common,modules/common-module,integration -am
```
