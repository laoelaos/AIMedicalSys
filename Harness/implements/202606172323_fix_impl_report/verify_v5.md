# 验证报告（v5）

## 结果
FAILED

## 统计
- 通过：35
- 失败：2

## 测试执行日志

 Test Files  1 failed | 1 passed (2)
      Tests  2 failed | 35 passed (37)
   Start at  00:31:49
   Duration  382ms

### Failed Tests (2)

1. `src/api/__tests__/interceptors.test.ts > Success interceptor > returns BusinessError when code is not SUCCESS`
   - AssertionError: expected Promise{} to deeply equal { code: 'BUSINESS_ERROR', ... }
   - Cause: success interceptor returns Promise.resolve() for non-SUCCESS, test does not await

2. `src/api/__tests__/interceptors.test.ts > Success interceptor > returns BusinessError with empty message fallback when message is undefined`
   - AssertionError: expected Promise{} to deeply equal { code: 'UNKNOWN_ERROR', ... }
   - Cause: success interceptor returns Promise.resolve() for non-SUCCESS, test does not await

### Passed Tests (35)

All 35 tests in `src/types/__tests__/types.test.ts` and `src/api/__tests__/interceptors.test.ts` (excluding the 2 failed above) passed.
