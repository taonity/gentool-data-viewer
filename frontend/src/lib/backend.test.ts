import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { fetchFromBackend } from './backend'
import { getServerEnv } from './env'

vi.mock('./env', () => ({ getServerEnv: vi.fn() }))

const request = new NextRequest('http://frontend.test/api/console/replays')

beforeEach(() => {
  vi.mocked(getServerEnv).mockReturnValue({
    profile: 'stage',
    localBackendUrl: 'http://backend:8080',
    publicBackendUrl: 'https://api.example.com',
    csrfCookieName: 'XSRF-TOKEN',
    backendRequestTimeoutMs: 60_000,
    benchmarkRefreshTimeoutMs: 240_000,
    benchmarkRefreshClientTimeoutMs: 250_000,
  })
  vi.spyOn(console, 'error').mockImplementation(() => undefined)
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('backend proxy logging', () => {
  it('logs backend server errors without logging query parameters', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 500 })))

    await fetchFromBackend(request, '/console/replays?q=private-search')

    expect(console.error).toHaveBeenCalledWith('[backend-proxy] GET /console/replays returned 500')
  })

  it('logs connection failures and rethrows them', async () => {
    const failure = new Error('connection refused')
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(failure))

    await expect(fetchFromBackend(request, '/console/replays')).rejects.toBe(failure)
    expect(console.error).toHaveBeenCalledWith('[backend-proxy] GET /console/replays failed', failure)
  })

  it('logs timeouts and returns a gateway timeout', async () => {
    vi.useFakeTimers()
    vi.stubGlobal('fetch', vi.fn((_url: string, init: RequestInit) => new Promise((_resolve, reject) => {
      init.signal?.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')))
    })))

    const responsePromise = fetchFromBackend(request, '/console/replays', {}, 25)
    await vi.advanceTimersByTimeAsync(25)
    const response = await responsePromise

    expect(response.status).toBe(504)
    expect(console.error).toHaveBeenCalledWith(
      '[backend-proxy] GET /console/replays timed out after 25ms',
      expect.objectContaining({ name: 'AbortError' }),
    )
  })
})
