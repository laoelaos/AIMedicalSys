<template>
  <LayoutBase sidebar-bg="#1e3c72">
    <template #sidebar>
      <Sidebar />
    </template>
    <template #header>
      <Header />
    </template>
    <router-view />
  </LayoutBase>
</template>

<script setup lang="ts">
/**
 * 管理员端布局
 *
 * <p>薄包装组件：复用 shared 包的 LayoutBase（T5 抽取重复组件），
 * 仅注入管理员端的 Sidebar、Header 组件和 sidebar 背景色。
 */
import { LayoutBase } from '@aimedical/shared/components'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
</script>
