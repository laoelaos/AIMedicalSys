import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { menuApi } from '@aimedical/shared/api'
import type { MenuItem, BusinessError } from '@aimedical/shared/types'

/**
 * 菜单状态管理Store
 *
 * 管理管理员菜单列表和当前激活菜单。
 */
export const useMenuStore = defineStore('menu', () => {
  const menus = ref<MenuItem[]>([])
  const activeMenu = ref<string>('')

  const hasMenus = computed(() => menus.value.length > 0)

  /**
   * 获取用户菜单列表
   */
  async function fetchMenus(): Promise<boolean> {
    const response = await menuApi.tree()
    if (isBusinessError(response)) {
      return false
    }
    menus.value = response
    return true
  }

  /**
   * 设置激活菜单
   */
  function setActiveMenu(path: string): void {
    activeMenu.value = path
  }

  /**
   * 清空菜单
   */
  function clearMenus(): void {
    menus.value = []
    activeMenu.value = ''
  }

  /**
   * 检查是否为业务错误
   */
  function isBusinessError(response: unknown): response is BusinessError {
    return (response as BusinessError).isBusinessError === true
  }

  return {
    menus,
    activeMenu,
    hasMenus,
    fetchMenus,
    setActiveMenu,
    clearMenus,
  }
})