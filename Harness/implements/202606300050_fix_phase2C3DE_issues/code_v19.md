# 实现报告（v19）

## 概述
修复 phase2 C3 DE 实现报告中的 6 个问题（C07/A02/A10/A05/A06/A01），涉及 AI 超时注入、配置补齐、MockAiService 重构、降级框架完善和 AiResultFactory 迁移。共修改 14 个文件，新建 2 个文件。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `consultation/.../TriageServiceImpl.java` | 注入 aiTimeout + future.get 超时 + AiResultFactory 替换 |
| 修改 | `prescription/audit/.../PrescriptionAuditServiceImpl.java` | 注入 aiTimeout + future.get 超时 |
| 修改 | `prescription/assist/.../PrescriptionAssistServiceImpl.java` | 注入 aiTimeout + future.get 超时 |
| 修改 | `ai/ai-impl/.../mock/MockAiService.java` | @Profile 切换 + 3 种策略模式 |
| 新建 | `ai/ai-impl/.../mock/MockAdminController.java` | 运行时切换 mock 策略端点 |
| 修改 | `ai/ai-api/.../degradation/DegradationContext.java` | 增加 serviceName/operationName 字段 |
| 修改 | `ai/ai-impl/.../fallback/FallbackAiService.java` | selectDelegate 方法 + 13 个方法替换 |
| 修改 | `application/src/main/resources/application.yml` | 追加 AI 超时/mock/consultation 配置 |
| 修改 | `application/src/test/resources/application.yml` | 追加相同配置键 |
| 修改 | `consultation/.../TriageServiceImplTest.java` | 构造参数追加 aiTimeout |
| 修改 | `prescription/audit/.../PrescriptionAuditServiceImplTest.java` | 构造参数追加 aiTimeout（2 处） |
| 修改 | `prescription/assist/.../PrescriptionAssistServiceImplTest.java` | 构造参数追加 aiTimeout |
| 修改 | `ai/ai-impl/.../mock/MockAiServiceTest.java` | @Profile 验证 + 3 策略覆盖 |
| 新建 | `ai/ai-impl/.../mock/MockAdminControllerTest.java` | GET/POST 端点测试 |
| 修改 | `ai/ai-impl/.../fallback/FallbackAiServiceTest.java` | 多 delegate + 降级跳过 + 全降级回退 |
| 修改 | `ai/ai-api/.../degradation/DegradationStrategyTest.java` | serviceName/operationName 决策验证 |

## 编译验证
未执行编译验证

## 设计偏差说明
无偏差
