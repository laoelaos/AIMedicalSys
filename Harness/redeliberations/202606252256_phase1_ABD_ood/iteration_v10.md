# 再审议判定报告（v10）

## 判定结果

RETRY

## 判定理由

组件B诊断报告定位到8个问题，其中严重等级2个（问题1：速率限制表头与GlobalRateLimitFilter行为矛盾；问题2：登出端点可选请求体与DTO约束不兼容），一般等级3个（问题6：SlidingWindowCounter接口契约未定义；问题7：密码变更后恢复流程异常场景未覆盖；问题8：8.3节潜在副作用/影响范围列缺失）。质询报告确认上述问题真实存在（LOCATED），实际轮次（1轮）未耗尽最大轮次（12轮）。依据判定标准，审查报告包含严重或一般等级问题，应当RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：速率限制表头声称限流维度为「同一IP（任意API路径）」，但GlobalRateLimitFilter将login/refresh端点列入白名单排除，且refresh端点无独立限流策略
- **所在位置**：4.1节速率限制表及GlobalRateLimitFilter描述
- **严重程度**：严重
- **改进建议**：修正表头为「同一IP（除login/refresh外的一般API路径）」；为refresh端点增加独立限流策略（如30次/60秒），或在设计决策中明确此风险

- **问题描述**：登出端点文档描述请求体可选携带refreshToken，但RefreshTokenRequest record标注@NotBlank，Controller层处理方式未指定
- **所在位置**：3.1.4节、4.4节、5.2节、6.1节
- **严重程度**：严重
- **改进建议**：补充Controller层签名说明，推荐使用@RequestBody(required=false)并判断null，或改为独立header传递refreshToken

- **问题描述**：SlidingWindowCounter的接口契约（锁机制、窗口精度）未定义；两套限流器返回相同ErrorCode时前端无法区分触发来源
- **所在位置**：4.1节
- **严重程度**：一般
- **改进建议**：补充SlidingWindowCounter接口契约说明；明确ErrorCode覆盖行为或确认Phase 2合并到Redis

- **问题描述**：密码变更后前端恢复流程（GET /api/auth/me → GET /api/menu/tree → 跳转首页）未覆盖中间步骤失败的异常场景
- **所在位置**：3.4节、7.4节
- **严重程度**：一般
- **改进建议**：补充异常场景处理策略：任一失败时显示loading/错误状态而非直接跳转首页；定义重试机制

- **问题描述**：8.3节A1和A3行缺少「潜在副作用」和「影响范围」列，与8.1/8.2节格式不一致
- **所在位置**：8.3节
- **严重程度**：一般
- **改进建议**：为A1补充「潜在副作用：无（DDL约束已存在）」「影响范围：schema.sql」；为A3补充副作用说明和影响范围
