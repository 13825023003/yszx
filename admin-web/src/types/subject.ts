/** 学科与阶段管理（A-S01 ~ A-S11）。 */

export interface SubjectStats {
  totalSubjects: number
  subjectsWithStages: number
  totalStages: number
  totalQuestions: number
}

export interface StageItem {
  id: number
  name: string
  icon: string | null
  sortOrder: number
  status: number
  questionCount: number
}

export interface SubjectItem {
  id: number
  name: string
  icon: string
  description: string
  themeColor: string
  sortOrder: number
  courseCount: number
  routeCount: number
  questionCount: number
  interviewQuestionCount: number
  stageCount: number
  enabledStageCount: number
  stages: StageItem[]
}

export interface SubjectPayload {
  name: string
  icon: string
  description: string
  sortOrder: number
  themeColor: string
}

export interface StagePayload {
  name: string
  icon?: string | null
  sortOrder?: number | null
}
