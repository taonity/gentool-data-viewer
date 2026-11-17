import {
  FRONTEND_PROFILE_CONFIG,
  isFrontendProfile,
  type FrontendProfile,
} from '@/config/environmentProfiles'

export interface ServerEnv {
  profile: FrontendProfile
  localBackendUrl: string
  publicBackendUrl: string
  csrfCookieName: string
  backendRequestTimeoutMs: number
  benchmarkRefreshTimeoutMs: number
  benchmarkRefreshClientTimeoutMs: number
}

function required(name: string): string {
  const value = process.env[name]?.trim()
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`)
  }
  return value
}

function positiveInteger(name: string, fallback: number): number {
  const raw = process.env[name]?.trim()
  if (!raw) return fallback
  const value = Number(raw)
  if (!Number.isSafeInteger(value) || value <= 0 || value > 2_147_483_647) {
    throw new Error(`${name} must be a positive integer no greater than 2147483647`)
  }
  return value
}

function frontendProfile(): FrontendProfile {
  const profile = required('FRONTEND_PROFILE')
  if (!isFrontendProfile(profile)) {
    throw new Error(`FRONTEND_PROFILE must be one of: local, stage, prod`)
  }
  return profile
}

export function getServerEnv(): ServerEnv {
  const profile = frontendProfile()
  const config = FRONTEND_PROFILE_CONFIG[profile]

  return {
    profile,
    ...config,
    backendRequestTimeoutMs: positiveInteger('BACKEND_REQUEST_TIMEOUT_MS', config.backendRequestTimeoutMs),
    localBackendUrl: process.env.LOCAL_BACKEND_URL?.trim() || config.localBackendUrl,
  }
}
