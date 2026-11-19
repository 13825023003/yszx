import type { SubjectBrief } from './subject'

/** 学习路线（A-R01 ~ A-R09）。 */

export interface RouteCourseItem {
  id: number
  name: string
  coverUrl: string | null
  durationMinutes: number
  source: number
  link: string | null
  bvid: string | null
  primarySubject: SubjectBrief | null
  subjects: SubjectBrief[]
  sortOrder: number
  localSubject: boolean
}

export interface RouteItem {
  id: number
  name: string
  subjectId: number
  subjectName: string
  status: number
  icon: string | null
  targetEducation: string | null
  description: string | null
  courseCount: number
  totalDurationMinutes: number
  previewCourses: RouteCourseItem[]
}

export interface RouteDetail extends Omit<RouteItem, 'previewCourses'> {
  courses: RouteCourseItem[]
}

export interface RoutePayload {
  name: string
  subjectId: number
  targetEducation?: string | null
  icon?: string | null
  description?: string | null
  status?: number
}
