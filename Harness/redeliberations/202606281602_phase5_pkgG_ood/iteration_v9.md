# 再审议判定报告（v9）

## 判定结果

RETRY

## 判定理由

诊断报告识别出 4 个质量问题（2 重要、2 中等），质询报告确认全部 LOCATED，实际轮次（1）< 最大轮次（12），说明审查准确命中问题后提前终止。4 个问题均非轻微等级，不满足 PASS 条件，需重新运行组件 A 进行修正。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：§3.7 PrescriptionLocalRuleFallback 引用 PrescriptionCheckRequest 上不存在的字段 allergyInfo，应为 request.patientInfo.getAllergyInfo() 或补充字段定义
- **所在位置**：§3.7 最小安全规则表第 4 行（line 2160）
- **严重程度**：严重
- **改进建议**：方案 A：数据来源改为 request.patientInfo 并注明方法签名；方案 B：在 §3.11.2 补充 allergyInfo 字段定义

- **问题描述**：§4.1 doExecuteInternal() 伪代码中 parsedResult 变量在 try 块内定义、try 块外引用，真实 Java 代码会编译错误
- **所在位置**：§4.1 伪代码（line 2809~2810、line 2863、line 2906~2916）
- **严重程度**：严重
- **改进建议**：在 try 块前声明 Object parsedResult = null，两个成功路径仅赋值，访问前添加 null 守卫检查

- **问题描述**：§3.5 聚合 SQL 使用 MySQL 不支持的 PERCENTILE_CONT，与文档声明的 MySQL 方言矛盾
- **所在位置**：§3.5 聚合 SQL（line 2060~2062）
- **严重程度**：一般
- **改进建议**：替换为 MySQL 兼容的百分位计算方式（如 ROW_NUMBER() + COUNT(*) 或 PERCENT_RANK()），或添加注释说明各数据库方言替代函数

- **问题描述**：多个 @Scheduled 任务（指标清理、CredentialProvider 退避、ModelRouter 轮询）缺少调度线程池配置定义，Spring 默认单线程可能因 DDL 元数据锁阻塞其他任务
- **所在位置**：§3.2、§3.5、§6.1、§3.9
- **严重程度**：一般
- **改进建议**：在 §3.9 AiPlatformConfig 中补充 @Bean("scheduledTaskExecutor") 配置 ThreadPoolTaskScheduler pool size ≥ 2~3
