import { ref } from 'vue'
import { defineStore } from 'pinia'

export interface InterviewMessage {
  role: 'ai' | 'student'
  content: string
}

export interface InterviewSessionState {
  sessionId: string | null
  subjectId: number | null
  difficulty: number | null
  questionCount: number
  currentOrder: number
  messages: InterviewMessage[]
}

/**
 * 面试会话（D16：未完成会话仅存前端本地，不支持跨设备续做）。
 */
export const useInterviewStore = defineStore('interview', () => {
  const state = ref<InterviewSessionState>({
    sessionId: null,
    subjectId: null,
    difficulty: null,
    questionCount: 10,
    currentOrder: 0,
    messages: [],
  })

  function start(value: Partial<InterviewSessionState>) {
    state.value = { ...state.value, ...value, messages: [] }
  }

  function appendMessage(message: InterviewMessage) {
    state.value.messages.push(message)
  }

  function reset() {
    state.value = {
      sessionId: null,
      subjectId: null,
      difficulty: null,
      questionCount: 10,
      currentOrder: 0,
      messages: [],
    }
  }

  return { state, start, appendMessage, reset }
})
