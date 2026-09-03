import { fetchWithTimeout } from '@/lib/clientApi'
import { getRuntimeConfig } from '@/lib/runtimeConfig'
import { getCookie } from '@/lib/cookies'
import type {
  AccessInfo,
  AuditLog,
  ConfigSchema,
  ConsoleRole,
  PageResponse,
  PendingRequest,
  ReplayCollectionJob,
  Replay,
  CpuBenchmarkSync,
  CpuPlayer,
  CpuPlayerSummary,
  UserSummary,
} from './types'

const BASE = '/api/console'

async function csrfToken(): Promise<string> {
  const config = await getRuntimeConfig()
  return getCookie(config.csrfCookieName) || ''
}

class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message)
  }
}

/** Extracts the backend's error message (ClientErrorResponse.errorMessage) if present. */
async function errorMessage(res: Response): Promise<string> {
  try {
    const data = (await res.json()) as { errorMessage?: string }
    if (data && typeof data.errorMessage === 'string' && data.errorMessage) {
      return data.errorMessage
    }
  } catch {
    // ignore parse errors, fall back to generic message
  }
  return `Request failed (${res.status})`
}

async function get<T>(path: string): Promise<T> {
  const res = await fetchWithTimeout(`${BASE}${path}`, { timeoutMs: 10000 })
  if (!res.ok) {
    throw new ApiError(res.status, `Request failed (${res.status})`)
  }
  return res.json() as Promise<T>
}

async function mutate<T>(path: string, method: string, body?: unknown, timeoutMs = 10000): Promise<T | null> {
  const token = await csrfToken()
  const res = await fetchWithTimeout(`${BASE}${path}`, {
    method,
    headers: {
      'X-XSRF-TOKEN': token,
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
    timeoutMs,
  })
  if (!res.ok) {
    throw new ApiError(res.status, await errorMessage(res))
  }
  if (res.status === 204) {
    return null
  }
  const text = await res.text()
  return text ? (JSON.parse(text) as T) : null
}

export const consoleApi = {
  ApiError,

  getAccess: () => get<AccessInfo>('/access/me'),

  requestAccess: (requestedRole: ConsoleRole) =>
    mutate<AccessInfo>('/access/request', 'POST', { requestedRole }),

  listPendingRequests: () => get<PendingRequest[]>('/access/requests'),

  approveRequest: (googleId: string, role: ConsoleRole) =>
    mutate(`/access/requests/${encodeURIComponent(googleId)}/approve`, 'POST', { role }),

  rejectRequest: (googleId: string) =>
    mutate(`/access/requests/${encodeURIComponent(googleId)}/reject`, 'POST'),

  listUsers: () => get<UserSummary[]>('/users'),

  changeUserRole: (googleId: string, role: ConsoleRole) =>
    mutate<UserSummary>(`/users/${encodeURIComponent(googleId)}/role`, 'PUT', { role }),

  listAuditLogs: (page: number, size: number, q?: string, field?: string) =>
    get<PageResponse<AuditLog>>(buildListQuery('/audit-logs', page, size, q, field)),

  getConfig: () => get<ConfigSchema>('/config'),

  updateConfig: (values: Record<string, unknown>) =>
    mutate<ConfigSchema>('/config', 'PUT', { values }),

  resetConfig: (key: string) =>
    mutate<ConfigSchema>(`/config/${encodeURIComponent(key)}`, 'DELETE'),

  listReplayCollectionJobs: () => get<ReplayCollectionJob[]>('/replay-collection/jobs'),

  startReplayCollection: (startDate: string, endDate: string, userLimit?: number) =>
    mutate<{ jobId: string }>('/replay-collection/jobs', 'POST', { startDate, endDate, userLimit }),

  listReplays: (page: number, size: number, q?: string, field?: string, sort?: string, direction?: string) =>
    get<PageResponse<Replay>>(buildListQuery('/replays', page, size, q, field, sort, direction)),

  listCpuPlayers: (page: number, size: number, q?: string, field?: string, sort?: string, direction?: string) =>
    get<PageResponse<CpuPlayer>>(buildListQuery('/cpu-players', page, size, q, field, sort, direction)),

  getCpuPlayerSummary: () => get<CpuPlayerSummary>('/cpu-players/summary'),

  refreshCpuBenchmarks: () =>
    mutate<CpuBenchmarkSync>('/cpu-players/benchmarks/refresh', 'POST', undefined, 120000),
}

function buildListQuery(
  path: string,
  page: number,
  size: number,
  q?: string,
  field?: string,
  sort?: string,
  direction?: string,
): string {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (q && q.trim()) {
    params.set('q', q.trim())
    if (field && field !== 'all') {
      params.set('field', field)
    }
  }
  if (sort) {
    params.set('sort', sort)
  }
  if (direction) {
    params.set('direction', direction)
  }
  return `${path}?${params.toString()}`
}
