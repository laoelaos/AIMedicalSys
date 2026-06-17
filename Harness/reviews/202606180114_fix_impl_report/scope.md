# 审查范围界定

## 基本信息
- **源分支**: `202606172323_fix_impl_report`
- **目标分支**: `main`
- **审查时间**: 2026-06-18 01:14
- **审查依据**: `Docs/04_ood_phase0.md` Phase 0 架构级 OOD 设计

## 审查目标
验证当前分支相对 main 的变更是否与 OOD 设计一致，包括：
1. **模块划分与目录布局** — 代码结构是否符合 §2 的 Monorepo 布局和包命名规范
2. **核心抽象实现** — Result, PageQuery/PageResponse, ErrorCode, BaseEntity, BaseEnum, GlobalExceptionHandler, JpaConfig 等是否按 §3 定义实现
3. **权限模型骨架** — User, Role, Post, Function 实体和关系映射是否按 §3.3 定义实现
4. **AI 能力模块** — AiService 接口、AiResult、MockAiService、降级策略框架是否按 §3.4 定义实现
5. **模块间依赖** — POM 依赖配置是否按 §2.2 的依赖方向规范
6. **API 通信规范** — Controller 路径前缀、返回类型、分页契约是否按 §8 定义
7. **前端骨架** — apps/*, packages/* 是否按 §2.4 和 §3.5 定义实现
8. **配置与启动** — application.yml, SecurityConfigPhase0, 启动类是否按 §9 定义
9. **CI 与文档** — CI 文件、QUICKSTART.md、CONTRIBUTING.md 是否匹配 OOD 规范
10. **测试覆盖率** — 各模块的占位测试是否满足最低要求

## 重点审查项
- 模块 POM 依赖是否符合依赖方向（common optional → modules compile → application）
- 跨模块调用是否遵守"不允许直接依赖"约束
- 实体 JPA 映射（cascade=FetchType.LAZY, 无 cascade, @SQLRestriction 等）
- AiService 的 13 个方法签名是否完整
- FallbackAiService 的自引用排除逻辑是否正确
- 前端 npm workspaces 引用是否正确（workspace:*）
- 安全配置的 @Profile("phase0") 是否正确激活

## 排除范围
- Harness/ 目录下的过程制品（审议记录、诊断报告等）不纳入代码审查
- 非骨架相关的历史遗留文件
