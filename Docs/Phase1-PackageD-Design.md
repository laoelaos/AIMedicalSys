# Phase 1 包 D：医生端与管理端登录/菜单动态化开发规划

## 1. 需求分析

### 1.1 业务背景
根据 [需求规格说明书](file:///C:/Users/23545/Documents/trae_projects/AIMedicalSys/Docs/01_requirement.md)，医生端与管理端需要实现：

| 终端 | 角色 | 核心功能 |
|------|------|----------|
| 医生端 | 门诊医生、检查医生、检验医生、药房医生、线下接诊医生 | 登录、岗位标识加载、个人资料维护 |
| 管理员端 | 管理员 | 登录、用户/角色/岗位管理、菜单管理、字典管理 |

### 1.2 权限模型要求
- 三级权限模型：角色 → 岗位 → 功能
- 前端按权限动态渲染菜单与按钮
- 医生岗位细分：门诊医生、检查医生、检验医生、药房医生、线下接诊医生

### 1.3 功能范围

#### 医生端
1. **登录模块**：账号密码登录、JWT令牌获取
2. **岗位标识加载**：登录后获取当前医生岗位类型
3. **菜单动态化**：根据岗位动态渲染菜单
4. **个人资料维护**：查看/修改个人基本信息

#### 管理员端
1. **登录模块**：账号密码登录、JWT令牌获取
2. **菜单动态化**：根据管理员权限动态渲染菜单

---

## 2. 技术架构设计

### 2.1 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 | 3.4.x |
| 前端语言 | TypeScript | 5.4.x |
| 构建工具 | Vite | 5.2.x |
| UI组件库 | Element Plus | 2.7.x |
| 状态管理 | Pinia | 2.1.x |
| 路由 | Vue Router | 4.3.x |
| HTTP客户端 | Axios | 1.6.x |
| 后端框架 | Spring Boot | 3.2.x |
| 认证 | JWT | — |

### 2.2 架构分层

```
┌─────────────────────────────────────────────────────────────┐
│ 前端层 (Vue 3 + TypeScript)                                │
│ ├── DoctorApp (医生端)                                      │
│ ├── AdminApp (管理员端)                                     │
│ └── Shared (公共组件/工具)                                   │
├─────────────────────────────────────────────────────────────┤
│ 网关层 (Spring Security + JWT)                              │
│ ├── AuthenticationFilter                                    │
│ └── AuthorizationInterceptor                                │
├─────────────────────────────────────────────────────────────┤
│ 业务服务层 (Spring Boot)                                    │
│ ├── AuthService (认证服务)                                   │
│ ├── UserService (用户服务)                                   │
│ ├── RoleService (角色服务)                                   │
│ └── MenuService (菜单服务)                                   │
├─────────────────────────────────────────────────────────────┤
│ 数据持久化层 (JPA + MySQL)                                  │
│ ├── UserRepository                                          │
│ ├── RoleRepository                                          │
│ ├── MenuRepository                                          │
│ └── PermissionRepository                                    │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 核心数据模型

#### 2.3.1 用户实体 (`UserEntity`)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | Long | 用户ID | 主键 |
| username | String | 用户名 | 唯一、非空 |
| password | String | 密码（加密） | 非空 |
| realName | String | 真实姓名 | 非空 |
| role | String | 角色类型 | 枚举：DOCTOR/ADMIN |
| position | String | 岗位类型 | 医生端必填 |
| status | Boolean | 状态 | 默认true |
| createdAt | LocalDateTime | 创建时间 | — |
| updatedAt | LocalDateTime | 更新时间 | — |

#### 2.3.2 角色实体 (`RoleEntity`)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | Long | 角色ID | 主键 |
| name | String | 角色名称 | 唯一、非空 |
| code | String | 角色编码 | 唯一、非空 |
| description | String | 角色描述 | — |

#### 2.3.3 菜单实体 (`MenuEntity`)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | Long | 菜单ID | 主键 |
| parentId | Long | 父菜单ID | 可为空 |
| name | String | 菜单名称 | 非空 |
| path | String | 路由路径 | — |
| icon | String | 图标名称 | — |
| permission | String | 权限标识 | — |
| sortOrder | Integer | 排序号 | 默认0 |
| visible | Boolean | 是否可见 | 默认true |

#### 2.3.4 岗位枚举 (`PositionEnum`)

| 枚举值 | 说明 |
|--------|------|
| OUTPATIENT | 门诊医生 |
| EXAMINATION | 检查医生 |
| LABTEST | 检验医生 |
| PHARMACY | 药房医生 |
| RECEPTION | 线下接诊医生 |

---

## 3. API 接口设计

### 3.1 认证接口

| 接口路径 | HTTP方法 | 所属Controller | 功能描述 |
|----------|----------|----------------|----------|
| `/api/auth/login` | POST | AuthController | 用户登录 |
| `/api/auth/logout` | POST | AuthController | 用户登出 |
| `/api/auth/refresh` | POST | AuthController | 刷新Token |
| `/api/auth/me` | GET | AuthController | 获取当前用户信息 |

#### 3.1.1 登录接口 `/api/auth/login`

**请求体：**
```json
{
  "username": "string (必填)",
  "password": "string (必填)"
}
```

**成功响应 (200)：**
```json
{
  "code": "SUCCESS",
  "message": "登录成功",
  "data": {
    "token": "string (JWT令牌)",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "username": "doctor001",
      "realName": "张医生",
      "role": "DOCTOR",
      "position": "OUTPATIENT"
    }
  }
}
```

#### 3.1.2 获取当前用户 `/api/auth/me`

**请求头：** `Authorization: Bearer <token>`

**成功响应 (200)：**
```json
{
  "code": "SUCCESS",
  "message": "获取成功",
  "data": {
    "id": 1,
    "username": "doctor001",
    "realName": "张医生",
    "role": "DOCTOR",
    "position": "OUTPATIENT",
    "permissions": ["prescription:create", "medical-record:view"]
  }
}
```

### 3.2 菜单接口

| 接口路径 | HTTP方法 | 所属Controller | 功能描述 |
|----------|----------|----------------|----------|
| `/api/menu/tree` | GET | MenuController | 获取当前用户菜单树 |
| `/api/menu/all` | GET | MenuController | 获取所有菜单（管理员） |
| `/api/menu` | POST | MenuController | 创建菜单 |
| `/api/menu/{id}` | PUT | MenuController | 更新菜单 |
| `/api/menu/{id}` | DELETE | MenuController | 删除菜单 |

#### 3.2.1 获取用户菜单树 `/api/menu/tree`

**请求头：** `Authorization: Bearer <token>`

**成功响应 (200)：**
```json
{
  "code": "SUCCESS",
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "name": "挂号管理",
      "path": "/registration",
      "icon": "icon-hospital",
      "children": [
        {
          "id": 11,
          "name": "挂号列表",
          "path": "/registration/list",
          "icon": "icon-list",
          "children": null
        }
      ]
    }
  ]
}
```

---

## 4. 前端实现方案

### 4.1 目录结构

```
frontend/
├── apps/
│   ├── doctor/
│   │   ├── src/
│   │   │   ├── views/           # 页面视图
│   │   │   │   ├── Login.vue    # 登录页
│   │   │   │   └── Dashboard/   # 仪表盘
│   │   │   ├── components/      # 公共组件
│   │   │   │   ├── Layout.vue   # 布局组件
│   │   │   │   ├── Sidebar.vue  # 侧边栏菜单
│   │   │   │   └── Header.vue   # 顶部导航
│   │   │   ├── stores/          # Pinia状态管理
│   │   │   │   ├── auth.ts      # 认证状态
│   │   │   │   └── menu.ts      # 菜单状态
│   │   │   ├── router/          # 路由配置
│   │   │   │   └── index.ts
│   │   │   ├── api/             # API调用
│   │   │   │   └── auth.ts
│   │   │   ├── types/           # 类型定义
│   │   │   │   └── index.ts
│   │   │   ├── App.vue
│   │   │   └── main.ts
│   └── admin/                   # 管理员端结构同医生端
└── packages/
    └── shared/
        └── src/
            ├── api/             # 共享API
            └── types/           # 共享类型
```

### 4.2 状态管理设计

#### 4.2.1 Auth Store (`stores/auth.ts`)

| 状态字段 | 类型 | 说明 |
|----------|------|------|
| token | string | JWT令牌 |
| user | UserInfo | 当前用户信息 |
| isAuthenticated | boolean | 是否已认证 |

| Action | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| login | { username, password } | Promise\<void\> | 用户登录 |
| logout | — | Promise\<void\> | 用户登出 |
| getCurrentUser | — | Promise\<UserInfo\> | 获取当前用户 |
| refreshToken | — | Promise\<void\> | 刷新Token |

#### 4.2.2 Menu Store (`stores/menu.ts`)

| 状态字段 | 类型 | 说明 |
|----------|------|------|
| menus | MenuItem[] | 用户菜单列表 |
| activeMenu | string | 当前激活菜单 |

| Action | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| fetchMenus | — | Promise\<void\> | 获取菜单列表 |
| setActiveMenu | path: string | void | 设置激活菜单 |

### 4.3 路由配置

#### 4.3.1 医生端路由 (`router/index.ts`)

```typescript
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../components/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '/dashboard', name: 'Dashboard', component: () => import('../views/Dashboard/index.vue') },
      { path: '/registration', name: 'Registration', component: () => import('../views/Registration/index.vue') },
      // ... 其他路由
    ]
  }
]
```

#### 4.3.2 路由守卫

```typescript
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      return next('/login')
    }
    // 动态路由加载
    if (!authStore.hasFetchedRoutes) {
      await authStore.fetchRoutes()
      return next({ ...to, replace: true })
    }
  } else if (to.path === '/login' && authStore.isAuthenticated) {
    return next('/')
  }
  next()
})
```

### 4.4 菜单动态渲染

#### 4.4.1 Sidebar组件 (`components/Sidebar.vue`)

```typescript
<script setup lang="ts">
import { computed } from 'vue'
import { useMenuStore } from '@/stores/menu'

const menuStore = useMenuStore()

const menuTree = computed(() => menuStore.menus)

function renderMenu(menus: MenuItem[]) {
  // 递归渲染菜单树
}
</script>
```

---

## 5. 后端实现方案

### 5.1 目录结构

```
backend/
└── modules/
    └── common-module/
        └── src/
            └── main/
                └── java/
                    └── com/aimedical/
                        └── modules/
                            └── commonmodule/
                                ├── controller/
                                │   ├── AuthController.java
                                │   └── MenuController.java
                                ├── service/
                                │   ├── AuthService.java
                                │   ├── MenuService.java
                                │   └── impl/
                                │       ├── AuthServiceImpl.java
                                │       └── MenuServiceImpl.java
                                ├── repository/
                                │   ├── UserRepository.java
                                │   ├── RoleRepository.java
                                │   └── MenuRepository.java
                                ├── entity/
                                │   ├── UserEntity.java
                                │   ├── RoleEntity.java
                                │   └── MenuEntity.java
                                ├── dto/
                                │   ├── request/
                                │   │   ├── LoginRequest.java
                                │   │   └── RefreshTokenRequest.java
                                │   └── response/
                                │       ├── LoginResponse.java
                                │       ├── UserInfoResponse.java
                                │       └── MenuResponse.java
                                └── config/
                                    ├── JwtConfig.java
                                    └── SecurityConfig.java
```

### 5.2 Service层设计

#### 5.2.1 AuthService 接口

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| login | LoginRequest request | LoginResponse | 用户登录 |
| logout | String token | void | 用户登出 |
| refreshToken | String refreshToken | LoginResponse | 刷新Token |
| getCurrentUser | String token | UserInfoResponse | 获取当前用户 |

#### 5.2.2 MenuService 接口

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| getUserMenuTree | Long userId | List\<MenuResponse\> | 获取用户菜单树 |
| getAllMenus | — | List\<MenuResponse\> | 获取所有菜单 |
| createMenu | MenuCreateRequest request | MenuResponse | 创建菜单 |
| updateMenu | Long id, MenuUpdateRequest request | MenuResponse | 更新菜单 |
| deleteMenu | Long id | void | 删除菜单 |

### 5.3 安全配置

#### 5.3.1 JWT工具类

```java
public class JwtUtil {
    // 生成Token
    public static String generateToken(String username, String role, String position)
    
    // 解析Token
    public static Claims parseToken(String token)
    
    // 验证Token
    public static boolean validateToken(String token)
}
```

#### 5.3.2 安全过滤器

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // 验证JWT令牌
    // 设置SecurityContext
}
```

---

## 6. 数据库设计

### 6.1 用户表 (`sys_user`)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(255) | NOT NULL | 密码(BCrypt加密) |
| real_name | VARCHAR(50) | NOT NULL | 真实姓名 |
| role | VARCHAR(20) | NOT NULL | 角色类型 |
| position | VARCHAR(20) | — | 岗位类型 |
| status | TINYINT | DEFAULT 1 | 状态(1启用/0禁用) |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 6.2 角色表 (`sys_role`)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 角色ID |
| name | VARCHAR(50) | UNIQUE, NOT NULL | 角色名称 |
| code | VARCHAR(50) | UNIQUE, NOT NULL | 角色编码 |
| description | VARCHAR(255) | — | 角色描述 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

### 6.3 菜单表 (`sys_menu`)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 菜单ID |
| parent_id | BIGINT | FOREIGN KEY | 父菜单ID |
| name | VARCHAR(50) | NOT NULL | 菜单名称 |
| path | VARCHAR(100) | — | 路由路径 |
| icon | VARCHAR(50) | — | 图标名称 |
| permission | VARCHAR(100) | — | 权限标识 |
| sort_order | INT | DEFAULT 0 | 排序号 |
| visible | TINYINT | DEFAULT 1 | 是否可见 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

### 6.4 用户角色关联表 (`sys_user_role`)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| user_id | BIGINT | FOREIGN KEY | 用户ID |
| role_id | BIGINT | FOREIGN KEY | 角色ID |

### 6.5 角色菜单关联表 (`sys_role_menu`)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| role_id | BIGINT | FOREIGN KEY | 角色ID |
| menu_id | BIGINT | FOREIGN KEY | 菜单ID |

---

## 7. 开发计划

### 7.1 里程碑

| 阶段 | 时间 | 任务 | 负责人 |
|------|------|------|--------|
| 需求分析 | Day 1 | 完成需求分析与设计文档 | 架构师 |
| 数据库设计 | Day 2 | 完成数据库表设计与初始化脚本 | 后端开发 |
| 后端API开发 | Day 3-5 | 完成认证、菜单、用户管理API | 后端开发 |
| 前端页面开发 | Day 6-8 | 完成登录页、布局、菜单组件 | 前端开发 |
| 联调测试 | Day 9-10 | 完成前后端联调与功能测试 | 全组 |

### 7.2 任务分解

#### 后端任务

| 任务 | 描述 | 估算工时 |
|------|------|----------|
| BE-01 | 创建用户实体与Repository | 4h |
| BE-02 | 创建角色实体与Repository | 4h |
| BE-03 | 创建菜单实体与Repository | 4h |
| BE-04 | 实现JwtUtil工具类 | 4h |
| BE-05 | 实现SecurityConfig配置 | 4h |
| BE-06 | 实现AuthService | 8h |
| BE-07 | 实现MenuService | 8h |
| BE-08 | 实现AuthController | 4h |
| BE-09 | 实现MenuController | 4h |
| BE-10 | 编写单元测试 | 8h |

#### 前端任务

| 任务 | 描述 | 估算工时 |
|------|------|----------|
| FE-01 | 创建登录页面 | 4h |
| FE-02 | 创建Layout布局组件 | 4h |
| FE-03 | 创建Sidebar菜单组件 | 6h |
| FE-04 | 创建Header头部组件 | 4h |
| FE-05 | 配置Pinia状态管理 | 4h |
| FE-06 | 配置Vue Router | 4h |
| FE-07 | 实现路由守卫 | 4h |
| FE-08 | 封装API请求 | 4h |
| FE-09 | 编写集成测试 | 4h |

---

## 8. 接口清单汇总

| API路径 | HTTP方法 | Controller | 功能 |
|---------|----------|------------|------|
| `/api/auth/login` | POST | AuthController | 用户登录 |
| `/api/auth/logout` | POST | AuthController | 用户登出 |
| `/api/auth/refresh` | POST | AuthController | 刷新Token |
| `/api/auth/me` | GET | AuthController | 获取当前用户 |
| `/api/menu/tree` | GET | MenuController | 获取用户菜单树 |
| `/api/menu/all` | GET | MenuController | 获取所有菜单 |
| `/api/menu` | POST | MenuController | 创建菜单 |
| `/api/menu/{id}` | PUT | MenuController | 更新菜单 |
| `/api/menu/{id}` | DELETE | MenuController | 删除菜单 |

---

## 9. 权限矩阵

### 9.1 医生岗位菜单权限

| 菜单 | 门诊医生 | 检查医生 | 检验医生 | 药房医生 | 线下接诊医生 |
|------|----------|----------|----------|----------|--------------|
| 挂号管理 | ✓ | ✓ | ✓ | ✓ | ✗ |
| 门诊接诊 | ✓ | ✗ | ✗ | ✗ | ✗ |
| 检查管理 | ✗ | ✓ | ✗ | ✗ | ✗ |
| 检验管理 | ✗ | ✗ | ✓ | ✗ | ✗ |
| 药房管理 | ✗ | ✗ | ✗ | ✓ | ✗ |
| 线下窗口 | ✗ | ✗ | ✗ | ✗ | ✓ |
| 个人中心 | ✓ | ✓ | ✓ | ✓ | ✓ |

### 9.2 管理员菜单权限

| 菜单 | 管理员 |
|------|--------|
| 用户管理 | ✓ |
| 角色管理 | ✓ |
| 菜单管理 | ✓ |
| 字典管理 | ✓ |
| 排班管理 | ✓ |
| 分诊台配置 | ✓ |

---

## 10. 安全性考虑

1. **密码加密**：使用BCrypt加密存储密码
2. **JWT令牌**：使用HS256算法签名，设置合理过期时间
3. **Token刷新**：实现refresh token机制
4. **防止CSRF**：使用JWT无状态认证，天然防御CSRF
5. **权限校验**：每个接口进行权限校验
6. **日志审计**：记录关键操作日志

---

## 附录：前后端联调约定

### A.1 接口调用规范

- 前端统一使用Axios封装
- 请求头携带JWT令牌：`Authorization: Bearer <token>`
- 统一响应格式：
  ```json
  {
    "code": "SUCCESS|FAIL|UNAUTHORIZED|FORBIDDEN",
    "message": "提示信息",
    "data": {}
  }
  ```

### A.2 错误码定义

| 错误码 | 含义 | HTTP状态码 |
|--------|------|-----------|
| SUCCESS | 成功 | 200 |
| FAIL | 业务失败 | 200 |
| UNAUTHORIZED | 未认证 | 401 |
| FORBIDDEN | 无权限 | 403 |
| VALIDATION_ERROR | 参数校验失败 | 400 |
| SYSTEM_ERROR | 系统错误 | 500 |

### A.3 开发环境配置

- 前端开发端口：医生端 `http://localhost:5173`，管理端 `http://localhost:5174`
- 后端服务端口：`http://localhost:8080`
- 数据库：MySQL 8.0+，数据库名：`aimedical`