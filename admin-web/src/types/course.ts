import type { SubjectBrief } from './subject'

/** B站课程库（A-C01 ~ A-C07）。 */

export interface CourseStats {
  totalCourses: number
  totalDurationMinutes: number
  associatedSubjectCount: number
}

export interface CourseItem {
  id: number
  name: string
  coverUrl: string | null
  durationMinutes: number
  source: number
  link: string | null
  bvid: string | null
  primarySubject: SubjectBrief | null
  subjects: SubjectBrief[]
}

export interface CoursePayload {
  name: string
  coverUrl?: string | null
  durationMinutes: number
  subjectId?: number | null
  subjectIds?: number[]
  source: number
  link?: string | null
  bvid?: string | null
}

export interface CourseImportRow {
  rowNumber: number
  name: string
  subjectText: string
  sourceText: string
  durationMinutes: number | null
  bvid: string
  coverUrl: string
  link: string
  status: 'valid' | 'warning' | 'invalid'
  messages: string[]
}

export interface CourseImportPreview {
  token: string | null
  filename: string
  totalRows: number
  validCount: number
  warningCount: number
  invalidCount: number
  rows: CourseImportRow[]
}
