import { http } from './request'
import type {
  StagePayload,
  SubjectItem,
  SubjectPayload,
  SubjectStats,
} from '@/types/subject'

export const subjectApi = {
  stats: () => http.get<SubjectStats>('/subjects/stats'),
  list: () => http.get<SubjectItem[]>('/subjects'),
  create: (payload: SubjectPayload) => http.post<void>('/subjects', payload),
  update: (id: number, payload: SubjectPayload) =>
    http.put<void>(`/subjects/${id}`, payload),
  sort: (orderedIds: number[]) => http.put<void>('/subjects/sort', { orderedIds }),
  remove: (id: number) => http.delete<void>(`/subjects/${id}`),
  createStage: (subjectId: number, payload: StagePayload) =>
    http.post<void>(`/subjects/${subjectId}/stages`, payload),
  updateStage: (subjectId: number, stageId: number, payload: StagePayload) =>
    http.put<void>(`/subjects/${subjectId}/stages/${stageId}`, payload),
  sortStages: (subjectId: number, orderedIds: number[]) =>
    http.put<void>(`/subjects/${subjectId}/stages/sort`, { orderedIds }),
  updateStageStatus: (subjectId: number, stageId: number, status: number) =>
    http.put<void>(`/subjects/${subjectId}/stages/${stageId}/status`, { status }),
  removeStage: (subjectId: number, stageId: number) =>
    http.delete<void>(`/subjects/${subjectId}/stages/${stageId}`),
}
