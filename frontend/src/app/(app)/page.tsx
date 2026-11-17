'use client'

import { useEffect, useState } from 'react'
import Image from 'next/image'
import { checkBackendLiveness, fetchAuthenticatedUserStatus, requestLogout, POST_LOGIN_PATH_KEY, safeLocalPath } from '@/lib/auth'
import { getRuntimeConfig } from '@/lib/runtimeConfig'
import { deleteCookie, getCookie } from '@/lib/cookies'
import ErrorNotification from '@/components/ErrorNotification'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import DataConsole from '@/features/console/DataConsole'
import { isDevLoadingEnabled, useDevLoading } from '@/features/console/devLoading'

interface AuthenticatedUser {
  displayName: string
}

export default function Home() {
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [authenticating, setAuthenticating] = useState(false)
  const [authButtonTouchSize, setAuthButtonTouchSize] = useState(false)
  const forceLoading = useDevLoading()

  useEffect(() => {
    // Keep the header action's dimensions for this page, even if the viewport changes.
    setAuthButtonTouchSize(window.matchMedia('(width < 768px)').matches)
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
        const returnPath = safeLocalPath(sessionStorage.getItem(POST_LOGIN_PATH_KEY))
        sessionStorage.removeItem(POST_LOGIN_PATH_KEY)
        if (returnPath && returnPath !== `${location.pathname}${location.search}${location.hash}`) {
          location.replace(returnPath)
          return
        }
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
      sessionStorage.setItem(POST_LOGIN_PATH_KEY, `${location.pathname}${location.search}${location.hash}`)
      window.location.href = `${config.publicBackendUrl}/oauth2/authorization/discord-gentool-data-viewer`
    } catch {
      setError('Application configuration is unavailable. Please try again later.')
      setAuthenticating(false)
    }
  }

  return (
    <div className="console-shell flex flex-1 justify-center px-2 py-2 sm:px-6 sm:py-4">
      <div className="w-full min-w-0 max-w-[1200px]">
        {/* Reserve room for the fixed theme toggle until the viewport is wide enough that it
            clears the centered content (~1300px), so Log out never sits under it. */}
        <header className="console-header flex items-center justify-between gap-2 border-b pb-2 pr-11 sm:pb-3 min-[1300px]:pr-0">
          <div className="flex min-w-0 flex-1 flex-col gap-0.5 sm:flex-row sm:items-baseline sm:gap-2">
            <span className="shrink-0 text-base font-semibold sm:text-lg">Data console</span>
            <div className="h-5 min-w-0 truncate text-sm text-muted-foreground">
              {loading || forceLoading ? (
                <Skeleton className="h-full w-40 max-w-full" />
              ) : user?.displayName ?? 'Guest'}
            </div>
          </div>
          {!loading && (
            user ? (
              <Button variant="outline" size="sm" className="console-auth-button" data-touch-size={authButtonTouchSize || undefined} onClick={handleLogout}>Log out</Button>
            ) : (
              <Button variant="outline" size="sm" className="console-auth-button" data-discord data-touch-size={authButtonTouchSize || undefined} aria-label="Log in with Discord" aria-busy={authenticating} title="Log in with Discord" disabled={authenticating} onClick={handleLogin}>
                <Image src="/discord-logo.svg" alt="" width={14} height={14} className="console-auth-logo shrink-0" unoptimized aria-hidden="true" />
                <span>Log in</span>
              </Button>
            )
          )}
          <span className="sr-only" role="status">{authenticating ? 'Connecting to Discord…' : ''}</span>
        </header>

        <main className="pt-2 sm:pt-4">
          {error && <ErrorNotification message={error} onClose={() => setError(null)} />}
          <DataConsole
            authenticated={user !== null}
            authenticationLoading={loading}
            forceLoading={forceLoading}
          />
        </main>


      </div>
    </div>
  )
}
