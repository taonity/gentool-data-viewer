import { afterEach, describe, expect, it, vi } from 'vitest'

import { getBuildInfo } from '@/lib/buildInfo'

describe('frontend build info', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('returns release identity and process uptime', () => {
    vi.stubEnv('APP_VERSION', '0.1.0')
    vi.stubEnv('GIT_COMMIT_SHA', '2b5a2c2261e22ce18378a66a14a82218040f857b')
    vi.stubEnv('GIT_BRANCH', 'main')
    vi.stubEnv('BUILD_TIME', '2026-08-21T10:59:25.345Z')

    expect(getBuildInfo()).toEqual({
      app: {
        name: 'gentool-data-viewer-frontend',
        version: '0.1.0',
      },
      git: {
        commit: '2b5a2c2261e22ce18378a66a14a82218040f857b',
        branch: 'main',
      },
      build: {
        time: '2026-08-21T10:59:25.345Z',
      },
      runtime: {
        uptime: expect.any(Number),
      },
    })
  })
})
