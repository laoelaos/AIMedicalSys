# 设计审查报告（v21 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** 缺少 Filter 存在性断言，可能产生误判。`List.indexOf()` 在元素不存在时返回 -1，当某个 Filter 从链中遗漏时 `-1 < 0` 仍为 true（例如 `GlobalRateLimitFilter` 被移除时 `idxGlobal=-1, idxJwt=0`，`-1 < 0` 通过），导致测试报告通过但实际上安全链不完整。此外，`indexOf` 基于 `equals` 判断，当同一个 Filter 类被注册多次时，返回的是第一个出现的位置，但目前不存在这种情况。

## 修改要求

在比较索引之前，先断言三个 Filter 均存在于 Filter 链中，例如：
```java
assertNotEquals(-1, idxGlobal, "GlobalRateLimitFilter should be registered");
assertNotEquals(-1, idxJwt, "JwtAuthenticationFilter should be registered");
assertNotEquals(-1, idxPwd, "PasswordChangeCheckFilter should be registered");
```
再执行顺序断言，防止因 Filter 缺失而产生的误判。
