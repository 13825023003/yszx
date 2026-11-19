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
