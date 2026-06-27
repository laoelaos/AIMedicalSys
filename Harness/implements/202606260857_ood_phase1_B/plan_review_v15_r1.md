# 计划审查报告（v15 r1）

## 审查结果
REJECTED

## 发现

### [一般] 计划上下文遗漏 JwtTokenProvider 的两个公开方法

计划第 154 行列出 JwtTokenProvider 的 5 个方法，但 task_v15.md 第 31-32 行定义了 7 个公开方法。遗漏：
- `getTokenVersionFromClaims(Claims claims)` — 返回 int，用于 Refresh Token 版本校验
- `getAccessTokenExpirationMs()` — 返回 long，Access Token 有效期毫秒值

这两个方法不是核心 CRUD 可能容易被遗漏，计划应完整列出所有公开方法签名以确保实现 Agent 覆盖。

**期望修正**：在计划上下文"JwtTokenProvider 封装"列表中补充 `getTokenVersionFromClaims(Claims)` 和 `getAccessTokenExpirationMs()`。

---

### [一般] 新建文件清单未明确列出测试文件名

计划 R15 的"新建文件（6 个）"下仅列出 3 个生产文件的路径，剩余 3 个测试文件仅写"对应的 3 个测试文件"而未给出具体文件名和路径。前序轮次（R13、R14）均逐行列出所有文件（含测试文件路径），此写法破坏了既有惯例。

缺少的测试文件路径：
- `modules/common-module/common-module-impl/src/test/java/.../auth/jwt/JwtTokenProviderTest.java`
- `modules/common-module/common-module-impl/src/test/java/.../auth/converter/UserConverterTest.java`
- `modules/common-module/common-module-impl/src/test/java/.../auth/config/AuthModuleConfigTest.java`

**期望修正**：参考 R14 格式（第 95-104 行），将所有 6 个文件的完整相对路径逐一列出。

---

### [轻微] 计划将 UserConverter 描述为"接口"与任务定义不符

计划第 155 行称"UserConverter 接口"，但 task_v15.md 第 41 行明确 UserConverter 的形态为 `@Component` class，无接口（"类型关系：无接口"）。

**期望修正**：将"UserConverter 接口"改为"UserConverter 转换器"或类似描述。

## 修改要求

- **[一般] 问题 1**：计划第 154 行所列 JwtTokenProvider 方法列表不完整。应在 `getJtiFromToken(token)` 后追加 `getTokenVersionFromClaims(Claims)` 和 `getAccessTokenExpirationMs()`。
- **[一般] 问题 2**：计划第 140-144 行"新建文件（6 个）"中，3 个测试文件需像前序轮次那样逐行列明完整路径而非仅写"对应的 3 个测试文件"。
- **[轻微] 问题 3**：计划第 155 行"UserConverter 接口"中的"接口"应删除或改为更准确的描述。
