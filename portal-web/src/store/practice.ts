import { ref } from 'vue'
import { defineStore } from 'pinia'

export interface PracticeSession {
  practiceId: string
  questionIds: number[]
  currentIndex: number
}

/**
 * 练习会话（前端本地保存，支持断点续练）。
 * 判分与统计以后端为准，前端只维护进度。
 */
export const usePracticeStore = defineStore('practice', () => {
  const session = ref<PracticeSession | null>(null)

  function start(value: PracticeSession) {
    session.value = value
  }

  function updateIndex(index: number) {
    if (session.value) {
      session.value.currentIndex = index
    }
  }

  function clear() {
    session.value = null
  }

  return { session, start, updateIndex, clear }
})
