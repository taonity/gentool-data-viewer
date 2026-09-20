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
    localBackendUrl: process.env.LOCAL_BACKEND_URL?.trim() || config.localBackendUrl,
  }
}
