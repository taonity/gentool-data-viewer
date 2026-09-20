import { NextResponse } from 'next/server'
import { getServerEnv } from '@/lib/env'

export const dynamic = 'force-dynamic'
export const runtime = 'nodejs'

export async function GET() {
  const env = getServerEnv()
  return NextResponse.json({
    profile: env.profile,
    csrfCookieName: env.csrfCookieName,
    publicBackendUrl: env.publicBackendUrl,
    benchmarkRefreshClientTimeoutMs: env.benchmarkRefreshClientTimeoutMs,
  })
}
