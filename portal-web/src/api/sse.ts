import { ApiError } from './request'
import type { ApiResponse } from '@/types/api'
import { getAccessToken } from '@/utils/token'

/**
 * SSE 消费工具（API 04 §2.9）。
 *
 * AI 面试逐题评分使用 POST + text/event-stream，EventSource 仅支持 GET，
 * 因此使用 fetch + ReadableStream 解析事件帧。
 */

export interface SseStatusPayload {
  phase: 'analyzing' | 'grading' | 'thinking'
}

export interface SseTokenPayload {
  delta: string
}

export interface SseGradePayload {
  order: number
  score: number
  feedback: string
  degraded: boolean
  referencePoints?: string[]
}

export interface SseErrorPayload {
  code: number
  message: string
}

export interface SseHandlers {
  onStatus?: (payload: SseStatusPayload) => void
  onToken?: (payload: SseTokenPayload) => void
  onGrade?: (payload: SseGradePayload) => void
  onError?: (payload: SseErrorPayload) => void
}

export async function postSse(
  path: string,
  body: unknown,
  handlers: SseHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const token = getAccessToken()
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
    signal,
  })

  if (!response.ok) {
    let payload: ApiResponse<unknown> | undefined
    try {
      payload = (await response.json()) as ApiResponse<unknown>
    } catch {
      payload = undefined
    }
    throw new ApiError(
      payload?.code ?? -1,
      payload?.message ?? `请求失败（${response.status}）`,
      payload?.fieldErrors,
      response.status,
    )
  }
  if (!response.body) {
    throw new ApiError(-1, '当前浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const frames = buffer.split(/\r?\n\r?\n/)
    buffer = frames.pop() ?? ''
    for (const frame of frames) {
      dispatchFrame(frame, handlers)
    }
  }
  if (buffer.trim()) {
    dispatchFrame(buffer, handlers)
  }
}

function dispatchFrame(frame: string, handlers: SseHandlers): void {
  let event = 'message'
  const dataLines: string[] = []
  for (const line of frame.split(/\r?\n/)) {
    if (!line || line.startsWith(':')) {
      continue
    }
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }
  if (dataLines.length === 0) {
    return
  }
  let payload: unknown
  try {
    payload = JSON.parse(dataLines.join('\n'))
  } catch {
    return
  }
  switch (event) {
    case 'status':
      handlers.onStatus?.(payload as SseStatusPayload)
      break
    case 'token':
      handlers.onToken?.(payload as SseTokenPayload)
      break
    case 'grade':
      handlers.onGrade?.(payload as SseGradePayload)
      break
    case 'error':
      handlers.onError?.(payload as SseErrorPayload)
      break
    default:
      break
  }
}
