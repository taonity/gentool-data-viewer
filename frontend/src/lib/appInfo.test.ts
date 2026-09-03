import { describe, expect, it } from 'vitest'

import { deploymentTime, infoRows, type InfoObject } from '@/lib/appInfo'

const buildTime = '2026-08-21T10:59:25.345Z'
const commit = '2b5a2c2261e22ce18378a66a14a82218040f857b'

describe('app info', () => {
  it('selects useful release fields from Spring Boot actuator info', () => {
    const data: InfoObject = {
      app: { name: 'backend', version: '0.14.0' },
      git: { commit, branch: 'main' },
      build: { time: buildTime },
    }

    expect(infoRows(data)).toMatchObject([
      { key: 'version', label: 'Version', value: '0.14.0' },
      { key: 'revision', label: 'Revision', value: '2b5a2c2' },
      { key: 'branch', label: 'Branch', value: 'main' },
      { key: 'built', label: 'Built', value: '2026-08-21 10:59:25 UTC' },
    ])
    expect(deploymentTime(data)).toBe(buildTime)
  })

  it('selects release fields and formats process uptime from frontend info', () => {
    const data: InfoObject = {
      app: { name: 'gentool-data-viewer-frontend', version: '0.1.0' },
      git: { commit, branch: 'main' },
      build: { time: buildTime, nodeVersion: 'v26.7.0' },
      runtime: { env: 'development', uptime: 183845 },
    }

    expect(infoRows(data).map(({ key }) => key)).toEqual([
      'version',
      'revision',
      'branch',
      'built',
      'uptime',
    ])
    expect(infoRows(data).at(-1)).toEqual({
      key: 'uptime',
      label: 'Uptime',
      value: '2d 3h 4m 5s',
    })
  })

  it('omits missing and unknown fields', () => {
    expect(infoRows({
      app: { version: 'unknown' },
      git: { commit: 'unknown', branch: 'main' },
    })).toEqual([{ key: 'branch', label: 'Branch', value: 'main' }])
  })
})
