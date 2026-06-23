# 再审议判定报告（v7）

## 判定结果

RETRY

## 判定理由

组件B诊断报告检出4个问题（1个严重、3个一般），质询报告确认为LOCATED（所有审查结论均成立）。根据判定标准，审查报告包含严重或一般等级的问题，判定为RETRY。

## 需要解决的问题

- **问题描述**：common 模块缺少 `spring-boot-starter-data-jpa` 依赖声明，直接编码将导致编译失败
- **所在位置**：2.2 节 common 模块依赖描述
- **严重程度**：严重
- **改进建议**：在 2.2 节 common 模块的依赖说明中明确加入 `spring-boot-starter-data-jpa`，标注用途；检查 common-module 是否需显式声明该依赖

- **问题描述**：真实 AiService 实现与 FallbackAiService 的 Bean 共存机制未定义，Phase 2+ 装配存在设计漏洞
- **所在位置**：3.4 节「Bean 装配策略」及「装配条件汇总表」
- **严重程度**：一般
- **改进建议**：选择方案 A（取消 @Primary，改由 AiServiceConfig 显式构造）或方案 B（保持 @Primary，改 @ConditionalOnProperty），更新装配条件汇总表

- **问题描述**：`ScheduleRequest.doctorIds` 字段类型与系统其他 doctor ID 字段类型不一致
- **所在位置**：8.2 节 ScheduleRequest DTO 定义
- **严重程度**：一般
- **改进建议**：将 `ScheduleRequest.doctorIds` 的类型统一为 `List<Long>`

- **问题描述**：`springdoc-openapi` 集成缺少依赖归属声明
- **所在位置**：8.3 节
- **严重程度**：一般
- **改进建议**：明确 springdoc-openapi 依赖的归属模块，在父 POM 的 `<dependencyManagement>` 中统一版本声明，各业务模块按需引入
