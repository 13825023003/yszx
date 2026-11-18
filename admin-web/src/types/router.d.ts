import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    guestOnly?: boolean
    /** 访问该路由所需权限码（04.API接口设计 附录 A） */
    permission?: string
    /** 仅超级管理员可访问（如角色管理） */
    superAdminOnly?: boolean
  }
}

export {}
