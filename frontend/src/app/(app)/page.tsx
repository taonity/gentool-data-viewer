'use client'

import { useEffect, useState } from 'react'
import { LogIn } from 'lucide-react'
import { checkBackendLiveness, fetchAuthenticatedUserStatus, requestLogout } from '@/lib/auth'
import { getRuntimeConfig } from '@/lib/runtimeConfig'
import { deleteCookie, getCookie } from '@/lib/cookies'
import ErrorNotification from '@/components/ErrorNotification'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import DataConsole from '@/features/console/DataConsole'
import { isDevLoadingEnabled, useDevLoading } from '@/features/console/devLoading'

interface AuthenticatedUser {
  email: string
}

export default function Home() {
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [authenticating, setAuthenticating] = useState(false)
  const forceLoading = useDevLoading()

  useEffect(() => {
    const authError = getCookie('auth_error')
    if (authError) {
      deleteCookie('auth_error')
      setError(
        authError === 'UNAUTHORIZED_ACCOUNT'
          ? 'Your account is not authorized to access protected tools.'
          : 'Authentication failed. Please try again.',
      )
    }
  }, [])

  useEffect(() => {
    if (forceLoading || isDevLoadingEnabled()) {
      setLoading(true)
      return
    }

    async function checkAuth() {
      const result = await fetchAuthenticatedUserStatus()

      if (result.status === 'authenticated') {
        setUser(result.data)
      } else if (result.status === 'unauthenticated') {
        setUser(null)
      } else {
        setError(result.message)
      }

      setLoading(false)
    }

    checkAuth()
  }, [forceLoading])

  const handleLogout = async () => {
    try {
      const config = await getRuntimeConfig()
      const xsrfToken = getCookie(config.csrfCookieName) || ''

      await requestLogout(xsrfToken)
      setUser(null)
    } catch {
      setError('Logout failed. Please try again.')
    }
  }

  const handleLogin = async () => {
    setError(null)
    setAuthenticating(true)
    const liveness = await checkBackendLiveness()
    if (!liveness.ok) {
      setError(liveness.message)
      setAuthenticating(false)
      return
    }
    try {
      const config = await getRuntimeConfig()
      window.location.href = `${config.publicBackendUrl}/oauth2/authorization/discord-gentool-data-viewer`
    } catch {
      setError('Application configuration is unavailable. Please try again later.')
      setAuthenticating(false)
    }
  }

  return (
    <div className="flex justify-center px-3 py-4 sm:px-6">
      <div className="w-full max-w-[1200px]">
        {/* Reserve room for the fixed theme toggle until the viewport is wide enough that it
            clears the centered content (~1300px), so Log out never sits under it. */}
        <header className="flex flex-wrap items-center justify-between gap-3 border-b pb-3 pr-10 min-[1300px]:pr-0">
          <div className="flex min-w-0 items-baseline gap-2">
            <span className="text-lg font-semibold">Data console</span>
            {loading || forceLoading ? (
              <Skeleton className="h-4 w-40" />
            ) : (
              user && (
                <span className="truncate text-sm text-muted-foreground">{user.email}</span>
              )
            )}
          </div>
          {!loading && (
            user ? (
              <Button variant="outline" size="sm" onClick={handleLogout}>Log out</Button>
            ) : (
              <Button size="sm" disabled={authenticating} onClick={handleLogin}>
                <LogIn />
                {authenticating ? 'Connecting...' : 'Log in'}
              </Button>
            )
          )}
        </header>

        <main className="pt-4">
          {error && <ErrorNotification message={error} onClose={() => setError(null)} />}
          <DataConsole authenticated={user !== null} forceLoading={forceLoading} />
        </main>
      </div>
    </div>
  )
}
