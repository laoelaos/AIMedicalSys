# 实现报告（v11）

## 概述

修复 `Docs/05_ood_phase1_B.md` 中的 5 处 OOD 文档缺陷：T8（BusinessException args 示例值错误）、T2-OOD（4.2 节补充异常刷新阻断逻辑定义）、T12-OOD（4.7 节密钥字符集迁移策略与验证顺序修正）、T13-OOD（4.1 节锁粒度描述更正）、T17-OOD（10.3 节补充 AuthenticationEntryPoint/AccessDeniedHandler 插值出口）。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `Docs/05_ood_phase1_B.md` | 5 处局部文本修改 |

## 变更细节

### T8 — 修正 BusinessException args 示例值

- **位置 1**（3.1.1 节末尾段落）：`"请30分钟后重试"` → `"30分钟"`，`"请15分钟后重试"` → `"15分钟"`
- **位置 2**（10.3 节 args 示例代码块）：同上

### T2-OOD — 补充异常刷新检测阻断逻辑（4.2 节）

- 标题从"异常刷新检测"改为"异常刷新检测与阻断"
- 新增阻断逻辑描述：拒绝请求、返回 TOKEN_REFRESH_FAILED、插入位置（步骤 3 之后、步骤 4 之前）
- 新增内存回收机制描述：惰性清除 + `ScheduledExecutorService` 每 60 秒定期清理
- 告警日志消息格式与阻断逻辑同时触发

### T12-OOD — 明确 URL-safe 密钥字符集迁移策略（4.7 节）

- **位置 1**（启动验证逻辑）：验证顺序对齐实际代码（字符集检查→解码→长度检查），错误消息对齐 `JwtTokenProvider`
- **位置 2**（新增过渡策略）：新增"从标准 Base64 迁移的过渡策略"小节，含方案 A（双密钥过渡）和方案 B（一次性重新生成）

### T13-OOD — 澄清锁粒度描述（4.1 节）

- **位置 1**（段落描述）：`ReentrantLock` → `ConcurrentHashMap.compute` 闭包原子访问
- **位置 2**（SlidingWindowCounter 契约·线程安全）：同上，删除 ReentrantLock 引用

### T17-OOD — 补充 AuthenticationEntryPoint/AccessDeniedHandler 插值出口（10.3 节）

- **位置 1**（管线图之后）：新增 EntryPoint/DeniedHandler 分支到管线图中
- **位置 2**（关键设计决策 item 6 之后）：新增 item 7~9，含 MessageInterpolator 接口定义、SimpleMessageInterpolator 说明、管线全貌更新图、设计说明

## 编译验证

不适用（文档修改，无代码变更）

## 设计偏差说明

无偏差。所有修改严格按 `detail_v11.md` 中的逐条变更详述执行。
