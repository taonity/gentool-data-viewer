import type { NextRequest } from 'next/server'
import { getServerEnv } from '@/lib/env'

export async function fetchFromBackend(
  req: NextRequest,
  path: string,
  init: RequestInit = {},
  timeoutMs?: number,
) {
  const { backendRequestTimeoutMs, localBackendUrl } = getServerEnv()
  const headers = new Headers(init.headers)
  const cookie = req.headers.get('cookie')
  if (cookie) {
    headers.set('cookie', cookie)
  }

  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs ?? backendRequestTimeoutMs)

  try {
    return await fetch(`${localBackendUrl}${path}`, {
      ...init,
      headers,
      signal: controller.signal,
    })
  } catch (err) {
    if (err instanceof Error && err.name === 'AbortError') {
      return new Response(null, { status: 504 })
    }
    throw err
  } finally {
    clearTimeout(timeoutId)
  }
}
