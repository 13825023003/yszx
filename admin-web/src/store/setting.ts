import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { settingApi } from '@/api/system'
import type { SystemSettings } from '@/types/system'

/**
 * 平台基本信息：登录后加载一次，供后台品牌、浏览器标题与默认头像复用。
 */
export const useSettingStore = defineStore('setting', () => {
  const settings = ref<SystemSettings | null>(null)
  const loaded = ref(false)
  let pending: Promise<SystemSettings | null> | null = null

  const siteName = computed(() => settings.value?.siteName || '云枢智学')
  const logoUrl = computed(() => settings.value?.logoUrl || '')
  const defaultAvatar = computed(() => settings.value?.defaultAvatar || '')

  async function load(force = false): Promise<SystemSettings | null> {
    if (loaded.value && !force) {
      return settings.value
    }
    if (pending) {
      return pending
    }
    pending = settingApi
      .get()
      .then((data) => {
        settings.value = data
        loaded.value = true
        return data
      })
      .catch(() => null)
      .finally(() => {
        pending = null
      })
    return pending
  }

  function apply(next: SystemSettings) {
    settings.value = next
    loaded.value = true
  }

  function reset() {
    settings.value = null
    loaded.value = false
  }

  return { settings, loaded, siteName, logoUrl, defaultAvatar, load, apply, reset }
})
