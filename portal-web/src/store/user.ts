import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi, type LoginPasswordPayload } from '@/api/auth'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/utils/token'

export interface PortalProfile {
  id: number
  nickname: string
  mobile?: string
  avatar?: string | null
}

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string | null>(getAccessToken())
  const profile = ref<PortalProfile | null>(null)
  const isLoggedIn = computed(() => Boolean(accessToken.value))

  async function loginByPassword(payload: LoginPasswordPayload) {
    const tokens = await authApi.loginByPassword(payload)
    setTokens(tokens)
    accessToken.value = tokens.accessToken
    return tokens
  }

  async function logout() {
    try {
      if (getRefreshToken()) {
        await authApi.logout()
      }
    } catch {
      // 登出接口失败不影响本地清理
    } finally {
      clearTokens()
      accessToken.value = null
      profile.value = null
    }
  }

  function setProfile(value: PortalProfile | null) {
    profile.value = value
  }

  return { accessToken, profile, isLoggedIn, loginByPassword, logout, setProfile }
})
