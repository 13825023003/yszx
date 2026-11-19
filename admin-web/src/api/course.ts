import { http } from './request'
import type { PageQuery, PageResult } from '@/types/api'
import type {
  CourseImportPreview,
  CourseItem,
  CoursePayload,
  CourseStats,
} from '@/types/course'

export interface CourseListQuery extends PageQuery {
  keyword?: string
  subjectId?: number
  source?: number
}

export const courseApi = {
  stats: () => http.get<CourseStats>('/courses/stats'),
  list: (params: CourseListQuery) =>
    http.get<PageResult<CourseItem>>('/courses', { params }),
  /** 供路线课程选择等场景使用的全量课程列表（按 100 条分页拉取）。 */
  listAll: async (): Promise<CourseItem[]> => {
    const result: CourseItem[] = []
    let page = 1
    let total = Number.POSITIVE_INFINITY
    while (result.length < total) {
      const data = await courseApi.list({ page, pageSize: 100 })
      result.push(...data.list)
      total = data.total
      if (data.list.length === 0) {
        break
      }
      page += 1
    }
    return result
  },
  create: (payload: CoursePayload) => http.post<void>('/courses', payload),
  update: (id: number, payload: CoursePayload) =>
    http.put<void>(`/courses/${id}`, payload),
  remove: (id: number) => http.delete<void>(`/courses/${id}`),
  previewImport: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<CourseImportPreview>('/courses/import/preview', formData)
  },
  confirmImport: (token: string) =>
    http.post<{ importedCount: number }>('/courses/import/confirm', { token }),
}
