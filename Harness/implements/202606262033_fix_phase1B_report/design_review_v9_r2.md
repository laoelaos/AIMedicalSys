# 设计审查报告（v9 r2）

## 审查结果
APPROVED

## 发现

无严重或一般缺陷。设计全面地覆盖了 T6（API 路径修正）和 T7（SecurityContext 重构）的完整变更范围，包括：

- **源文件修改**：`@PutMapping("/me")` → `@PutMapping("/profile")`；`changePassword()` 重构移除 JwtTokenProvider 依赖，新增 `getCurrentUserId()` 私有方法；字段、构造参数、import 的增删改
- **测试文件修改**：删除 `@Mock JwtTokenProvider`、`setUp()` 构造参数调整、`ChangePasswordTests` 从 mock JwtTokenProvider 改为 mock SecurityContextHolder、import 变更
- **行为契约**：前置/后置条件清晰，不受影响文件范围明确
- **错误处理**：与 MenuController 模式一致，测试策略协调
- **v9 r1 修订**：已补全 AuthControllerTest import 中缺失的 `SecurityContextHolder`

所有要求均已覆盖，设计细节明确、无歧义，可直接进入编码阶段。
