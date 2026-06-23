# 任务（v13）

## 概述

创建前端 monorepo 完整骨架（`AIMedical/frontend/`），填补 Phase 0 最后一个 P0 缺口。根据 OOD §2.4（前端模块划分）和 §9.3（一键启动）完成 npm workspaces + Vite 多应用单仓的目录结构、共享包和三端占位首页。

## 文件规划

### 根级

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `frontend/package.json` | 新建 | npm workspaces 根配置（workspaces: packages/*, apps/*; scripts.build:all） |
| `frontend/tsconfig.base.json` | 新建 | TypeScript 共享编译配置（target es2020, module ESNext, strict true） |
| `frontend/.gitignore` | 新建 | 前端专有 gitignore（node_modules, dist, .env.local） |

### packages/shared

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `frontend/packages/shared/package.json` | 新建 | `@aimedical/shared` 内部包配置（private, main & types → src/index.ts） |
| `frontend/packages/shared/src/index.ts` | 新建 | 统一导出入口 |
| `frontend/packages/shared/src/api/index.ts` | 新建 | ApiClient 占位（Axios 实例骨架，baseURL 拦截器预留） |
| `frontend/packages/shared/src/types/index.ts` | 新建 | TypeScript 类型定义入口（与后端 DTO 对应接口占位） |
| `frontend/packages/shared/src/utils/index.ts` | 新建 | 通用工具函数入口 |

### packages/ui-core

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `frontend/packages/ui-core/package.json` | 新建 | `@aimedical/ui-core` 内部包配置，声明 `@aimedical/shared` 依赖 |
| `frontend/packages/ui-core/src/index.ts` | 新建 | 统一导出入口 |
| `frontend/packages/ui-core/src/components/index.ts` | 新建 | 共享 UI 组件导出占位 |

### apps/patient（端口 5173）

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `frontend/apps/patient/package.json` | 新建 | `@aimedical/app-patient`，dependencies: shared + ui-core + vue + vue-router；devDependencies: vite（由 root 统一提供 @vitejs/plugin-vue） |
| `frontend/apps/patient/tsconfig.json` | 新建 | 继承 ../../tsconfig.base.json，包含引用配置 |
| `frontend/apps/patient/vite.config.ts` | 新建 | port 5173，proxy /api → localhost:8080 |
| `frontend/apps/patient/index.html` | 新建 | HTML 入口，挂载点 #app，标题"智慧云脑诊疗平台 - 患者端" |
| `frontend/apps/patient/env.d.ts` | 新建 | Vite env 类型声明 |
| `frontend/apps/patient/src/main.ts` | 新建 | createApp(App).mount('#app') |
| `frontend/apps/patient/src/App.vue` | 新建 | 占位首页：h1 "智慧云脑诊疗平台 - 患者端" + 占位提示 |

### apps/doctor（端口 5174）

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `frontend/apps/doctor/package.json` | 新建 | `@aimedical/app-doctor`，dependencies: shared + ui-core + vue + vue-router；devDependencies: vite |
| `frontend/apps/doctor/tsconfig.json` | 新建 | 继承 ../../tsconfig.base.json，包含引用配置 |
| `frontend/apps/doctor/vite.config.ts` | 新建 | port 5174，proxy /api → localhost:8080 |
| `frontend/apps/doctor/index.html` | 新建 | HTML 入口，挂载点 #app，标题"智慧云脑诊疗平台 - 医生端" |
| `frontend/apps/doctor/env.d.ts` | 新建 | Vite env 类型声明 |
| `frontend/apps/doctor/src/main.ts` | 新建 | createApp(App).mount('#app') |
| `frontend/apps/doctor/src/App.vue` | 新建 | 占位首页：h1 "智慧云脑诊疗平台 - 医生端" + 占位提示 |

### apps/admin（端口 5175）

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `frontend/apps/admin/package.json` | 新建 | `@aimedical/app-admin`，dependencies: shared + ui-core + vue + vue-router；devDependencies: vite |
| `frontend/apps/admin/tsconfig.json` | 新建 | 继承 ../../tsconfig.base.json，包含引用配置 |
| `frontend/apps/admin/vite.config.ts` | 新建 | port 5175，proxy /api → localhost:8080 |
| `frontend/apps/admin/index.html` | 新建 | HTML 入口，挂载点 #app，标题"智慧云脑诊疗平台 - 管理员端" |
| `frontend/apps/admin/env.d.ts` | 新建 | Vite env 类型声明 |
| `frontend/apps/admin/src/main.ts` | 新建 | createApp(App).mount('#app') |
| `frontend/apps/admin/src/App.vue` | 新建 | 占位首页：h1 "智慧云脑诊疗平台 - 管理员端" + 占位提示 |

## 依赖关系

```
@aimedical/shared      — 纯 TS，无内部依赖
@aimedical/ui-core     — 依赖 @aimedical/shared
@aimedical/app-*       — 依赖 @aimedical/shared + @aimedical/ui-core + vue + vue-router
```

## 设计细节

### Root package.json

```json
{
  "name": "aimedical-frontend",
  "private": true,
  "scripts": {
    "dev:patient": "npm run dev -w @aimedical/app-patient",
    "dev:doctor": "npm run dev -w @aimedical/app-doctor",
    "dev:admin": "npm run dev -w @aimedical/app-admin",
    "dev": "concurrently \"npm run dev:patient\" \"npm run dev:doctor\" \"npm run dev:admin\"",
    "build:all": "npm run build --workspaces --if-present"
  },
  "devDependencies": {
    "concurrently": "^8.2.2",
    "@vitejs/plugin-vue": "^5.0.0"
  },
  "workspaces": [
    "packages/*",
    "apps/*"
  ]
}
```

### tsconfig.base.json

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "jsx": "preserve",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "esModuleInterop": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "noEmit": true
  }
}
```

### Vite 代理配置（各端通用，仅 port 不同）

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173, // 三端分别为 5173/5174/5175
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

### 占位首页（各端共 3 个入口文件）

**index.html**:
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>智慧云脑诊疗平台 - {患者端/医生端/管理员端}</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.ts"></script>
</body>
</html>
```

**src/main.ts**:
```typescript
import { createApp } from 'vue'
import App from './App.vue'

createApp(App).mount('#app')
```

**src/App.vue**:
```vue
<template>
  <div class="app-container">
    <h1>智慧云脑诊疗平台 - {{ appName }}</h1>
    <p class="placeholder-hint">Phase 0 占位页面 — 后端开发中</p>
  </div>
</template>

<script setup lang="ts">
const appName = '{患者端/医生端/管理员端}'
</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  font-family: sans-serif;
}
.placeholder-hint {
  color: #666;
  font-size: 14px;
}
</style>
```

## 验收标准

1. `AIMedical/frontend/` 目录下执行 `npm install`（需 Node.js 18+）不报错
2. `AIMedical/frontend/` 目录下执行 `npm run dev` 可同时启动三端 Vite dev server
3. 浏览器访问 `http://localhost:5173` 显示患者端占位首页（h1 含"患者端"）
4. 浏览器访问 `http://localhost:5174` 显示医生端占位首页
5. 浏览器访问 `http://localhost:5175` 显示管理员端占位首页
6. 各端代理 `/api/ping → localhost:8080/ping` 配置正确（需后端同时启动验证）
