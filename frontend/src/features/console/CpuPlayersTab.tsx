'use client'

import { useCallback, useEffect, useRef, useState } from 'react'
import { DatabaseZap, ExternalLink, Link2, Loader2, RefreshCw, RotateCw } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { DataTab, type Column } from './DataTab'
import { consoleApi } from './api'
import { formatTime } from './format'
import { formatRelativeAge } from '@/lib/appInfo'
import type { CpuMatchStatus, CpuPlayer, CpuPlayerSummary, ReplayRescanDashboard } from './types'

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

const PLAYER_COLUMNS: Column<CpuPlayer>[] = [
  {
    key: 'mainName',
    label: 'Main name',
    sortKey: 'mainName',
    value: (player) => player.mainName,
    cellClassName: 'truncate font-medium',
    searchKey: 'player',
  },
  {
    key: 'playerId',
    label: 'ID',
    sortKey: 'playerId',
    value: (player) => player.playerId,
    render: (player) => <span className="font-mono text-xs">{player.playerId}</span>,
    cellClassName: 'truncate',
  },
  {
    key: 'aliases',
    label: 'Aliases',
    sortKey: 'aliases',
    value: (player) => player.aliases.join(', '),
    render: (player) => player.aliases.length ? player.aliases.join(', ') : '—',
    cellClassName: 'truncate text-muted-foreground',
  },
  {
    key: 'replayCount',
    label: 'Games',
    sortKey: 'replayCount',
    initialSortDirection: 'desc',
    value: (player) => player.replayCount.toString(),
    render: (player) => player.replayCount.toLocaleString(),
    cellClassName: 'font-mono tabular-nums',
    headClassName: 'w-[80px]',
  },
  {
    key: 'reportedCpu',
    label: 'CPU',
    sortKey: 'reportedCpu',
    value: (player) => player.reportedCpu ?? '',
    render: (player) => player.reportedCpu ?? 'Not reported',
    cellClassName: 'truncate',
    searchKey: 'cpu',
  },
  {
    key: 'score',
    label: 'Thread Mark',
    sortKey: 'score',
    initialSortDirection: 'desc',
    value: (player) => player.singleThreadScore?.toString() ?? '',
    render: (player) => player.singleThreadScore?.toLocaleString() ?? '—',
    cellClassName: 'font-mono font-medium tabular-nums',
    headClassName: 'w-[120px]',
  },
  {
    key: 'latestName',
    label: 'Latest name',
    sortKey: 'latestName',
    value: (player) => player.latestName,
    cellClassName: 'truncate',
    defaultVisible: false,
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
    key: 'gentoolRefreshedAt',
    label: 'Refreshed',
    sortKey: 'gentoolRefreshedAt',
    initialSortDirection: 'desc',
    value: (player) => player.gentoolRefreshedAt ?? '',
    render: (player) => player.gentoolRefreshedAt ? (
      <span title={formatTime(player.gentoolRefreshedAt)}>
        {formatRelativeAge(player.gentoolRefreshedAt) ?? formatTime(player.gentoolRefreshedAt)}
      </span>
    ) : 'Never',
    cellClassName: 'whitespace-nowrap text-muted-foreground tabular-nums',
    headClassName: 'w-[170px]',
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
  },
]

export function CpuPlayersTab({
  active = true,
  canManage,
  canRefresh,
  forceLoading,
  onError,
}: {
  active?: boolean
  canManage: boolean
  canRefresh: boolean
  forceLoading: boolean
  onError: (message: string) => void
}) {
  const [summary, setSummary] = useState<CpuPlayerSummary | null>(null)
  const [summaryLoading, setSummaryLoading] = useState(true)
  const [summaryError, setSummaryError] = useState(false)
  const [rescanDashboard, setRescanDashboard] = useState<ReplayRescanDashboard | null>(null)
  const [busyActions, setBusyActions] = useState<Record<string, 'claim' | 'refresh'>>({})
  const [refreshingCatalog, setRefreshingCatalog] = useState(false)
  const [tableRefreshToken, setTableRefreshToken] = useState(0)
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

  const claimPlayer = async (player: CpuPlayer) => {
    const existing = rescanDashboard?.link
    if (existing && existing.playerId !== player.playerId) {
      if (!confirm(`Replace your linked player ${existing.playerName ?? existing.playerId} with ${player.mainName}?`)) return
    }
    setBusyActions((current) => ({ ...current, [player.playerId]: 'claim' }))
    try {
      await consoleApi.claimGentoolPlayer(player.playerId)
      await loadRescanDashboard()
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to claim player.')
    } finally {
      setBusyActions((current) => {
        const next = { ...current }
        delete next[player.playerId]
        return next
      })
    }
  }

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
        columns={PLAYER_COLUMNS}
        columnWidthsKey="players"
        defaultSortKey="score"
        defaultSortDirection="desc"
        columnSelection
        rowKey={(player) => player.playerId}
        rowActions={canManage ? (player) => {
          const busyAction = busyActions[player.playerId]
          const linked = rescanDashboard?.link?.playerId === player.playerId
          const refreshInProgress = rescanDashboard?.history.some(
            (item) => item.targetPlayerId === player.playerId
              && (item.status === 'QUEUED' || item.status === 'RUNNING'),
          ) === true
          const cooldownRemaining = rescanDashboard
            ? Math.max(0, (cooldownEnd(rescanDashboard, player.playerId) ?? 0) - Math.max(cooldownClock, Date.now()))
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
                className={linked ? 'text-primary' : 'text-muted-foreground hover:text-foreground'}
                disabled={busyAction !== undefined || !rescanDashboard || linked}
                aria-label={linked ? 'Linked as my player' : rescanDashboard?.link ? 'Reclaim as my player' : 'Claim as my player'}
                title={linked ? 'Linked as my player' : rescanDashboard?.link ? 'Reclaim as my player' : 'Claim as my player'}
                onClick={() => void claimPlayer(player)}
              >
                {busyAction === 'claim'
                  ? <Loader2 className="animate-spin" />
                  : <Link2 />}
              </Button>
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
            </>
          )
        } : undefined}
        load={(page, size, q, field, sort, direction) => consoleApi.listCpuPlayers(page, size, q, field, sort, direction)}
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
      <span>
        Linked: <span className="font-medium text-foreground">{dashboard.link?.playerName ?? 'None'}</span>
        {dashboard.link && <span className="ml-1 font-mono">{dashboard.link.playerId}</span>}
      </span>
      <span>{remaining} of {dashboard.otherDailyLimit} other-player refreshes left</span>
      {latest && <Badge variant="outline">Latest: {latest.status}</Badge>}
    </div>
  )
}