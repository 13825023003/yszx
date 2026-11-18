import type { App, Directive } from 'vue'
import { usePermissionStore } from '@/store/permission'

/**
 * v-permission="'subject:add'"：无权限时移除元素。
 * 权限未初始化（initialized=false）时不拦截，避免权限加载前误隐藏界面；
 * 真正的访问控制仍由后端 RBAC 切面兜底。
 */
const permission: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    const store = usePermissionStore()
    if (store.initialized && !store.hasPermission(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
}

export function setupPermissionDirective(app: App): void {
  app.directive('permission', permission)
}
