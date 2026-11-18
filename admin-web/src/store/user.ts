import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi, type AdminLoginPayload } from '@/api/auth'
import type { AdminProfile } from '@/types/system'
import { usePermissionStore } from '@/store/permission'
import { useSettingStore } from '@/store/setting'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/utils/token'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string | null>(getAccessToken())
  const account = ref<string>(localStorage.getItem('yszh.admin.account') ?? '')
  const profile = ref<AdminProfile | null>(null)
  const isLoggedIn = computed(() => Boolean(accessToken.value))
  const roleName = computed(() => profile.value?.roleName ?? '后台管理员')
  const avatar = computed(() => profile.value?.avatar ?? '')

  async function login(payload: AdminLoginPayload) {
    const tokens = await authApi.login(payload)
    setTokens(tokens)
    accessToken.value = tokens.accessToken
    try {
      await loadProfile()
      await useSettingStore().load(true)
    } catch (error) {
      clearLocal()
      throw error
    }
    return tokens
  }

  async function loadProfile(): Promise<AdminProfile> {
    const data = await authApi.me()
    profile.value = data
    account.value = data.username
    localStorage.setItem('yszh.admin.account', data.username)
    usePermissionStore().setPermissions(data.permissions)
    return data
  }

  /** 刷新页面后恢复当前管理员与权限；失败时清空失效登录态。 */
  async function ensureProfile(): Promise<AdminProfile | null> {
    if (!accessToken.value) {
      return null
    }
    if (profile.value) {
      return profile.value
    }
    try {
      return await loadProfile()
    } catch {
      clearLocal()
      return null
    }
  }

  async function logout() {
    try {
      if (getRefreshToken()) {
        await authApi.logout()
      }
    } catch {
      // 登出接口失败不影响本地清理
    } finally {
      clearLocal()
    }
  }

  function clearLocal() {
    clearTokens()
    accessToken.value = null
    account.value = ''
    profile.value = null
    localStorage.removeItem('yszh.admin.account')
    usePermissionStore().reset()
    useSettingStore().reset()
  }

  return {
    accessToken,
    account,
    profile,
    roleName,
    avatar,
    isLoggedIn,
    login,
    loadProfile,
    ensureProfile,
    logout,
  }
})
