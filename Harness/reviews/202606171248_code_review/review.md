# 审查进度跟踪

## 审查概况
- 日期: 20260617
- 源分支: `202606170026_phase0_skeleton`
- 目标分支: `main`
- 审查依据: `Docs/04_ood_phase0.md`
- 审查范围: 详见 `scope.md`
- 审查轮次计划: 2 轮（每轮 3 个并行 agent）

## R1: 后端 Common + POM + 业务模块 — 严重 0 / 一般 7 / 轻微 8 — 核心类型高度一致，POM 存在结构性偏离 → `review_v1_A1.md`, `review_v1_A2.md`, `review_v1_A3.md`

### R1 结论

| Agent | 审查范围 | 严重 | 一般 | 轻微 | 总评 |
|-------|---------|------|------|------|------|
| A1 | Common 模块（Result/PageQuery/ErrorCode/BaseEntity等） | 0 | 1 | 3 | 核心类型高度一致 |
| A2 | POM 结构与依赖管理 | 0 | 6 | 4 | 扁平布局偏离设计分层结构 |
| A3 | 业务模块 + common-module + Application | 0 | 0 | 1 | 高度一致，仅缺 @Table |

## R2: AI 模块 + 测试覆盖 + 前端 Monorepo — 严重 0 / 一般 3 / 轻微 6 — 高度一致，仅轻微偏离 → `review_v2_A1.md`, `review_v2_A2.md`, `review_v2_A3.md`

### R2 结论

| Agent | 审查范围 | 严重 | 一般 | 轻微 | 总评 |
|-------|---------|------|------|------|------|
| A1 | AI 模块（ai-api/ai-impl） | 0 | 0 | 2 | 与 §3.4 高度一致 |
| A2 | 测试覆盖分析（41 个文件） | 0 | 2 | 3 | 覆盖良好，缺日志/注解验证 |
| A3 | 前端 Monorepo | 0 | 1 | 1 | 基本一致，缺 NETWORK_ERROR 处理 |

### 总体统计

| 严重程度 | R1 | R2 | 总计 |
|---------|----|----|------|
| 严重 | 0 | 0 | **0** |
| 一般 | 7 | 3 | **10** |
| 轻微 | 8 | 6 | **14** |
| **合计** | **15** | **9** | **24** |

### R1+R2 所有决定
- T1: BaseEnum 需在 OOD 设计中补充规范定义
- T2: GlobalExceptionHandler 缺少 HttpMessageNotReadableException/HttpMessageNotWritableException 专用处理器
- T3: GlobalExceptionHandler.handleBusinessException 需按 ErrorCode 区分 HTTP 状态码
- T4: JacksonConfig 移除未使用的 import
- T5: POM 目录结构与设计 §2.1 不一致（扁平 vs 分层）
- T6: 缺失聚合 POM（common-module/pom.xml, ai/pom.xml）
- T7: Spring Boot 版本 3.2.5 与设计 3.3.0 不一致
- T8: dependencyManagement 缺少 starter 显式声明
- T9: Common POM 多引入 spring-boot-starter-validation
- T10: maven-dependency-plugin 豁免范围过宽
- T11: 业务实体缺少 @Table 注解
- T12: FallbackAiService 空委托 ERROR 日志触发时机与设计描述存在细微偏差
- T13: @phase0-mock-field 注解未在代码中定义
- T14: FallbackAiServiceTest 未验证空委托列表的日志输出
- T15: PageQueryTest 未验证校验注解的编译期存在性
- T16: BaseEntityTest 未验证 JPA 核心注解的存在性
- T17: SecurityConfigPhase0 无单元测试
- T18: ApiClient 错误拦截器未按 §3.5 实现 NETWORK_ERROR 处理
- T19: shared 包中 axios 声明为 devDependency 而非 dependency
