import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 管理端权限码状态（04.API接口设计 附录 A，9 组 34 项）。
 *
 * 登录后由 `GET /auth/me`（A-A04）写入；超级管理员使用通配符 "*"。
 * 初始化前不拦截菜单与按钮，避免权限加载过程中误隐藏界面。
 */
export const usePermissionStore = defineStore('permission', () => {
  const permissions = ref<string[]>([])
  const initialized = ref(false)

  const isSuperAdmin = computed(() => permissions.value.includes('*'))

  function setPermissions(codes: string[]) {
    permissions.value = Array.from(new Set(codes))
    initialized.value = true
  }

  function hasPermission(code?: string): boolean {
    if (!code) {
      return true
    }
    return isSuperAdmin.value || permissions.value.includes(code)
  }

  function reset() {
    permissions.value = []
    initialized.value = false
  }

  return { permissions, initialized, isSuperAdmin, setPermissions, hasPermission, reset }
})
