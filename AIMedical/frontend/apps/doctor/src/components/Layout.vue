<template>
  <LayoutBase sidebar-bg="#304156">
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
 * 医生端布局
 *
 * <p>薄包装组件：复用 shared 包的 LayoutBase（T5 抽取重复组件），
 * 仅注入医生端的 Sidebar、Header 组件和 sidebar 背景色。
 */
import { LayoutBase } from '@aimedical/shared/components'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
</script>
