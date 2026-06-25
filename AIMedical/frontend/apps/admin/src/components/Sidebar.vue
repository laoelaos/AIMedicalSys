<template>
  <SidebarBase
    title="管理员端"
    :menus="menuStore.menus"
    :active-menu="menuStore.activeMenu"
    @select="handleSelect"
  />
</template>

<script setup lang="ts">
/**
 * 管理员端侧边栏
 *
 * <p>薄包装组件：复用 shared 包的 SidebarBase（T10 抽取重复组件），
 * 仅负责连接管理员端的 menuStore，将菜单数据和激活状态传入基础组件。
 */
import { SidebarBase } from '@aimedical/shared/components'
import { useMenuStore } from '../stores/menu'

const menuStore = useMenuStore()

function handleSelect(path: string): void {
  menuStore.setActiveMenu(path)
}
</script>
