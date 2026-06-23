# AI 代码生成规范

> **生效范围**：本规范适用于智慧云脑诊疗平台（AIMedicalSys）的所有代码生成工作。
> **版本**：v1.0
> **最后更新**：2026-06-23

---

## 1. 技术栈与版本规范

### 1.1 后端技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 | LTS 版本 |
| 框架 | Spring Boot | 3.2.5 | 后端核心框架 |
| 数据库 | MySQL | 8.0+ | 主数据库 |
| ORM | Spring Data JPA | 3.2.x | 数据持久化 |
| 认证 | JWT | — | 无状态认证 |
| API文档 | SpringDoc OpenAPI | 2.5.0 | API文档生成 |
| 工具 | Lombok | 1.18.x | 简化代码 |

**必须遵守**：
- 所有后端代码必须基于 Spring Boot 3.2.x 编写
- Java 语法必须符合 Java 17 标准
- 禁止使用已废弃的 API（如 javax.* 包，改用 jakarta.*）

**禁止**：
- 禁止混合使用其他 ORM 框架（如 MyBatis）
- 禁止使用低于 Java 17 的版本

### 1.2 前端技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | TypeScript | 5.4.x | 类型安全 |
| 框架 | Vue | 3.4.x | 前端核心框架 |
| 构建 | Vite | 5.2.x | 构建工具 |
| 路由 | Vue Router | 4.3.x | 路由管理 |
| 状态管理 | Pinia | 2.1.x | 状态管理 |
| UI组件 | Element Plus | 2.7.x | UI组件库 |
| HTTP客户端 | Axios | 1.6.x | 网络请求 |

**必须遵守**：
- 所有前端代码必须使用 TypeScript
- Vue 组件必须使用组合式 API（setup 语法糖）
- 状态管理必须使用 Pinia

**禁止**：
- 禁止使用 Vue 2 选项式 API
- 禁止直接操作 DOM（必须通过 Vue 响应式系统）

---

## 2. 目录分层规则

### 2.1 后端目录结构

```
backend/
├── application/           # 启动模块（仅包含启动类和配置）
│   └── src/main/java/com/aimedical/
│       ├── Application.java          # 启动类
│       ├── config/                   # 全局配置
│       └── HealthController.java     # 健康检查
├── common/                # 通用模块（跨模块复用）
│   └── src/main/java/com/aimedical/common/
│       ├── base/             # 基类（BaseEntity、BaseEnum）
│       ├── config/           # 通用配置
│       ├── exception/        # 异常定义
│       ├── result/           # 统一返回格式
│       └── util/             # 工具类
└── modules/               # 业务模块
    ├── patient/             # 患者模块
    ├── doctor/              # 医生模块
    ├── admin/               # 管理员模块
    ├── ai/                  # AI能力模块
    │   ├── ai-api/          # AI接口定义
    │   └── ai-impl/         # AI实现
    └── common-module/       # 通用业务模块
```

### 2.2 业务模块内部分层（以 patient 为例）

```
modules/patient/src/main/java/com/aimedical/modules/patient/
├── api/              # 对外暴露的 REST API（Controller）
├── service/          # 业务逻辑层
│   └── impl/         # 服务实现
├── repository/       # 数据访问层（Repository）
├── entity/           # 数据库实体（Entity）
├── dto/              # 数据传输对象
│   ├── request/      # 请求DTO
│   └── response/     # 响应DTO
└── converter/        # DTO与Entity转换
```

**各层职责说明**：

| 层级 | 职责 | 必须遵守 |
|------|------|----------|
| **Controller** | 处理HTTP请求，参数校验，调用Service | 禁止包含业务逻辑 |
| **Service** | 封装业务逻辑，事务管理 | 禁止直接操作数据库 |
| **Repository** | 数据访问，执行SQL | 禁止包含业务逻辑 |
| **Entity** | 数据库表映射 | 必须继承 BaseEntity |
| **DTO** | 数据传输载体 | 禁止包含业务逻辑 |
| **Converter** | DTO与Entity转换 | 禁止包含业务逻辑 |

**必须遵守**：
- 严格遵循 Controller → Service → Repository 的调用链
- 每个模块必须包含上述所有层级
- Entity 必须继承 `BaseEntity`

**禁止**：
- 禁止跨层调用（如 Controller 直接调用 Repository）
- 禁止在 Entity 或 DTO 中编写业务逻辑

### 2.3 前端目录结构

```
frontend/
├── apps/                    # 应用入口
│   ├── patient/             # 患者端
│   ├── doctor/              # 医生端
│   └── admin/               # 管理员端
│       └── src/
│           ├── views/        # 页面视图
│           ├── components/   # 组件
│           ├── stores/       # Pinia状态管理
│           ├── router/       # 路由配置
│           ├── api/          # API调用封装
│           ├── types/        # TypeScript类型定义
│           └── utils/        # 工具函数
└── packages/                # 共享包
    ├── shared/              # 共享API和类型
    └── ui-core/             # UI组件库
```

**必须遵守**：
- 页面级组件放在 `views/`，可复用组件放在 `components/`
- API 调用统一封装在 `api/` 目录
- 类型定义统一放在 `types/` 目录

**禁止**：
- 禁止在组件中直接写复杂业务逻辑
- 禁止在多个地方重复定义相同的类型

---

## 3. 命名规范

### 3.1 文件命名

| 文件类型 | 命名规则 | 示例 |
|----------|----------|------|
| Controller | 大驼峰 + Controller 后缀 | `PatientController.java` |
| Service 接口 | 大驼峰 + Service 后缀 | `PatientService.java` |
| Service 实现 | 大驼峰 + ServiceImpl 后缀 | `PatientServiceImpl.java` |
| Repository | 大驼峰 + Repository 后缀 | `PatientRepository.java` |
| Entity | 大驼峰 + Entity 后缀 | `PatientEntity.java` |
| DTO 请求 | 大驼峰 + Request 后缀 | `PatientCreateRequest.java` |
| DTO 响应 | 大驼峰 + Response 后缀 | `PatientResponse.java` |
| Converter | 大驼峰 + Converter 后缀 | `PatientConverter.java` |
| 工具类 | 大驼峰 + Util 后缀 | `JwtUtil.java` |
| 配置类 | 大驼峰 + Config 后缀 | `SecurityConfig.java` |

**必须遵守**：
- 所有文件必须使用大驼峰命名法（PascalCase）
- 文件命名必须清晰反映其职责

**禁止**：
- 禁止使用下划线命名（如 `patient_controller.java`）
- 禁止使用拼音命名（如 `HuanZheController.java`）

### 3.2 类命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 类名 | 大驼峰（PascalCase） | `PatientService` |
| 接口名 | 大驼峰 | `AiService` |
| 枚举名 | 大驼峰 | `PositionEnum` |
| 异常类 | 大驼峰 + Exception 后缀 | `BusinessException.java` |

### 3.3 方法命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 获取单个 | `getById(Long id)` | `getById(1L)` |
| 获取列表 | `list(Condition condition)` | `list(patientQuery)` |
| 获取分页 | `page(PageQuery query)` | `page(pageQuery)` |
| 创建 | `create(Request request)` | `create(patientCreateRequest)` |
| 更新 | `update(Long id, Request request)` | `update(1L, patientUpdateRequest)` |
| 删除 | `delete(Long id)` | `delete(1L)` |
| 业务方法 | 动词开头，清晰描述 | `registerPatient()` |

**必须遵守**：
- 方法名使用小驼峰（camelCase）
- 方法名必须清晰描述其功能

**禁止**：
- 禁止使用单个字母命名（如 `a()`）
- 禁止使用无意义的命名（如 `doSomething()`）

### 3.4 变量命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 普通变量 | 小驼峰 | `patientName`, `totalCount` |
| 常量 | 全大写 + 下划线 | `MAX_PAGE_SIZE`, `JWT_SECRET` |
| 布尔变量 | 以 `is` 或 `has` 开头 | `isActive`, `hasPermission` |
| 集合变量 | 复数形式 | `patients`, `doctors` |

**必须遵守**：
- 变量名使用小驼峰（camelCase）
- 常量必须使用全大写 + 下划线

**禁止**：
- 禁止使用拼音变量名（如 `xingMing`）
- 禁止使用缩写（如 `usrNm`）

---

## 4. 统一返回格式与异常处理

### 4.1 统一返回格式

后端所有接口必须使用 `Result<T>` 封装返回：

```java
public class Result<T> {
    private String code;      // 状态码
    private String message;   // 提示信息
    private T data;          // 数据
    
    // 成功响应
    public static <T> Result<T> success(T data) { ... }
    
    // 失败响应
    public static <T> Result<T> fail(String code, String message) { ... }
    public static <T> Result<T> fail(ErrorCode errorCode) { ... }
}
```

**必须遵守**：
- 成功响应：`code = "SUCCESS"`，`data` 为返回数据
- 失败响应：`code` 为错误码，`message` 为错误信息

**禁止**：
- 禁止直接返回实体对象（必须封装在 Result 中）
- 禁止返回非标准格式

### 4.2 错误码规范

| 错误码 | 含义 | HTTP状态码 |
|--------|------|-----------|
| SUCCESS | 成功 | 200 |
| FAIL | 业务失败 | 200 |
| UNAUTHORIZED | 未认证 | 401 |
| FORBIDDEN | 无权限 | 403 |
| VALIDATION_ERROR | 参数校验失败 | 400 |
| NOT_FOUND | 资源不存在 | 404 |
| SYSTEM_ERROR | 系统错误 | 500 |

### 4.3 异常处理

#### 4.3.1 异常分类

| 异常类型 | 说明 | 使用场景 |
|----------|------|----------|
| `BusinessException` | 业务异常 | 业务逻辑校验失败 |
| `ValidationException` | 参数校验异常 | 参数格式错误 |
| `ResourceNotFoundException` | 资源不存在异常 | 查询不到数据 |
| `AuthenticationException` | 认证异常 | 登录失败、Token过期 |

#### 4.3.2 全局异常处理

必须通过 `GlobalExceptionHandler` 统一处理异常：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) { ... }
    
    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) { ... }
    
    // 处理资源不存在异常
    @ExceptionHandler(ResourceNotFoundException.class)
    public Result<Void> handleResourceNotFoundException(ResourceNotFoundException e) { ... }
    
    // 处理系统异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) { ... }
}
```

**必须遵守**：
- 所有异常必须继承自 `RuntimeException`
- 业务异常必须使用 `BusinessException`
- 异常信息必须清晰描述错误原因

**禁止**：
- 禁止直接抛出 `Exception`
- 禁止在 Controller 中使用 try-catch 处理业务异常

### 4.4 参数校验

#### 4.4.1 后端参数校验

使用 JSR-380 注解进行参数校验：

```java
public class PatientCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
    private String username;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Min(value = 0, message = "年龄不能为负数")
    @Max(value = 150, message = "年龄不能超过150")
    private Integer age;
}
```

**必须遵守**：
- Controller 方法参数必须添加 `@Valid` 注解
- 校验失败必须抛出 `MethodArgumentNotValidException`
- 校验注解必须包含中文错误信息

**禁止**：
- 禁止在业务层手动校验参数（必须使用注解）
- 禁止使用 System.out.println 输出校验信息

#### 4.4.2 前端参数校验

使用 Element Plus 的表单校验：

```typescript
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度必须在2-50之间', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}
```

---

## 5. 代码注释规范

### 5.1 类注释

```java
/**
 * 患者服务接口
 * 
 * <p>提供患者相关的业务操作，包括注册、查询、更新等功能。
 * 
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface PatientService {
    // ...
}
```

### 5.2 方法注释

```java
/**
 * 创建患者
 * 
 * @param request 患者创建请求
 * @return 创建成功的患者信息
 * @throws BusinessException 当用户名已存在时抛出
 */
PatientResponse create(PatientCreateRequest request);
```

### 5.3 字段注释

```java
public class PatientEntity extends BaseEntity {
    
    /**
     * 用户名（登录账号）
     */
    private String username;
    
    /**
     * 密码（BCrypt加密）
     */
    private String password;
}
```

**必须遵守**：
- 所有类、方法、字段必须添加注释
- 注释必须使用 Javadoc 格式
- 注释语言必须使用中文

**禁止**：
- 禁止使用无意义的注释（如 `// 用户名`）
- 禁止注释与代码不一致

### 5.4 导入规则

#### 5.4.1 导入顺序（后端）

1. Java 标准库（java.*, javax.*, jakarta.*）
2. 第三方库（org.*, com.* 非本项目）
3. 本项目内部包（com.aimedical.*）

```java
// 1. 标准库
import java.time.LocalDateTime;
import java.util.List;

// 2. 第三方库
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 3. 本项目
import com.aimedical.common.base.BaseEntity;
import com.aimedical.modules.patient.entity.PatientEntity;
```

#### 5.4.2 导入顺序（前端）

1. 第三方库（vue, pinia, axios 等）
2. 本项目共享包（@aimedical/shared）
3. 本项目内部模块

```typescript
// 1. 第三方库
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { defineStore } from 'pinia'

// 2. 共享包
import { apiPost } from '@aimedical/shared/api'
import type { UserInfo } from '@aimedical/shared/types'

// 3. 本项目
import type { LoginRequest } from '../types'
```

**必须遵守**：
- 导入必须按顺序分组，组间用空行分隔
- 禁止使用 `import * from`（除非明确需要）

**禁止**：
- 禁止导入未使用的包
- 禁止循环导入

---

## 6. 代码风格规范

### 6.1 缩进与换行

**必须遵守**：
- 使用 4 个空格进行缩进（禁止使用 Tab）
- 每行代码长度不超过 120 字符
- 左大括号 `{` 必须与声明在同一行

```java
// 正确
public void create(PatientCreateRequest request) {
    // 代码
}

// 错误
public void create(PatientCreateRequest request) 
{
    // 代码
}
```

### 6.2 空行规则

**必须遵守**：
- 类成员之间空一行
- 方法之间空两行
- 逻辑块之间空一行

```java
public class PatientServiceImpl implements PatientService {
    
    private final PatientRepository patientRepository;
    
    public PatientServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }
    
    // 方法之间空两行
    @Override
    public PatientResponse create(PatientCreateRequest request) {
        // 业务逻辑
        
        // 逻辑块之间空一行
        PatientEntity entity = PatientConverter.toEntity(request);
        
        // 逻辑块之间空一行
        return PatientConverter.toResponse(entity);
    }
}
```

### 6.3 条件语句

**必须遵守**：
- `if-else` 语句必须使用大括号，即使只有一行
- 三元运算符只用于简单判断，禁止嵌套

```java
// 正确
if (user != null) {
    processUser(user);
} else {
    throw new ResourceNotFoundException("用户不存在");
}

// 错误
if (user != null) processUser(user);
```

### 6.4 异常抛出

**必须遵守**：
- 异常信息必须清晰描述原因
- 禁止抛出空信息的异常

```java
// 正确
throw new BusinessException("用户名已存在");

// 错误
throw new BusinessException();
throw new BusinessException("");
```

---

## 7. 数据库规范

### 7.1 表命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 业务表 | 小写 + 下划线 | `sys_user`, `patient_record` |
| 关联表 | 小写 + 下划线（按字母顺序） | `sys_user_role`, `sys_role_menu` |

**必须遵守**：
- 表名必须使用小写字母
- 禁止使用驼峰命名

### 7.2 字段命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 普通字段 | 小写 + 下划线 | `user_name`, `created_at` |
| 主键 | `id` | `id` |
| 外键 | 表名 + `_id` | `patient_id`, `role_id` |

**必须遵守**：
- 字段名必须使用小写字母
- 禁止使用驼峰命名

### 7.3 Entity 规范

**必须遵守**：
- Entity 必须继承 `BaseEntity`
- 表名使用 `@Entity` 和 `@Table(name = "...")` 注解
- 字段使用 JPA 注解（`@Column`, `@ManyToOne`, `@OneToMany`）

```java
@Entity
@Table(name = "sys_user")
public class UserEntity extends BaseEntity {
    
    @Column(name = "user_name", nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(name = "password", nullable = false, length = 255)
    private String password;
}
```

---

## 8. 安全规范

### 8.1 密码处理

**必须遵守**：
- 密码必须使用 BCrypt 加密存储
- 禁止明文存储密码
- 禁止日志输出密码

### 8.2 JWT 规范

**必须遵守**：
- Token 必须设置过期时间
- Token 必须使用安全的签名算法
- 禁止将敏感信息放入 Token

### 8.3 权限控制

**必须遵守**：
- 使用 Spring Security 进行权限控制
- 每个接口必须添加权限注解
- 禁止硬编码权限判断

---

## 9. 前端规范补充

### 9.1 组件规范

**必须遵守**：
- 组件名使用大驼峰（PascalCase）
- 组件必须有清晰的职责
- 组件通信优先使用 Props/Events

**禁止**：
- 禁止在组件中直接修改 Props
- 禁止使用 `$parent` / `$children` 访问组件

### 9.2 状态管理规范

**必须遵守**：
- 全局状态使用 Pinia Store
- Store 必须按功能划分模块
- 禁止在多个 Store 中重复存储相同数据

### 9.3 API 调用规范

**必须遵守**：
- API 调用统一封装在 `api/` 目录
- 使用 Axios 拦截器统一处理响应
- 错误处理必须使用统一的错误码

---

## 附录：代码模板

### A.1 Controller 模板

```java
@RestController
@RequestMapping("/api/patient")
public class PatientController {
    
    private final PatientService patientService;
    
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }
    
    @GetMapping("/{id}")
    public Result<PatientResponse> getById(@PathVariable Long id) {
        return Result.success(patientService.getById(id));
    }
    
    @GetMapping
    public Result<List<PatientResponse>> list(PatientQuery query) {
        return Result.success(patientService.list(query));
    }
    
    @PostMapping
    public Result<PatientResponse> create(@Valid @RequestBody PatientCreateRequest request) {
        return Result.success(patientService.create(request));
    }
    
    @PutMapping("/{id}")
    public Result<PatientResponse> update(@PathVariable Long id, @Valid @RequestBody PatientUpdateRequest request) {
        return Result.success(patientService.update(id, request));
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return Result.success(null);
    }
}
```

### A.2 Service 模板

```java
public interface PatientService {
    
    PatientResponse getById(Long id);
    
    List<PatientResponse> list(PatientQuery query);
    
    PatientResponse create(PatientCreateRequest request);
    
    PatientResponse update(Long id, PatientUpdateRequest request);
    
    void delete(Long id);
}
```

### A.3 Store 模板（前端）

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiGet, apiPost } from '@aimedical/shared/api'
import type { UserInfo, LoginRequest } from '@aimedical/shared/types'

export const useAuthStore = defineStore('auth', () => {
    const token = ref('')
    const user = ref<UserInfo | null>(null)
    const isAuthenticated = ref(false)
    
    async function login(request: LoginRequest) {
        const response = await apiPost('/api/auth/login', request)
        if (response.code === 'SUCCESS') {
            token.value = response.data.token
            user.value = response.data.user
            isAuthenticated.value = true
        }
    }
    
    async function logout() {
        token.value = ''
        user.value = null
        isAuthenticated.value = false
    }
    
    return {
        token,
        user,
        isAuthenticated,
        login,
        logout
    }
})
```

---

## 总结

本规范旨在统一项目代码风格，提高代码质量和可维护性。所有开发者必须严格遵守本规范，确保代码的一致性和规范性。

**核心原则**：
1. **清晰性**：代码必须易于理解
2. **一致性**：遵循统一的命名和格式
3. **安全性**：重视安全规范
4. **可维护性**：便于后续维护和扩展