# 审查进度跟踪

## R1: Auth 核心服务 — 严重 0 / 一般 5 / 轻微 3 — 实现与设计基本一致，主要问题在刷新流程遗漏IP计数、端点路径不一致、UserConverter重复逻辑 → `review_v1.md`
> 决定：待办 T1-T5 待创建

## R2: Security & JWT — 严重 2 / 一般 4 / 轻微 4 — 架构问题突出：JwtTokenProvider启动验证缺失、Filter依赖旧JwtUtil → `review_v2.md`
> 决定：待办 T6-T11 待创建

## R3: 支持基础设施 — 严重 0 / 一般 2 / 轻微 3 — 实现质量良好，与设计高度一致 → `review_v3.md`
> 决定：待办 T12-T14 待创建

## R4: 测试覆盖 — 严重 0 / 一般 7 / 轻微 6 — 测试整体质量良好，主要问题在架构一致性（Filter依赖旧JwtUtil、changePassword手动解析JWT）和边界缺测 → `review_v4.md`
> 决定：待办 T15-T20 待创建

## R5: 实体/仓库/Schema & 菜单模块 — 严重 0 / 一般 6 / 轻微 3 — 实体/Schema质量良好；菜单模块实现与设计存在多处不一致（PUT vs PATCH、CHILDREN_EXIST缺、component未映射、N+1风险） → `review_v5.md`
> 决定：待办 T21-T26 待创建

## R6: 集成验证与整体架构一致性 — 严重 2 / 一般 3 / 轻微 1 — ACCOUNT_LOCKED消息模板未解析、UNAUTHORIZED消息不对齐、429状态码缺失、TokenBlacklist Bean重复定义 → `review_v6.md`
> 决定：待办 T27-T31 待创建
