<template>
  <HeaderBase
    title="智慧云脑诊疗平台 - 医生端"
    fallback-name="医生"
    :user="authStore.user"
    @logout="handleLogout"
  />
</template>

<script setup lang="ts">
/**
 * 医生端顶部栏
 *
 * <p>薄包装组件：复用 shared 包的 HeaderBase（T4 抽取重复组件），
 * 仅连接医生端的 authStore 和 menuStore，处理登出逻辑。
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
