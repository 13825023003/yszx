import { http } from './request'
import type { RouteDetail, RouteItem, RoutePayload } from '@/types/route'

export const routeApi = {
  list: () => http.get<RouteItem[]>('/routes'),
  create: (payload: RoutePayload) => http.post<{ id: number }>('/routes', payload),
  update: (id: number, payload: RoutePayload) => http.put<void>(`/routes/${id}`, payload),
  updateStatus: (id: number, status: number) =>
    http.post<void>(`/routes/${id}/status`, { status }),
  remove: (id: number) => http.delete<void>(`/routes/${id}`),
  detail: (id: number) => http.get<RouteDetail>(`/routes/${id}/detail`),
  addCourses: (id: number, courseIds: number[]) =>
    http.post<void>(`/routes/${id}/courses`, { courseIds }),
  removeCourse: (id: number, courseId: number) =>
    http.delete<void>(`/routes/${id}/courses/${courseId}`),
  sortCourses: (id: number, courseIds: number[]) =>
    http.put<void>(`/routes/${id}/courses/sort`, { courseIds }),
}
