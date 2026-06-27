# 设计审查报告（v17 r1）

## 审查结果
APPROVED

## 发现

无严重或一般性问题。

设计准确覆盖了 task_v17.md 全部要求：
- AuthController.java：移除 JwtUtil 字段/导入/构造函数参数，内联 extractToken() 使用硬编码 "Bearer " 前缀，保留 jwtTokenProvider 和 Claims 导入
- AuthControllerTest.java：移除 JwtConfig/JwtUtil 导入、字段、@BeforeEach 构造逻辑，构造函数简化为 2 参数，保留全部 11 个测试用例
- 行为契约表（5 种输入）与原 JwtUtil.extractToken(authHeader, "Bearer") 完全一致
- 文件路径、包结构、import 变动与实际代码一致
