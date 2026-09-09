'use client'

import { useCallback, useEffect, useRef, useState } from 'react'
import { Clapperboard, DatabaseZap, ExternalLink, Loader2, RefreshCw, RotateCw, X } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import { DataTab, type Column } from './DataTab'
import { consoleApi } from './api'
import { formatTime } from './format'
import { formatRelativeAge } from '@/lib/appInfo'
import type { CpuMatchStatus, CpuPlayer, CpuPlayerSummary, DiscordUser, ReplayRescanDashboard } from './types'

const STATUS_VARIANT: Record<CpuMatchStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  EXACT: 'default',
  MODEL: 'secondary',
  AMBIGUOUS: 'destructive',
  UNMATCHED: 'outline',
  NO_CPU: 'outline',
}

function cooldownEnd(dashboard: ReplayRescanDashboard, playerId: string): number | null {
  const latest = dashboard.history.find((item) => item.targetPlayerId === playerId)
  return latest
    ? Date.parse(latest.requestedAt) + dashboard.targetCooldownSeconds * 1000
    : null
}

function cooldownTitle(remainingMs: number): string {
  const seconds = Math.max(1, Math.ceil(remainingMs / 1000))
  if (seconds < 60) return `Available again in ${seconds} seconds`
  const minutes = Math.ceil(seconds / 60)
  return `Available again in ${minutes} minute${minutes === 1 ? '' : 's'}`
}

function DiscordIdentity({ user }: { user: DiscordUser }) {
  const initials = user.displayName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()

  return (
    <span className="flex min-w-0 items-center gap-2" title={`Discord: ${user.displayName}`}>
      <span
        aria-hidden="true"
        className="flex size-6 shrink-0 items-center justify-center rounded-full bg-muted bg-cover bg-center text-[9px] font-semibold text-muted-foreground ring-1 ring-border"
        style={user.pictureUrl ? { backgroundImage: `url(${JSON.stringify(user.pictureUrl)})` } : undefined}
      >
        {initials}
      </span>
      <span className="truncate">{user.displayName}</span>
    </span>
  )
}

function playerColumns(onNavigateToPlayer: (playerId: string) => void): Column<CpuPlayer>[] {
  return [
  {
    key: 'mainName',
    label: 'Main name',
    sortKey: 'mainName',
    value: (player) => player.mainName,
    render: (player) => (
      <a
        href={`?tab=players&player=${encodeURIComponent(player.playerId)}`}
        className="text-primary hover:underline"
        onClick={(event) => {
          if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
          event.preventDefault()
          onNavigateToPlayer(player.playerId)
        }}
      >
        {player.mainName}
      </a>
    ),
    cellClassName: 'truncate font-medium',
    defaultWidth: 122,
    searchKey: 'mainName',
  },
  {
    key: 'discordUser',
    label: 'Discord',
    sortKey: 'discordUser',
    value: (player) => player.discordUser?.displayName ?? '',
    render: (player) => player.discordUser
      ? <DiscordIdentity user={player.discordUser} />
      : <span className="text-muted-foreground">—</span>,
    cellClassName: 'truncate',
    defaultWidth: 147,
  },
  {
    key: 'playerId',
    label: 'ID',
    sortKey: 'playerId',
    value: (player) => player.playerId,
    render: (player) => <span className="font-mono text-xs">{player.playerId}</span>,
    cellClassName: 'truncate',
    defaultWidth: 110,
    searchKey: 'playerId',
  },
  {
    key: 'aliases',
    label: 'Aliases',
    sortKey: 'aliases',
    value: (player) => player.aliases.join(', '),
    render: (player) => player.aliases.length ? player.aliases.join(', ') : '—',
    cellClassName: 'truncate text-muted-foreground',
    defaultWidth: 155,
    searchKey: 'aliases',
  },
  {
    key: 'replayCount',
    label: 'Games',
    sortKey: 'replayCount',
    initialSortDirection: 'desc',
    value: (player) => player.replayCount.toString(),
    render: (player) => player.replayCount.toLocaleString(),
    cellClassName: 'font-mono tabular-nums',
    defaultWidth: 81,
    searchKey: 'replayCount',
  },
  {
    key: 'reportedCpu',
    label: 'CPU',
    sortKey: 'reportedCpu',
    value: (player) => player.reportedCpu ?? '',
    render: (player) => player.reportedCpu ?? 'Not reported',
    cellClassName: 'truncate',
    defaultWidth: 256,
    searchKey: 'reportedCpu',
  },
  {
    key: 'score',
    label: 'Thread Mark',
    sortKey: 'score',
    initialSortDirection: 'desc',
    value: (player) => player.singleThreadScore?.toString() ?? '',
    render: (player) => player.singleThreadScore?.toLocaleString() ?? '—',
    cellClassName: 'font-mono font-medium tabular-nums',
    defaultWidth: 119,
    searchKey: 'score',
  },
  {
    key: 'latestName',
    label: 'Latest name',
    sortKey: 'latestName',
    value: (player) => player.latestName,
    cellClassName: 'truncate',
    defaultVisible: false,
    searchKey: 'latestName',
  },
  {
    key: 'benchmark',
    label: 'PassMark match',
    sortKey: 'benchmark',
    value: (player) => player.benchmarkModel ?? '',
    render: (player) => player.benchmarkUrl ? (
      <a
        href={player.benchmarkUrl}
        target="_blank"
        rel="noreferrer"
        className="inline-flex max-w-full items-center gap-1 text-primary hover:underline"
      >
        <span className="truncate">{player.benchmarkModel}</span><ExternalLink className="size-3" />
      </a>
    ) : '—',
    cellClassName: 'truncate',
    searchKey: 'benchmark',
    defaultVisible: false,
  },
  {
    key: 'status',
    label: 'Match',
    sortKey: 'status',
    value: (player) => player.matchStatus,
    render: (player) => <Badge variant={STATUS_VARIANT[player.matchStatus]}>{player.matchStatus.replace('_', ' ')}</Badge>,
    headClassName: 'w-[120px]',
    searchKey: 'status',
    defaultVisible: false,
  },
  {
    key: 'gentoolUpdatedAt',
    label: 'GenTool update',
    sortKey: 'gentoolUpdatedAt',
    initialSortDirection: 'desc',
    value: (player) => player.gentoolUpdatedAt ?? '',
    render: (player) => player.gentoolUpdatedAt ? (
      <span title={formatTime(player.gentoolUpdatedAt)}>
        {formatRelativeAge(player.gentoolUpdatedAt) ?? formatTime(player.gentoolUpdatedAt)}
      </span>
    ) : 'Never',
    cellClassName: 'whitespace-nowrap text-muted-foreground tabular-nums',
    defaultWidth: 129,
    searchKey: 'gentoolUpdatedAt',
  },
  {
    key: 'scoreUpdatedAt',
    label: 'Score updated',
    sortKey: 'scoreUpdatedAt',
    initialSortDirection: 'desc',
    value: (player) => player.scoreUpdatedAt ?? '',
    render: (player) => player.scoreUpdatedAt ? formatTime(player.scoreUpdatedAt) : '—',
    cellClassName: 'whitespace-nowrap text-muted-foreground tabular-nums',
    headClassName: 'w-[170px]',
    defaultVisible: false,
    searchKey: 'scoreUpdatedAt',
  },
  ]
}

export function CpuPlayersTab({
  active = true,
  canManage,
  canRefresh,
  targetPlayerId,
  onClearPlayer,
  onNavigateToPlayer,
  onNavigateToReplays,
  forceLoading,
  onError,
}: {
  active?: boolean
  canManage: boolean
  canRefresh: boolean
  targetPlayerId: string | null
  onClearPlayer: () => void
  onNavigateToPlayer: (playerId: string) => void
  onNavigateToReplays: (playerId: string) => void
  forceLoading: boolean
  onError: (message: string) => void
}) {
  const [summary, setSummary] = useState<CpuPlayerSummary | null>(null)
  const [summaryLoading, setSummaryLoading] = useState(true)
  const [summaryError, setSummaryError] = useState(false)
  const [rescanDashboard, setRescanDashboard] = useState<ReplayRescanDashboard | null>(null)
  const [busyActions, setBusyActions] = useState<Record<string, 'refresh'>>({})
  const [refreshingCatalog, setRefreshingCatalog] = useState(false)
  const [tableRefreshToken, setTableRefreshToken] = useState(0)
  const [linkedOnly, setLinkedOnly] = useState(false)
  const [cooldownClock, setCooldownClock] = useState(() => Date.now())
  const wasActive = useRef(false)
  const hadActiveRescan = useRef(false)

  const loadSummary = useCallback(async () => {
    setSummaryLoading(true)
    try {
      setSummary(await consoleApi.getCpuPlayerSummary())
      setSummaryError(false)
    } catch {
      setSummaryError(true)
    } finally {
      setSummaryLoading(false)
    }
  }, [])

  const loadRescanDashboard = useCallback(async () => {
    if (!canManage) return
    try {
      const next = await consoleApi.getReplayRescanDashboard()
      const activeRescan = next.history.some((item) => item.status === 'QUEUED' || item.status === 'RUNNING')
      if (hadActiveRescan.current && !activeRescan) {
        setTableRefreshToken((value) => value + 1)
        void loadSummary()
      }
      hadActiveRescan.current = activeRescan
      setCooldownClock(Date.now())
      setRescanDashboard(next)
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to load rescan status.')
    }
  }, [canManage, loadSummary, onError])

  useEffect(() => {
    if (forceLoading || !active) {
      wasActive.current = false
      return
    }
    if (!wasActive.current) {
      void loadSummary()
    }
    wasActive.current = true
  }, [active, forceLoading, loadRescanDashboard, loadSummary])

  useEffect(() => {
    if (active && canManage && !forceLoading) void loadRescanDashboard()
  }, [active, canManage, forceLoading, loadRescanDashboard])

  useEffect(() => {
    if (!active || !canManage || !hadActiveRescan.current) return
    const timer = window.setInterval(() => void loadRescanDashboard(), 3000)
    return () => window.clearInterval(timer)
  }, [active, canManage, loadRescanDashboard, rescanDashboard])

  useEffect(() => {
    if (!active || !rescanDashboard) return
    const now = Date.now()
    const nextExpiry = rescanDashboard.history
      .map((item) => Date.parse(item.requestedAt) + rescanDashboard.targetCooldownSeconds * 1000)
      .filter((expiry) => expiry > now)
      .sort((left, right) => left - right)[0]
    if (nextExpiry === undefined) return
    const timer = window.setTimeout(() => setCooldownClock(Date.now()), nextExpiry - now + 100)
    return () => window.clearTimeout(timer)
  }, [active, cooldownClock, rescanDashboard])

  const refreshPlayer = async (player: CpuPlayer) => {
    setBusyActions((current) => ({ ...current, [player.playerId]: 'refresh' }))
    try {
      await consoleApi.requestReplayRescan(player.playerId)
      hadActiveRescan.current = true
      await loadRescanDashboard()
    } catch (error) {
      await loadRescanDashboard()
      onError(error instanceof Error ? error.message : 'Failed to refresh player data.')
    } finally {
      setBusyActions((current) => {
        const next = { ...current }
        delete next[player.playerId]
        return next
      })
    }
  }

  const refreshCatalog = async () => {
    setRefreshingCatalog(true)
    try {
      await consoleApi.refreshCpuBenchmarks()
      await loadSummary()
      setTableRefreshToken((value) => value + 1)
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to refresh CPU benchmarks.')
    } finally {
      setRefreshingCatalog(false)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-3 border-b pb-4 sm:flex-row sm:items-center sm:justify-between">
        <Summary
          summary={forceLoading ? null : summary}
          loading={forceLoading || summaryLoading}
          error={summaryError}
          onRetry={() => void loadSummary()}
        />
        {canRefresh && (
          <Button variant="outline" size="sm" disabled={refreshingCatalog} onClick={refreshCatalog}>
            {refreshingCatalog ? <Loader2 className="animate-spin" /> : <DatabaseZap />}
            Refresh benchmarks
          </Button>
        )}
      </div>
      {canManage && rescanDashboard && (
        <PlayerRescanStatus dashboard={rescanDashboard} />
      )}
      <DataTab<CpuPlayer>
        active={active}
        refreshToken={tableRefreshToken}
        filterKey={`${linkedOnly}:${targetPlayerId ?? ''}`}
        toolbarFilters={(
          <>
            {targetPlayerId && (
              <Badge variant="outline" className="h-7 gap-1 pl-2 font-mono font-normal">
                Player: {targetPlayerId}
                <Button
                  variant="ghost"
                  size="icon-xs"
                  className="-mr-1"
                  aria-label="Show all players"
                  title="Show all players"
                  onClick={onClearPlayer}
                >
                  <X />
                </Button>
              </Badge>
            )}
            <label className="flex cursor-pointer items-center gap-2 text-xs">
              <Switch checked={linkedOnly} onCheckedChange={setLinkedOnly} />
              Linked users only
            </label>
          </>
        )}
        columns={playerColumns(onNavigateToPlayer)}
        columnWidthsKey="players"
        defaultSortKey="score"
        defaultSortDirection="desc"
        columnSelection
        rowKey={(player) => player.playerId}
        rowActions={(player) => {
          const busyAction = busyActions[player.playerId]
          const linked = rescanDashboard?.link?.playerId === player.playerId
          const refreshInProgress = rescanDashboard?.history.some(
            (item) => item.targetPlayerId === player.playerId
              && (item.status === 'QUEUED' || item.status === 'RUNNING'),
          ) === true
          const cooldownRemaining = rescanDashboard
            ? Math.max(0, (cooldownEnd(rescanDashboard, player.playerId) ?? 0) - cooldownClock)
            : 0
          const otherQuotaReached = !linked
            && Boolean(rescanDashboard)
            && rescanDashboard!.otherUsedToday >= rescanDashboard!.otherDailyLimit
          const refreshTitle = refreshInProgress
            ? 'GenTool update in progress'
            : otherQuotaReached
            ? 'Daily other-player refresh quota reached'
            : cooldownRemaining > 0
              ? cooldownTitle(cooldownRemaining)
              : 'Refresh player data'
          return (
            <>
              <Button
                variant="ghost"
                size="icon-sm"
                className="text-muted-foreground hover:text-foreground"
                nativeButton={false}
                render={<a href={`?tab=replays&player=${encodeURIComponent(player.playerId)}`} />}
                aria-label={`View replays for ${player.mainName}`}
                title="View player replays"
                onClick={(event) => {
                  if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
                  event.preventDefault()
                  onNavigateToReplays(player.playerId)
                }}
              >
                <Clapperboard />
              </Button>
              {canManage && (
                <Button
                  variant="ghost"
                  size="icon-sm"
                  className="text-muted-foreground hover:text-foreground"
                  disabled={busyAction !== undefined || !rescanDashboard || refreshInProgress || otherQuotaReached || cooldownRemaining > 0}
                  aria-label={refreshInProgress ? `Refreshing ${player.mainName}` : `Refresh ${player.mainName}`}
                  title={refreshTitle}
                  onClick={() => void refreshPlayer(player)}
                >
                  {refreshInProgress || busyAction === 'refresh'
                    ? <Loader2 className="animate-spin" />
                    : <RefreshCw />}
                </Button>
              )}
            </>
          )
        }}
        load={(page, size, q, field, sort, direction) => consoleApi.listCpuPlayers(
          page, size, q, field, sort, direction, linkedOnly, targetPlayerId ?? undefined,
        )}
        emptyLabel="No player hardware collected."
        sortLabel="single-thread score"
        sortDescendingLabel="Highest"
        sortAscendingLabel="Lowest"
        forceLoading={forceLoading}
        onError={onError}
      />
    </div>
  )
}

function Summary({
  summary,
  loading,
  error,
  onRetry,
}: {
  summary: CpuPlayerSummary | null
  loading: boolean
  error: boolean
  onRetry: () => void
}) {
  if (summary) {
    const coverage = summary.totalPlayers ? Math.round((summary.ratedPlayers / summary.totalPlayers) * 100) : 0
    return (
      <dl className="flex flex-wrap gap-x-6 gap-y-2 text-xs">
        <Stat label="Rated" value={`${summary.ratedPlayers.toLocaleString()} / ${summary.totalPlayers.toLocaleString()} (${coverage}%)`} />
        <Stat label="Unmatched" value={summary.unmatchedPlayers.toLocaleString()} />
        <Stat label="Ambiguous" value={summary.ambiguousPlayers.toLocaleString()} />
        <Stat label="No CPU" value={summary.playersWithoutCpu.toLocaleString()} />
        <Stat label="Catalog" value={summary.catalogEntries ? `${summary.catalogEntries.toLocaleString()} · ${summary.catalogFetchedAt ? formatTime(summary.catalogFetchedAt) : ''}` : 'Not loaded'} />
        <Stat label="Data since" value={summary.dataSince ? formatTime(summary.dataSince) : 'No data yet'} />
      </dl>
    )
  }
  if (error && !loading) {
    return (
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <span>CPU summary unavailable.</span>
        <Button variant="ghost" size="xs" onClick={onRetry}>
          <RotateCw />
          Retry
        </Button>
      </div>
    )
  }
  if (loading) {
    return <div className="flex flex-wrap gap-4">{Array.from({ length: 4 }).map((_, index) => <Skeleton key={index} className="h-9 w-28" />)}</div>
  }
  return null
}

function Stat({ label, value }: { label: string; value: string }) {
  return <div><dt className="text-muted-foreground">{label}</dt><dd className="mt-0.5 font-medium tabular-nums">{value}</dd></div>
}

function PlayerRescanStatus({ dashboard }: { dashboard: ReplayRescanDashboard }) {
  const remaining = Math.max(0, dashboard.otherDailyLimit - dashboard.otherUsedToday)
  const latest = dashboard.history[0]
  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
      <span>{remaining} of {dashboard.otherDailyLimit} other-player refreshes left</span>
      {latest && <Badge variant="outline" title={latest.errorMessage ?? undefined}>Latest: {latest.status}</Badge>}
    </div>
  )
}