# Package C Issues Fix Plan

## Background

Two issues from PR feedback + three issues identified from code review need fixing for the Patient module (Package C).

---

## Issue 1: Login "登录已过期，请重新登录"

### Root Cause Analysis

After thorough code tracing through the entire authentication chain:

1. **Backend JWT generation**: `JwtTokenProvider` generates tokens with `userId`, `userType`, `type: "access"` claims. Secret from `application-dev.yml` (64-char Base64) → `Base64.getUrlDecoder()`. Correct.
2. **JWT filter**: `JwtAuthenticationFilter` uses `findWithDetailsById()` with `@EntityGraph` (eager loads `roles`, `posts`, `posts.functions`). Collects authorities as `ROLE_PATIENT`.
3. **Security config**: `/api/patient/login` is `permitAll()`. `/api/patient/**` requires `hasRole("PATIENT")`.
4. **Frontend flow**: LoginPage calls `auth.login(form)` → saves tokens → calls `fetchProfile()`.
5. **Frontend interceptor**: On 401, returns `"登录已过期，请重新登录"` as BusinessError.

The most likely cause: `fetchProfile()` fails with 401 after login, but the error message is too generic. The JWT token from login is valid but something between login and fetchProfile causes the token to become invalid/unavailable.

### Fixes (4 files):

**A) `AIMedical/frontend/apps/patient/src/views/LoginPage.vue`**: 
- Add console logging to diagnose actual error response
- Show specific error code alongside the message
- Better error handling for the fetchProfile case

**B) `AIMedical/frontend/apps/patient/src/stores/auth.ts`**: 
- Add error state tracking
- Return more specific error info from login/fetchProfile

**C) `AIMedical/frontend/packages/shared/src/api/index.ts`**:
- In `loginApi()`, check if the interceptor actually returns valid data before calling `saveTokens()`
- Add `console.error` logging on auth failures for debugging

**D) `AIMedical/backend/modules/common-module/common-module-impl/.../security/JwtAuthenticationFilter.java`**:
- Add `log.warn()` when token validation fails (currently silent `log.debug`)
- Add `log.warn()` when user not found

---

## Issue 2: jacoco.skip.check = true

**File**: `AIMedical/backend/modules/patient/pom.xml` line 59

**Change**: `<jacoco.skip.check>true</jacoco.skip.check>` → `<jacoco.skip.check>false</jacoco.skip.check>`

This enables JaCoCo coverage threshold enforcement (LINE >= 0.50, BRANCH >= 0.40) for the patient module in CI.

---

## Issue 3: T13 - Frontend UserInfo Type Missing phone/email

**File**: `AIMedical/frontend/packages/shared/src/types/index.ts` lines 79-86

**Change**: Add `phone: string` and `email: string` fields to the `UserInfo` interface, matching the backend `UserInfoResponse` record which has `phone` (Java `String`) and `email` fields.

The backend `UserInfoResponse` record at `common-module-api/.../auth/UserInfoResponse.java` has:
```java
public record UserInfoResponse(
    Long id, String username, String realName, String phone, String email,
    String role, String position, Set<String> permissions
) {}
```

Jackson SNAKE_CASE serializes `phone` → `phone` and `email` → `email`. Frontend needs to match.

---

## Issue 4: T32 - Patient Module Direct Dependency on common-module-impl

**Files affected**:
- `AIMedical/backend/modules/patient/pom.xml` (remove common-module-impl dependency)
- `AIMedical/backend/modules/patient/.../service/impl/PatientServiceImpl.java` (change imports)

**Plan**:
1. Create a `UserFacade` interface in `common-module-api/.../user/UserFacade.java` that exposes the methods PatientServiceImpl needs (`findByUsername`, `findById`)
2. Implement `UserFacadeImpl` in `common-module-impl/.../user/UserFacadeImpl.java` delegating to `UserRepository`
3. Update `PatientServiceImpl` to inject `UserFacade` instead of `UserRepository`
4. Update `PatientEntity` to remove direct `User` reference (use `userId` only)
5. Remove `common-module-impl` from patient's `pom.xml` dependencies

**Caution**: Note the PR feedback report says "已在 Package B 分支 API 层封装后消除". Since we're on Package C, we should implement this refactoring carefully, ensuring no breakage.

---

## Issue 5: T36 - Patient Frontend Test Framework

**Files**:
- `AIMedical/frontend/apps/patient/package.json`
- `AIMedical/frontend/apps/patient/vitest.config.ts` (new)

**Changes**:
1. Add `vitest` (v2.1.0, matches root) and `@vue/test-utils` to devDependencies
2. Add `jsdom` for DOM simulation
3. Add `test` and `test:coverage` scripts
4. Create `vitest.config.ts` (reference the shared package's `vitest.config.ts` as template)
5. Create a basic test for `LoginPage.vue` to verify the framework works

---

## Execution Order

1. Fix jacoco.skip.check (simple, no deps)
2. Fix T13 UserInfo type (simple, no deps)
3. Fix login issue (critical, affects usability)
4. Fix T36 test framework (depends on login fix for useful tests)
5. Fix T32 architecture dependency (most complex, last)
