<template>
  <HeaderBase
    title="智慧云脑诊疗平台 - 管理员端"
    fallback-name="管理员"
    :user="authStore.user"
    @logout="handleLogout"
  />
</template>

<script setup lang="ts">
/**
 * 管理员端顶部栏
 *
 * <p>薄包装组件：复用 shared 包的 HeaderBase（T4 抽取重复组件），
 * 仅连接管理员端的 authStore 和 menuStore，处理登出逻辑。
 */
import { HeaderBase } from '@aimedical/shared/components'
import { useAuthStore } from '../stores/auth'
import { useMenuStore } from '../stores/menu'

const authStore = useAuthStore()
const menuStore = useMenuStore()

async function handleLogout() {
  await authStore.logout()
  menuStore.clearMenus()
}
</script>
