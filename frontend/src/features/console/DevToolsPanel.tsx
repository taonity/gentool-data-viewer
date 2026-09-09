'use client'

import { useCallback, useEffect, useState } from 'react'
import { Bug, ClipboardCopy, LoaderCircle, LogIn, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { getRuntimeConfig } from '@/lib/runtimeConfig'
import type { StubLogin } from './types'
import {
  isDevelopmentBuild,
  setDevLoadingEnabled,
  useDevLoading,
} from './devLoading'

function roundCssPixel(value: number): number {
  return Math.round(value * 100) / 100
}

function storedWidths(storageKey: string | undefined): Record<string, number> {
  if (!storageKey) return {}
  try {
    return JSON.parse(localStorage.getItem(storageKey) ?? '{}') as Record<string, number>
  } catch {
    return {}
  }
}

async function copyColumnBorders(): Promise<number> {
  const tables = [...document.querySelectorAll<HTMLTableElement>('table[data-column-layout-key]')]
    .filter((table) => table.offsetParent !== null)
    .map((table) => {
      const tableRect = table.getBoundingClientRect()
      const container = table.closest<HTMLElement>('[data-slot="table-container"]')
      return {
        key: table.dataset.columnLayoutKey,
        table: {
          width: roundCssPixel(tableRect.width),
          viewportLeft: roundCssPixel(tableRect.left),
          viewportRight: roundCssPixel(tableRect.right),
          scrollLeft: roundCssPixel(container?.scrollLeft ?? 0),
          visibleWidth: roundCssPixel(container?.clientWidth ?? tableRect.width),
        },
        savedWidthOverrides: storedWidths(table.dataset.columnWidthsStorageKey),
        columns: [...table.querySelectorAll<HTMLElement>('[data-column-key]')].map((header) => {
          const rect = header.getBoundingClientRect()
          return {
            key: header.dataset.columnKey,
            label: header.dataset.columnLabel,
            width: roundCssPixel(rect.width),
            left: roundCssPixel(rect.left - tableRect.left),
            right: roundCssPixel(rect.right - tableRect.left),
            viewportLeft: roundCssPixel(rect.left),
            viewportRight: roundCssPixel(rect.right),
          }
        }),
      }
    })

  const exportedAt = new Date()
  const activeTab = localStorage.getItem('console.activeTab') ?? 'unknown'
  const payload = {
    version: 1,
    scope: 'current-tab',
    exportedAt: exportedAt.toISOString(),
    path: window.location.pathname,
    activeTab,
    viewport: {
      width: window.innerWidth,
      height: window.innerHeight,
      devicePixelRatio: window.devicePixelRatio,
    },
    tables,
  }
  await navigator.clipboard.writeText(JSON.stringify(payload, null, 2))
  return tables.length
}

export function DevToolsPanel() {
  const [open, setOpen] = useState(false)
  const [logins, setLogins] = useState<StubLogin[] | null>(null)
  const [backendUrl, setBackendUrl] = useState('')
  const [exportMessage, setExportMessage] = useState<string | null>(null)
  const forceLoading = useDevLoading()
  const hidden =
    process.env.NEXT_PUBLIC_HIDE_DEV_TOOLS === 'true' ||
    process.env.NEXT_PUBLIC_HIDE_DEV_LOGIN === 'true'

  const toggle = useCallback(() => setOpen((value) => !value), [])

  useEffect(() => {
    if (!isDevelopmentBuild || hidden) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.altKey && event.shiftKey && event.code === 'KeyD') {
        event.preventDefault()
        toggle()
      } else if (event.key === 'Escape') {
        setOpen(false)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [hidden, toggle])

  useEffect(() => {
    if (!open || logins || hidden) return
    let active = true
    void (async () => {
      try {
        const [response, config] = await Promise.all([
          fetch('/api/dev/stub-users', { cache: 'no-store' }),
          getRuntimeConfig(),
        ])
        if (!response.ok || !active) return
        setBackendUrl(config.publicBackendUrl)
        setLogins((await response.json()) as StubLogin[])
      } catch {
        if (active) setLogins([])
      }
    })()
    return () => {
      active = false
    }
  }, [hidden, logins, open])

  if (!isDevelopmentBuild || hidden) return null

  if (!open) {
    return (
      <Button
        type="button"
        size="icon"
        variant="outline"
        className="fixed right-3 bottom-3 z-[60] size-8 bg-background/95 shadow-md backdrop-blur"
        onClick={() => setOpen(true)}
        aria-label="Open developer tools"
        title="Developer tools (Alt+Shift+D)"
        data-dev-tools-trigger
      >
        <Bug />
      </Button>
    )
  }

  return (
    <aside
      className="fixed right-3 bottom-3 z-[60] w-[min(22rem,calc(100vw-1.5rem))] rounded-lg border bg-card/95 p-2.5 shadow-lg backdrop-blur"
      aria-label="Developer tools"
      data-dev-tools-panel
      data-force-loading={forceLoading}
    >
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 text-sm font-medium">
          <Bug className="size-4" />
          Developer tools
        </div>
        <Button
          type="button"
          size="icon-sm"
          variant="ghost"
          onClick={() => setOpen(false)}
          aria-label="Close developer tools"
          title="Close (Escape)"
        >
          <X />
        </Button>
      </div>

      <div className="mt-2 flex flex-col gap-2 border-t pt-2">
        <Button
          type="button"
          size="sm"
          variant={forceLoading ? 'default' : 'outline'}
          className="justify-start"
          onClick={() => {
            const enabled = !forceLoading
            setDevLoadingEnabled(enabled)
            if (enabled && window.location.pathname === '/login') {
              window.location.href = '/'
            }
          }}
          aria-pressed={forceLoading}
          data-dev-loading-toggle
        >
          <LoaderCircle className={forceLoading ? 'animate-spin' : ''} />
          {forceLoading ? 'Resume live UI' : 'Hold loading UI'}
        </Button>

        <Button
          type="button"
          size="sm"
          variant="outline"
          className="justify-start"
          title="Copy resizable column borders from the current tab"
          onClick={() => {
            void copyColumnBorders().then((count) => {
              setExportMessage(count > 0 ? `Copied ${count} table${count === 1 ? '' : 's'}.` : 'No resizable tables in this tab.')
            }).catch(() => {
              setExportMessage('Clipboard access failed.')
            })
          }}
        >
          <ClipboardCopy />
          Copy column borders
        </Button>
        {exportMessage && <span className="px-1 text-xs text-muted-foreground" aria-live="polite">{exportMessage}</span>}

        {logins && logins.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {logins.map((login) => (
              <Button
                key={login.registrationId}
                type="button"
                size="xs"
                variant="outline"
                onClick={() => {
                  window.location.href = `${backendUrl}/oauth2/authorization/${login.registrationId}`
                }}
              >
                <LogIn />
                {login.label}
              </Button>
            ))}
          </div>
        )}
      </div>
    </aside>
  )
}
