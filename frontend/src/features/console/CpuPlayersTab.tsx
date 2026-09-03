'use client'

import { useCallback, useEffect, useRef, useState } from 'react'
import { DatabaseZap, ExternalLink, Loader2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { DataTab, type Column } from './DataTab'
import { consoleApi } from './api'
import { formatTime } from './format'
import type { CpuMatchStatus, CpuPlayer, CpuPlayerSummary } from './types'

const STATUS_VARIANT: Record<CpuMatchStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  EXACT: 'default',
  MODEL: 'secondary',
  AMBIGUOUS: 'destructive',
  UNMATCHED: 'outline',
  NO_CPU: 'outline',
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
    key: 'observedAt',
    label: 'Observed',
    sortKey: 'observedAt',
    initialSortDirection: 'desc',
    value: (player) => player.observedAt,
    render: (player) => formatTime(player.observedAt),
    cellClassName: 'whitespace-nowrap text-muted-foreground tabular-nums',
    headClassName: 'w-[170px]',
    defaultVisible: false,
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
  canRefresh,
  forceLoading,
  onError,
}: {
  active?: boolean
  canRefresh: boolean
  forceLoading: boolean
  onError: (message: string) => void
}) {
  const [summary, setSummary] = useState<CpuPlayerSummary | null>(null)
  const [refreshingCatalog, setRefreshingCatalog] = useState(false)
  const [tableRefreshToken, setTableRefreshToken] = useState(0)
  const wasActive = useRef(false)

  const loadSummary = useCallback(async () => {
    try {
      setSummary(await consoleApi.getCpuPlayerSummary())
    } catch {
      onError('Failed to load CPU rating summary.')
    }
  }, [onError])

  useEffect(() => {
    if (forceLoading || !active) {
      wasActive.current = false
      return
    }
    if (!wasActive.current) void loadSummary()
    wasActive.current = true
  }, [active, forceLoading, loadSummary])

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
        <Summary summary={forceLoading ? null : summary} />
        {canRefresh && (
          <Button variant="outline" size="sm" disabled={refreshingCatalog} onClick={refreshCatalog}>
            {refreshingCatalog ? <Loader2 className="animate-spin" /> : <DatabaseZap />}
            Refresh benchmarks
          </Button>
        )}
      </div>
      <DataTab<CpuPlayer>
        active={active}
        refreshToken={tableRefreshToken}
        columns={PLAYER_COLUMNS}
        defaultSortKey="score"
        defaultSortDirection="desc"
        columnSelection
        rowKey={(player) => player.playerId}
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

function Summary({ summary }: { summary: CpuPlayerSummary | null }) {
  if (!summary) {
    return <div className="flex flex-wrap gap-4">{Array.from({ length: 4 }).map((_, index) => <Skeleton key={index} className="h-9 w-28" />)}</div>
  }
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

function Stat({ label, value }: { label: string; value: string }) {
  return <div><dt className="text-muted-foreground">{label}</dt><dd className="mt-0.5 font-medium tabular-nums">{value}</dd></div>
}