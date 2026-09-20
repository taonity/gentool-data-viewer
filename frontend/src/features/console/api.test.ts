import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchWithTimeout } from '@/lib/clientApi'
import { getRuntimeConfig } from '@/lib/runtimeConfig'
import { consoleApi } from './api'

vi.mock('@/lib/clientApi', () => ({ fetchWithTimeout: vi.fn() }))
vi.mock('@/lib/runtimeConfig', () => ({ getRuntimeConfig: vi.fn() }))

beforeEach(() => {
  vi.mocked(fetchWithTimeout).mockClear()
  vi.mocked(fetchWithTimeout).mockResolvedValue(new Response('{}', { status: 200 }))
  vi.mocked(getRuntimeConfig).mockResolvedValue({
    profile: 'stage',
    csrfCookieName: 'XSRF-TOKEN',
    publicBackendUrl: 'https://api.example.com',
    benchmarkRefreshClientTimeoutMs: 250_000,
  })
})

function requestedUrl(): URL {
  const request = vi.mocked(fetchWithTimeout).mock.calls[0]
  if (!request) throw new Error('Expected a backend request')
  return new URL(String(request[0]), 'https://example.invalid')
}

describe('match date period requests', () => {
  it('loads full dataset bounds independently of table filters', async () => {
    await consoleApi.getReplayDateRange()
    expect(requestedUrl().pathname).toBe('/api/console/replays/date-range')
    expect(requestedUrl().search).toBe('')
  })

  it('combines player dates with search sorting pagination and link filters', async () => {
    await consoleApi.listCpuPlayers(2, 20, 'Alpha', 'mainName', 'replayCount', 'desc', true, 'ABC', true, '2026-09-08', '2026-09-09')
    expect(Object.fromEntries(requestedUrl().searchParams)).toEqual({
      page: '2', size: '20', q: 'Alpha', field: 'mainName', sort: 'replayCount', direction: 'desc',
      linkedOnly: 'true', player: 'ABC', exact: 'true', startDate: '2026-09-08', endDate: '2026-09-09',
    })
  })

  it('uses the same period for pinned rankings', async () => {
    await consoleApi.listMyRankedCpuPlayers('score', 'asc', true, '2026-09-08', '2026-09-09')
    expect(requestedUrl().searchParams.get('startDate')).toBe('2026-09-08')
    expect(requestedUrl().searchParams.get('endDate')).toBe('2026-09-09')
    expect(requestedUrl().searchParams.get('linkedOnly')).toBe('true')
  })

  it('supports open-ended replay periods without losing the reporter filter', async () => {
    await consoleApi.listReplays(0, 20, 'Alias', 'players', 'matchAt', 'desc', true, ['ABC'], undefined, '', '2026-09-09')
    const params = requestedUrl().searchParams
    expect(params.has('startDate')).toBe(false)
    expect(params.get('endDate')).toBe('2026-09-09')
    expect(params.get('reporterIds')).toBe('ABC')
    expect(params.get('exact')).toBe('true')
  })

  it('omits cleared date parameters for all time summaries', async () => {
    await consoleApi.getCpuPlayerSummary('', '')
    expect(requestedUrl().searchParams.has('startDate')).toBe(false)
    expect(requestedUrl().searchParams.has('endDate')).toBe(false)
  })

  it('allows benchmark refresh to outlive the backend proxy timeout', async () => {
    await consoleApi.refreshCpuBenchmarks()

    expect(fetchWithTimeout).toHaveBeenCalledWith(
      '/api/console/cpu-players/benchmarks/refresh',
      expect.objectContaining({ method: 'POST', timeoutMs: 250_000 }),
    )
  })
})