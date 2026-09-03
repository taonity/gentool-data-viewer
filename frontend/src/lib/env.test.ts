import { afterEach, describe, expect, it, vi } from 'vitest'

import { getServerEnv } from '@/lib/env'

afterEach(() => {
  vi.unstubAllEnvs()
})

describe('server environment', () => {
  it.each([
    ['local', 'http://127.0.0.1:8080', 'XSRF-TOKEN-LOCAL'],
    ['stage', 'https://gentool-data-viewer-api-stage.taonity.org', 'XSRF-TOKEN-GENTOOL-DATA-VIEWER-STAGE'],
    ['prod', 'https://gentool-data-viewer-api.taonity.org', 'XSRF-TOKEN-GENTOOL-DATA-VIEWER-PROD'],
  ])('returns the %s profile configuration', (profile, backendUrl, csrfCookieName) => {
    vi.stubEnv('FRONTEND_PROFILE', profile)

    expect(getServerEnv()).toEqual({
      profile,
      localBackendUrl: backendUrl,
      publicBackendUrl: backendUrl,
      csrfCookieName,
    })
  })

  it('rejects a missing profile', () => {
    vi.stubEnv('FRONTEND_PROFILE', '')

    expect(() => getServerEnv()).toThrow('Missing required environment variable: FRONTEND_PROFILE')
  })

  it('rejects an unknown profile', () => {
    vi.stubEnv('FRONTEND_PROFILE', 'qa')

    expect(() => getServerEnv()).toThrow('FRONTEND_PROFILE must be one of: local, stage, prod')
  })

  it('uses the backend URL supplied by the runtime topology', () => {
    vi.stubEnv('FRONTEND_PROFILE', 'local')
    vi.stubEnv('LOCAL_BACKEND_URL', 'http://backend:8080')

    expect(getServerEnv().localBackendUrl).toBe('http://backend:8080')
  })
})
