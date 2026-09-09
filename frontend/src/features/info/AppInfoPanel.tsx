'use client'

import { useEffect, useState } from 'react'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import {
  deploymentTime,
  fetchBackendInfo,
  fetchFrontendInfo,
  formatRelativeAge,
  GITHUB_REPO_URL,
  infoRows,
  type AppInfoSource,
  type InfoRow,
} from '@/lib/appInfo'

interface AppInfoPanelProps {
  /** Compact mode tightens spacing/typography for the login card. */
  compact?: boolean
  className?: string
  forceLoading?: boolean
  active?: boolean
}

/**
 * Remembers how many rows each source rendered last time so the skeleton reserves the same
 * height on the next mount and the layout does not jump. Seeded with the typical field counts
 * so even the very first load is close to the final size.
 */
const rowCountCache: Record<string, number> = { Backend: 5, Frontend: 5 }

const LABELS = ['Backend', 'Frontend'] as const

/**
 * Displays service and release information. Compact mode, used on the login screen,
 * shows release information only.
 */
export function AppInfoPanel({ compact = false, className, forceLoading = false, active = true }: AppInfoPanelProps) {
  const [sources, setSources] = useState<AppInfoSource[] | null>(null)

  useEffect(() => {
    if (forceLoading || !active) return
    let mounted = true
    Promise.all([fetchBackendInfo(), fetchFrontendInfo()]).then(([backend, frontend]) => {
      if (!mounted) return
      setSources([
        { label: 'Backend', data: backend },
        { label: 'Frontend', data: frontend },
      ])
    })
    return () => {
      mounted = false
    }
  }, [active, forceLoading])

  const visibleSources = forceLoading ? null : sources

  return (
    <div className={className}>
      {!compact && <ServiceGuide />}
      <div className="grid gap-4 sm:grid-cols-2">
        {LABELS.map((label, index) => (
          <InfoCard
            key={label}
            label={label}
            source={visibleSources?.[index] ?? null}
            compact={compact}
          />
        ))}
      </div>
    </div>
  )
}

function ServiceGuide() {
  const issuesUrl = GITHUB_REPO_URL ? `${GITHUB_REPO_URL}/issues` : undefined

  return (
    <Card className="mb-4 text-left">
      <CardHeader className="border-b">
        <CardTitle>GenTool Data Viewer</CardTitle>
        <p className="max-w-3xl text-sm text-muted-foreground">
          A searchable view of public Command &amp; Conquer: Generals - Zero Hour replay reports,
          with match details, player hardware, and CPU comparisons.
        </p>
      </CardHeader>
      <CardContent className="grid gap-x-8 gap-y-5 md:grid-cols-2">
        <AboutSection title="Replays and scans">
          <p>
            Replay data comes from GenTool&apos;s public Zero Hour archive. The service reads GenTool&apos;s
            uploaded text reports and links associated replay files back to their source; it does not
            scan your computer.
          </p>
          <p>
            The automatic scan starts daily at 02:15 UTC and scans the previous calendar day; it does
            not backfill on startup. A user-requested rescan fetches a player&apos;s latest available data
            now, without waiting for the daily scan. It checks the latest 7 days and runs in the background.
          </p>
        </AboutSection>

        <AboutSection title="CPU rating">
          <p>
            The score is PassMark&apos;s Single Thread Rating matched against the CPU name reported by
            GenTool. It is a reference score, not a benchmark run by this service.
          </p>
          <p>
            Missing, unmatched, or ambiguous CPU names are left unrated. Ratings may change when the
            PassMark catalog is refreshed.
          </p>
        </AboutSection>

        <AboutSection title="Roles">
          <dl className="grid gap-2 sm:grid-cols-[7rem_1fr]">
            <dt className="font-medium text-foreground">Public</dt>
            <dd>Browse player, CPU, and replay data.</dd>
            <dt className="font-medium text-foreground">Viewer (default)</dt>
            <dd>Link a GenTool player and request recent rescans.</dd>
            <dt className="font-medium text-foreground">Editor</dt>
            <dd>Viewer access; no editor-only actions are currently available.</dd>
            <dt className="font-medium text-foreground">Admin</dt>
            <dd>Run collection jobs, refresh CPU data, and manage access and player links.</dd>
            <dt className="font-medium text-foreground">Owner</dt>
            <dd>Admin access plus runtime configuration and admin-role management.</dd>
          </dl>
        </AboutSection>

        <AboutSection title="Feedback">
          <p>
            Report bugs, incorrect replay or CPU data, and feature requests in{' '}
            {issuesUrl ? (
              <a
                href={issuesUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="font-medium text-primary underline decoration-dotted underline-offset-2 hover:decoration-solid"
              >
                GitHub Issues
              </a>
            ) : (
              'GitHub Issues'
            )}
            . Include the relevant player or replay and what you expected. Do not post credentials or
            private information.
          </p>
        </AboutSection>
      </CardContent>
    </Card>
  )
}

function AboutSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="space-y-2 text-sm text-foreground">
      <h3 className="font-semibold text-foreground">{title}</h3>
      {children}
    </section>
  )
}

function InfoCard({
  label,
  source,
  compact,
}: {
  label: string
  source: AppInfoSource | null
  compact: boolean
}) {
  const loading = source === null
  const rows = loading ? [] : infoRows(source.data)
  const available = !loading && rows.length > 0
  const deployedAt = loading ? null : deploymentTime(source.data)
  const deployedAgo = formatRelativeAge(deployedAt)

  if (available) {
    rowCountCache[label] = rows.length
  }
  const skeletonRows = rowCountCache[label] ?? 10

  return (
    <Card size={compact ? 'sm' : 'default'} className="text-left">
      <CardHeader className="border-b">
        <CardTitle className="flex items-center justify-between gap-2">
          <span>{label}</span>
          {loading ? (
            <Skeleton className="h-5 w-16 rounded-full" />
          ) : !available ? (
            <Badge variant="outline" className="font-normal">unavailable</Badge>
          ) : null}
        </CardTitle>
        {loading ? (
          <Skeleton className="mt-1 h-3 w-32" />
        ) : (
          deployedAgo && (
            <p className="text-xs text-muted-foreground" title={deployedAt ?? undefined}>
              Built {deployedAgo}
            </p>
          )
        )}
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="flex flex-col">
            {Array.from({ length: skeletonRows }).map((_, index) => (
              <div key={index}>
                {index > 0 && <Separator className="my-1.5" />}
                <div className="flex items-center justify-between gap-4 py-0.5">
                  <Skeleton className="h-3 w-24" />
                  <Skeleton className="h-3 w-28" />
                </div>
              </div>
            ))}
          </div>
        ) : available ? (
          <dl className="flex flex-col">
            {rows.map((row, index) => (
              <div key={row.key}>
                {index > 0 && <Separator className="my-1.5" />}
                <div className="flex items-baseline justify-between gap-4">
                  <dt className="shrink-0 text-xs text-muted-foreground">{row.label}</dt>
                  <dd className="min-w-0 break-all text-right text-xs font-medium tabular-nums">
                    <InfoValueCell row={row} />
                  </dd>
                </div>
              </div>
            ))}
          </dl>
        ) : (
          <p className="text-xs text-muted-foreground">Information unavailable.</p>
        )}
      </CardContent>
    </Card>
  )
}

function InfoValueCell({ row }: { row: InfoRow }) {
  if (!row.href) {
    return <>{row.value}</>
  }
  return (
    <a
      href={row.href}
      target="_blank"
      rel="noopener noreferrer"
      className="text-primary underline decoration-dotted underline-offset-2 hover:decoration-solid"
      title="View commit on GitHub"
    >
      {row.value}
    </a>
  )
}
