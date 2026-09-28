import type { NextRequest } from 'next/server'
import { getServerEnv } from '@/lib/env'

export async function fetchFromBackend(
  req: NextRequest,
  path: string,
  init: RequestInit = {},
  timeoutMs?: number,
) {
  const { backendRequestTimeoutMs, localBackendUrl } = getServerEnv()
  const effectiveTimeoutMs = timeoutMs ?? backendRequestTimeoutMs
  const requestUrl = `${localBackendUrl}${path}`
  const requestLabel = `${init.method ?? req.method} ${new URL(requestUrl).pathname}`
  const headers = new Headers(init.headers)
  const cookie = req.headers.get('cookie')
  if (cookie) {
    headers.set('cookie', cookie)
  }

  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), effectiveTimeoutMs)

  try {
    const response = await fetch(requestUrl, {
      ...init,
      headers,
      signal: controller.signal,
    })
    if (response.status >= 500) {
      console.error(`[backend-proxy] ${requestLabel} returned ${response.status}`)
    }
    return response
  } catch (err) {
    if (err instanceof Error && err.name === 'AbortError') {
      console.error(`[backend-proxy] ${requestLabel} timed out after ${effectiveTimeoutMs}ms`, err)
      return new Response(null, { status: 504 })
    }
    console.error(`[backend-proxy] ${requestLabel} failed`, err)
    throw err
  } finally {
    clearTimeout(timeoutId)
  }
}
