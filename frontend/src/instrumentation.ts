import { getServerEnv } from '@/lib/env'

export async function register() {
  if (process.env.NEXT_PHASE !== 'phase-production-build') {
    getServerEnv()
  }
  console.log(`[gentool-data-viewer-frontend] commit=${process.env.GIT_COMMIT_SHORT} built=${process.env.BUILD_TIME}`)
}
