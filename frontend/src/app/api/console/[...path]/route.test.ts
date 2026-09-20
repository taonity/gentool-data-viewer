import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NextRequest } from 'next/server'
import { fetchFromBackend } from '@/lib/backend'
import { getServerEnv } from '@/lib/env'
import { POST } from './route'

vi.mock('@/lib/backend', () => ({ fetchFromBackend: vi.fn() }))
vi.mock('@/lib/env', () => ({ getServerEnv: vi.fn() }))

beforeEach(() => {
  vi.mocked(fetchFromBackend).mockResolvedValue(new Response('{}'))
  vi.mocked(getServerEnv).mockReturnValue({
    profile: 'stage',
    localBackendUrl: 'http://backend:8080',
    publicBackendUrl: 'https://api.example.com',
    csrfCookieName: 'XSRF-TOKEN',
    backendRequestTimeoutMs: 60_000,
    benchmarkRefreshTimeoutMs: 240_000,
    benchmarkRefreshClientTimeoutMs: 250_000,
  })
})

describe('console backend proxy timeout', () => {
  it('extends the timeout for CPU benchmark refresh', async () => {
    const request = new NextRequest('http://localhost/api/console/cpu-players/benchmarks/refresh', {
      method: 'POST',
    })

    await POST(request, {
      params: Promise.resolve({ path: ['cpu-players', 'benchmarks', 'refresh'] }),
    })

    expect(fetchFromBackend).toHaveBeenCalledWith(
      request,
      '/console/cpu-players/benchmarks/refresh',
      expect.objectContaining({ method: 'POST' }),
      240_000,
    )
  })

  it('uses the default timeout for other console requests', async () => {
    const request = new NextRequest('http://localhost/api/console/replay-rescans', {
      method: 'POST',
    })

    await POST(request, {
      params: Promise.resolve({ path: ['replay-rescans'] }),
    })

    expect(fetchFromBackend).toHaveBeenCalledWith(
      request,
      '/console/replay-rescans',
      expect.objectContaining({ method: 'POST' }),
      undefined,
    )
  })
})