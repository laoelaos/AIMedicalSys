# 再审议判定报告（v10）

## 判定结果

RETRY

## 判定理由

诊断报告共识别出 8 项问题，其中严重级 3 项（#1 CRITICAL阻断链路断裂、#2 submit端点缺Controller归属、#6 DosageAlert缺少warningType字段级契约断裂），一般级 4 项（#3/#4 DTO字段缺失、#7 字符约束缺失、#8 迁移断言条件缺失）。质询报告结论为 LOCATED，确认审查有效。组件B内部循环实际轮次（2）小于最大轮次（12），因审查被确认后提前终止。根据判定标准，审查报告包含严重或一般等级问题，应判定 RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：CRITICAL 剂量告警在提交流程的阻断链路不完整，且 PrescriptionDraftContext 的 CRITICAL 标记缺乏覆盖/清理语义
- **所在位置**：§3.4 DosageAlertLevel 职责描述、§4.2 处方提交端点行为（第 1/2/3 条）、§3.4 PrescriptionDraftContext 清理时机
- **严重程度**：严重
- **改进建议**：在 §4.2 补充提交前检查 CRITICAL 告警；在 §4.4 check-dose 流程中明确每次重新计算并覆盖标记；补充清理契约

- **问题描述**：POST /api/prescription/submit 端点缺少 Controller 归属
- **所在位置**：§4.2 处方提交端点行为契约、§2.1 目录结构
- **严重程度**：严重
- **改进建议**：新增 PrescriptionSubmitController 或将 submit 端点归入 PrescriptionAuditController

- **问题描述**：业务层 DosageAlert 缺少 warningType 字段，导致需求 3.4.10 字段级契约断裂
- **所在位置**：§3.4 DosageAlert 字段定义、§3.4 DosageThresholdService 职责描述、§10.4 ai-api 层 DoseWarningItem.warningType
- **严重程度**：严重
- **改进建议**：DosageAlert 增加 warningType 字段；DosageThresholdService 明确三种输出路径及 warningType 赋值规则；补充 OVER_DURATION 实现说明

- **问题描述**：AllergyWarningItem 与 AllergyWarning 命名不一致，且业务层 DTO 字段定义缺失
- **所在位置**：§2.1 目录、§3.4 PrescriptionAssistResponse、§10.4
- **严重程度**：一般
- **改进建议**：统一命名，补充业务层 AllergyWarning DTO 完整字段定义

- **问题描述**：DoseWarning 业务层 DTO 字段定义缺失
- **所在位置**：§3.4 PrescriptionAssistResponse 字段描述、§1.3 核心抽象一览
- **严重程度**：一般
- **改进建议**：补充 DoseWarning DTO 字段定义（drugId、warningType、message、severity）

- **问题描述**：RecordGenerateRequest 缺少 dialogueText 的 50–10000 字符约束
- **所在位置**：§3.3 RecordGenerateRequest 描述
- **严重程度**：一般
- **改进建议**：追加"(必填，字符数 50–10000)"约束说明

- **问题描述**：Phase 5 迁移透明性断言缺少条件限定
- **所在位置**：§1.1 设计目标、§10 ai-api 层 DTO 扩展规格、§4.5 Converter 依赖
- **严重程度**：一般
- **改进建议**：将"业务模块代码无须修改"修订为有条件的表述
