# OOD 设计需求

## 背景
已完成 Phase0、Phase1ABD。现需要对以下内容进行 OOD 设计，所有包直接落地在底座（而非独立接入），规避 Phase 5 迁移成本。

## 设计范围

### Phase 2 包 C 智能分诊（3.4.1）
- 单轮/多轮双对话
- 规则可配置
- Mock 兜底回退科室列表

### Phase 3 包 D-AI1 处方审核（3.4.2）
- 风险等级差异化阻断
- AI 超时回退本地规则校验打标

### Phase 3 包 D-AI2 病历生成（3.4.3）
- 对话转结构化病历
- 按科室配置规则
- 关键字段缺失提示补全

### Phase 3 包 E 辅助开方（3.4.10）
- 剂量阈值告警
- 与处方审核强耦合同步落地

## 参考文档
- 已有项目代码：AIMedical
- 其他文档（需求、路线、技术栈、其他 OOD）：Docs 目录
- Phase 5 包 G 初步 OOD（未完成）：`Harness\redeliberations\202606271627_phase5_pkgG_ood\a_v22_copy_from_v21.md`
- 本阶段已有 OOD 草案：`Harness\redeliberations\202606281422_phase23_ood\a_v19_copy_from_v18.md`

## 约束
- 所有包直接落地在底座上，不独立接入
- 与处方审核强耦合同步落地
