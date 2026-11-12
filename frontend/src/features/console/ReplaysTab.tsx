'use client'

import { useEffect, useState } from 'react'
import { Check, Copy, ExternalLink, X } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Switch } from '@/components/ui/switch'
import { DataTab, type Column } from './DataTab'
import { consoleApi } from './api'
import { DatePeriodFilter, type DatePeriod } from './DatePeriodFilter'
import { formatTime } from './format'
import type { Replay, ReplayAssociatedFile } from './types'

function formatDuration(seconds: number | null): string {
  if (seconds === null) return '—'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainder = seconds % 60
  return [hours, minutes, remainder].map((part) => String(part).padStart(2, '0')).join(':')
}

function formatBytes(bytes: number | null): string {
  if (bytes === null) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function playerMatchup(replay: Replay): string {
  const teams = Map.groupBy(
    replay.players.toSorted((left, right) => left.teamNumber - right.teamNumber || left.slotNumber - right.slotNumber),
    (player) => player.teamNumber,
  )
  return [...teams.values()]
    .map((players) => players.map((player) => player.name).join(' + '))
    .join(' vs ')
}

const REPLAY_COLUMNS: Column<Replay>[] = [
  {
    key: 'matchAt',
    label: 'Match time',
    sortKey: 'matchAt',
    initialSortDirection: 'desc',
    value: (replay) => replay.matchAt,
    render: (replay) => formatTime(replay.matchAt),
    cellClassName: 'whitespace-nowrap text-muted-foreground tabular-nums',
    defaultWidth: 187,
    searchKey: 'matchAt',
  },
  {
    key: 'reporter',
    mobilePrimary: true,
    label: 'Reporter',
    sortKey: 'reporter',
    value: (replay) => replay.reporterName,
    render: (replay) => (
      <div className="min-w-0">
        <div className="truncate font-medium">{replay.reporterName}</div>
        <div className="truncate font-mono text-[11px] text-muted-foreground">{replay.reporterId}</div>
      </div>
    ),
    cellClassName: 'truncate',
    defaultWidth: 153,
    searchKey: 'reporter',
  },
  {
    key: 'players',
    mobileContent: true,
    label: 'Players',
    sortKey: 'players',
    value: playerMatchup,
    cellClassName: 'truncate',
    defaultWidth: 363,
    searchKey: 'players',
  },
  {
    key: 'mapName',
    label: 'Map',
    sortKey: 'mapName',
    value: (replay) => replay.mapName ?? '',
    render: (replay) => replay.mapName ?? '—',
    cellClassName: 'truncate',
    defaultWidth: 218,
    searchKey: 'mapName',
  },
  {
    key: 'matchType',
    mobileCompact: true,
    label: 'Type',
    sortKey: 'matchType',
    value: (replay) => replay.matchType ?? '',
    render: (replay) => replay.matchType ? <Badge variant="outline">{replay.matchType}</Badge> : '—',
    defaultWidth: 88,
    searchKey: 'matchType',
  },
  {
    key: 'duration',
    mobileCompact: true,
    label: 'Duration',
    sortKey: 'duration',
    initialSortDirection: 'desc',
    value: (replay) => formatDuration(replay.matchLengthSeconds),
    cellClassName: 'font-mono text-xs tabular-nums',
    defaultWidth: 149,
    searchKey: 'duration',
  },
  {
    key: 'cpu',
    label: 'CPU',
    sortKey: 'cpu',
    value: (replay) => replay.cpu ?? '',
    render: (replay) => replay.cpu ?? '—',
    cellClassName: 'truncate',
    searchKey: 'cpu',
    defaultVisible: false,
  },
  {
    key: 'matchMode',
    mobileCompact: true,
    label: 'Mode',
    sortKey: 'matchMode',
    value: (replay) => replay.matchMode ?? '',
    defaultVisible: false,
    searchKey: 'matchMode',
  },
  {
    key: 'startCash',
    mobileCompact: true,
    label: 'Start cash',
    sortKey: 'startCash',
    initialSortDirection: 'desc',
    value: (replay) => replay.startCash?.toLocaleString() ?? '',
    defaultVisible: false,
    searchKey: 'startCash',
  },
  {
    key: 'gentoolVersion',
    mobileCompact: true,
    label: 'GenTool',
    sortKey: 'gentoolVersion',
    value: (replay) => replay.gentoolVersion ?? '',
    defaultVisible: false,
    searchKey: 'gentoolVersion',
  },
  {
    key: 'gameVersion',
    label: 'Game version',
    sortKey: 'gameVersion',
    value: (replay) => replay.gameVersion ?? '',
    cellClassName: 'truncate',
    defaultVisible: false,
    searchKey: 'gameVersion',
  },
  {
    key: 'windowsCompat',
    label: 'Windows',
    sortKey: 'windowsCompat',
    value: (replay) => replay.windowsCompat ?? '',
    defaultVisible: false,
    searchKey: 'windowsCompat',
  },
  {
    key: 'repInfoInUse',
    mobileCompact: true,
    label: 'RepInfo',
    sortKey: 'repInfoInUse',
    value: (replay) => replay.repInfoInUse ?? '',
    defaultVisible: false,
    searchKey: 'repInfoInUse',
  },
  {
    key: 'replaySize',
    mobileCompact: true,
    label: 'Replay size',
    sortKey: 'replaySize',
    initialSortDirection: 'desc',
    value: (replay) => formatBytes(replay.replaySizeBytes),
    defaultVisible: false,
    searchKey: 'replaySize',
  },
  {
    key: 'sourceDate',
    label: 'Source date',
    sortKey: 'sourceDate',
    initialSortDirection: 'desc',
    value: (replay) => replay.sourceDate,
    defaultVisible: false,
    searchKey: 'sourceDate',
  },
  {
    key: 'collectedAt',
    label: 'Collected',
    sortKey: 'collectedAt',
    initialSortDirection: 'desc',
    value: (replay) => replay.collectedAt,
    render: (replay) => formatTime(replay.collectedAt),
    defaultVisible: false,
    searchKey: 'collectedAt',
  },
]

export function ReplaysTab({
  active = true,
  canUseMyReplays,
  targetPlayerId,
  targetReplayId,
  onClearPlayer,
  onClearReplay,
  forceLoading,
  onError,
}: {
  active?: boolean
  canUseMyReplays: boolean
  targetPlayerId: string | null
  targetReplayId: string | null
  onClearPlayer: () => void
  onClearReplay: () => void
  forceLoading: boolean
  onError: (message: string) => void
}) {
  const [linkedPlayerIds, setLinkedPlayerIds] = useState<string[]>([])
  const [myReplaysOnly, setMyReplaysOnly] = useState(false)
  const [period, setPeriod] = useState<DatePeriod>({ startDate: '', endDate: '' })
  const [copiedReplayId, setCopiedReplayId] = useState<string | null>(null)

  useEffect(() => {
    if (!active || !canUseMyReplays || forceLoading) return
    let current = true
    consoleApi.getReplayRescanDashboard()
      .then((dashboard) => {
        if (!current) return
        const playerIds = (dashboard.links ?? [])
          .filter((link) => link.status === 'APPROVED')
          .map((link) => link.playerId)
        setLinkedPlayerIds(playerIds)
        if (playerIds.length === 0) setMyReplaysOnly(false)
      })
      .catch(() => onError('Failed to load linked GenTool account.'))
    return () => {
      current = false
    }
  }, [active, canUseMyReplays, forceLoading, onError])

  useEffect(() => {
    if (!copiedReplayId) return
    const timer = window.setTimeout(() => setCopiedReplayId(null), 2000)
    return () => window.clearTimeout(timer)
  }, [copiedReplayId])

  const copyReplayLink = async (replay: Replay) => {
    const url = new URL(window.location.href)
    url.searchParams.set('tab', 'replays')
    url.searchParams.set('replay', replay.id)
    url.searchParams.delete('player')
    try {
      await navigator.clipboard.writeText(url.toString())
      setCopiedReplayId(replay.id)
    } catch {
      onError('Failed to copy replay link.')
    }
  }

  return (
    <div className="flex flex-col gap-3">
    <DatePeriodFilter value={period} onChange={setPeriod} active={active} forceLoading={forceLoading} onError={onError} />
    <DataTab<Replay>
      columns={REPLAY_COLUMNS}
      columnWidthsKey="replays"
      defaultSortKey="matchAt"
      defaultSearchField="reporter"
      defaultSortDirection="desc"
      active={active}
      filterKey={`${targetReplayId ?? targetPlayerId ?? (myReplaysOnly ? linkedPlayerIds.join(',') : '')}:${period.startDate}:${period.endDate}`}
      toolbarFilters={(
        <>
          {targetReplayId && (
            <Badge variant="outline" className="h-auto max-w-full gap-1 py-0 pl-2 font-mono font-normal sm:h-7">
              <span className="min-w-0 truncate" title={`Replay: ${targetReplayId}`}>Replay: {targetReplayId}</span>
              <Button
                variant="ghost"
                size="icon-xs"
                className="-mr-1"
                aria-label="Show all replays"
                title="Show all replays"
                onClick={onClearReplay}
              >
                <X />
              </Button>
            </Badge>
          )}
          {!targetReplayId && targetPlayerId && (
            <Badge variant="outline" className="h-7 gap-1 pl-2 font-mono font-normal">
              Player: {targetPlayerId}
              <Button
                variant="ghost"
                size="icon-xs"
                className="-mr-1"
                aria-label="Show all replays"
                title="Show all replays"
                onClick={() => {
                  setMyReplaysOnly(false)
                  onClearPlayer()
                }}
              >
                <X />
              </Button>
            </Badge>
          )}
          {!targetReplayId && !targetPlayerId && linkedPlayerIds.length > 0 && (
            <label className="flex cursor-pointer items-center gap-2 text-xs">
              <Switch checked={myReplaysOnly} onCheckedChange={setMyReplaysOnly} />
              My replays
            </label>
          )}
        </>
      )}
      columnSelection
      rowKey={(replay) => replay.id}
      rowActions={(replay) => (
        <Button
          variant="ghost"
          size="icon-sm"
          className="text-muted-foreground hover:text-foreground"
          aria-label={copiedReplayId === replay.id ? 'Replay link copied' : 'Copy replay link'}
          title={copiedReplayId === replay.id ? 'Copied' : 'Copy replay link'}
          onClick={() => void copyReplayLink(replay)}
        >
          {copiedReplayId === replay.id ? <Check className="text-primary" /> : <Copy />}
        </Button>
      )}
      load={(page, size, q, field, sort, direction, exact) => consoleApi.listReplays(
        page,
        size,
        q,
        field,
        sort,
        direction,
        exact,
        targetReplayId ? undefined : targetPlayerId ? [targetPlayerId] : myReplaysOnly ? linkedPlayerIds : undefined,
        targetReplayId ?? undefined,
        period.startDate,
        period.endDate,
      )}
      expand={(replay) => <ReplayDetails replay={replay} />}
      initialExpandedId={targetReplayId}
      emptyLabel={targetReplayId ? 'Replay not found.' : 'No replays collected.'}
      sortLabel="match time"
      forceLoading={forceLoading}
      onError={onError}
    />
    </div>
  )
}

const COMPACT_REPLAY_FIELDS = new Set([
  'GenTool Version', 'RepInfo in use', 'Start Cash', 'Match Type', 'Match Length', 'Match Mode',
])

function replayFieldSize(label: string, value: string): 'normal' | 'compact' | 'wide' {
  if (value.includes('\n') || value.length > 72) return 'wide'
  return COMPACT_REPLAY_FIELDS.has(label) ? 'compact' : 'normal'
}

function ReplayDetails({ replay }: { replay: Replay }) {
  const teams = Map.groupBy(replay.players, (player) => player.teamNumber)
  const parsedFields = Object.entries(replay.fields)
    .filter(([label]) => label !== 'Associated files')
  const systemInFields = parsedFields.some(([label, value]) => label === 'System' && value === replay.systemInfo)
  const firstCompactField = parsedFields.find(([label, value]) => replayFieldSize(label, value) === 'compact')?.[0]

  return (
    <div className="replay-details grid min-w-0 gap-2 text-sm lg:gap-5 lg:bg-muted/20 lg:p-4">
      <div className="grid gap-2 lg:grid-cols-3 lg:gap-4">
        <section className="grid min-w-0 content-start gap-1">
          <h3 className="font-medium">Teams</h3>
          <div className="grid grid-cols-2 gap-x-3 gap-y-1.5 lg:grid-cols-1">
            {[...teams.entries()].map(([team, players]) => (
              <div key={team} className="grid min-w-0 content-start gap-0.5 lg:grid-cols-[4rem_minmax(0,1fr)] lg:gap-2 lg:border-t lg:pt-2 first:lg:border-0 first:lg:pt-0">
                <h4 className="text-xs text-muted-foreground">Team {team}</h4>
                <div className="grid min-w-0 gap-1">
                  {players.map((player) => (
                    <div key={`${player.teamNumber}-${player.slotNumber}`} className="grid min-w-0 grid-cols-[minmax(0,1fr)_auto] items-baseline gap-x-1">
                      <span className="wrap-anywhere font-medium">{player.name}</span>
                      <span className="max-w-24 wrap-anywhere text-right text-xs text-muted-foreground">{player.army ?? 'Unknown army'}</span>
                      <span className="col-span-2 font-mono text-[11px] text-muted-foreground">{player.address}</span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="replay-system-summary grid min-w-0 content-start gap-1 lg:col-span-2" data-duplicated={systemInFields || undefined}>
          <h3 className="font-medium">System</h3>
          <pre className="whitespace-pre-wrap wrap-anywhere font-mono text-xs text-muted-foreground">{replay.systemInfo ?? 'Not reported'}</pre>
        </section>
      </div>

      <section className="grid min-w-0 gap-1">
        <h3 className="font-medium">All parsed fields</h3>
        <dl className="replay-fields grid grid-cols-12 gap-x-3 gap-y-1.5 lg:grid-cols-[max-content_minmax(0,1fr)] lg:gap-y-2">
          {parsedFields.map(([label, value]) => (
            <div key={label} className="min-w-0 lg:contents"
              data-field-size={replayFieldSize(label, value)} data-group-start={label === firstCompactField || undefined}>
              <dt className="text-muted-foreground">{label}</dt>
              <dd className="min-w-0 whitespace-pre-wrap wrap-anywhere">{value || '\u2014'}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section className="grid min-w-0 gap-1">
        <h3 className="font-medium">Associated files</h3>
        <div className="grid min-w-0 gap-x-4 lg:grid-cols-2">
          {replay.associatedFiles.map((file) => (
            <a key={file.name} href={associatedFileUrl(replay.sourceUrl, file)} target="_blank" rel="noreferrer"
              className="replay-file flex min-w-0 items-center gap-2 text-primary hover:underline">
              <span className="min-w-0 flex-1 wrap-anywhere">{file.name}</span>
              <span className="shrink-0 text-xs text-muted-foreground">{formatBytes(file.sizeBytes)}</span>
              <ExternalLink className="size-3 shrink-0" />
            </a>
          ))}
        </div>
      </section>

      <div className="grid min-w-0 grid-cols-[minmax(0,1fr)_auto] gap-x-2">
        <details className="col-start-1 col-end-3 row-start-1 min-w-0">
          <summary className="replay-raw-summary cursor-pointer pr-32 font-medium">Raw replay text</summary>
          <pre className="replay-raw-text mt-1 max-h-96 overflow-y-auto whitespace-pre-wrap wrap-anywhere rounded-md border bg-background p-3 font-mono text-xs">{replay.rawText}</pre>
        </details>
        <a href={replay.sourceUrl} target="_blank" rel="noreferrer"
          className="replay-source-link col-start-2 row-start-1 flex w-fit items-center gap-1 self-start text-primary hover:underline">
          Open source text <ExternalLink className="size-3 shrink-0" />
        </a>
      </div>
    </div>
  )
}

function associatedFileUrl(sourceUrl: string, file: ReplayAssociatedFile): string {
  return `${sourceUrl.slice(0, sourceUrl.lastIndexOf('/') + 1)}${encodeURIComponent(file.name)}`
}
