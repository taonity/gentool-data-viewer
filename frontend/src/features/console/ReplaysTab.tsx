'use client'

import { useEffect, useState } from 'react'
import { Check, Copy, ExternalLink, X } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Switch } from '@/components/ui/switch'
import { DataTab, type Column } from './DataTab'
import { consoleApi } from './api'
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

function playerNames(replay: Replay): string {
  return replay.players.map((player) => player.name).join(', ')
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
    label: 'Players',
    sortKey: 'players',
    value: playerNames,
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
    defaultWidth: 275,
    searchKey: 'mapName',
  },
  {
    key: 'matchType',
    label: 'Type',
    sortKey: 'matchType',
    value: (replay) => replay.matchType ?? '',
    render: (replay) => replay.matchType ? <Badge variant="outline">{replay.matchType}</Badge> : '—',
    headClassName: 'w-[90px]',
    searchKey: 'matchType',
  },
  {
    key: 'duration',
    label: 'Duration',
    sortKey: 'duration',
    initialSortDirection: 'desc',
    value: (replay) => formatDuration(replay.matchLengthSeconds),
    cellClassName: 'font-mono text-xs tabular-nums',
    headClassName: 'w-[90px]',
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
    label: 'Mode',
    sortKey: 'matchMode',
    value: (replay) => replay.matchMode ?? '',
    defaultVisible: false,
    searchKey: 'matchMode',
  },
  {
    key: 'startCash',
    label: 'Start cash',
    sortKey: 'startCash',
    initialSortDirection: 'desc',
    value: (replay) => replay.startCash?.toLocaleString() ?? '',
    defaultVisible: false,
    searchKey: 'startCash',
  },
  {
    key: 'gentoolVersion',
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
    label: 'RepInfo',
    sortKey: 'repInfoInUse',
    value: (replay) => replay.repInfoInUse ?? '',
    defaultVisible: false,
    searchKey: 'repInfoInUse',
  },
  {
    key: 'replaySize',
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
    <DataTab<Replay>
      columns={REPLAY_COLUMNS}
      columnWidthsKey="replays"
      defaultSortKey="matchAt"
      defaultSortDirection="desc"
      active={active}
      filterKey={targetReplayId ?? targetPlayerId ?? (myReplaysOnly ? linkedPlayerIds.join(',') : null)}
      toolbarFilters={(
        <>
          {targetReplayId && (
            <Badge variant="outline" className="h-7 gap-1 pl-2 font-mono font-normal">
              Replay: {targetReplayId}
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
      load={(page, size, q, field, sort, direction) => consoleApi.listReplays(
        page,
        size,
        q,
        field,
        sort,
        direction,
        targetReplayId ? undefined : targetPlayerId ? [targetPlayerId] : myReplaysOnly ? linkedPlayerIds : undefined,
        targetReplayId ?? undefined,
      )}
      expand={(replay) => <ReplayDetails replay={replay} />}
      initialExpandedId={targetReplayId}
      emptyLabel={targetReplayId ? 'Replay not found.' : 'No replays collected.'}
      sortLabel="match time"
      forceLoading={forceLoading}
      onError={onError}
    />
  )
}

function ReplayDetails({ replay }: { replay: Replay }) {
  const teams = Map.groupBy(replay.players, (player) => player.teamNumber)
  const parsedFields = Object.entries(replay.fields)
    .filter(([label]) => label !== 'Associated files')
  return (
    <div className="grid gap-5 bg-muted/20 p-4 text-sm">
      <div className="grid gap-4 lg:grid-cols-3">
        <section className="grid content-start gap-2">
          <h3 className="font-medium">Teams</h3>
          {[...teams.entries()].map(([team, players]) => (
            <div key={team} className="grid grid-cols-[4rem_1fr] gap-2 border-t pt-2 first:border-0 first:pt-0">
              <span className="text-muted-foreground">Team {team}</span>
              <div className="grid gap-1">
                {players.map((player) => (
                  <div key={`${player.teamNumber}-${player.slotNumber}`}>
                    <span className="font-medium">{player.name}</span>
                    <span className="text-muted-foreground"> · {player.army ?? 'Unknown army'} · {player.address}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </section>

        <section className="grid content-start gap-2 lg:col-span-2">
          <h3 className="font-medium">System</h3>
          <pre className="whitespace-pre-wrap break-words font-mono text-xs text-muted-foreground">{replay.systemInfo ?? 'Not reported'}</pre>
        </section>
      </div>

      <section className="grid gap-2">
        <h3 className="font-medium">All parsed fields</h3>
        <dl className="grid gap-x-4 gap-y-2 sm:grid-cols-[max-content_1fr]">
          {parsedFields.map(([label, value]) => (
            <div key={label} className="contents">
              <dt className="text-muted-foreground">{label}</dt>
              <dd className="whitespace-pre-wrap break-words">{value || '—'}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section className="grid gap-2">
        <h3 className="font-medium">Associated files</h3>
        <div className="grid gap-1">
          {replay.associatedFiles.map((file) => (
            <a
              key={file.name}
              href={associatedFileUrl(replay.sourceUrl, file)}
              target="_blank"
              rel="noreferrer"
              className="flex w-fit items-center gap-1 text-primary hover:underline"
            >
              {file.name} <span className="text-muted-foreground">({formatBytes(file.sizeBytes)})</span><ExternalLink className="size-3" />
            </a>
          ))}
        </div>
      </section>

      <details>
        <summary className="cursor-pointer font-medium">Raw replay text</summary>
        <pre className="mt-2 max-h-96 overflow-auto whitespace-pre-wrap break-words rounded-md border bg-background p-3 font-mono text-xs">{replay.rawText}</pre>
      </details>

      <a href={replay.sourceUrl} target="_blank" rel="noreferrer" className="flex w-fit items-center gap-1 text-primary hover:underline">
        Open source text <ExternalLink className="size-3" />
      </a>
    </div>
  )
}

function associatedFileUrl(sourceUrl: string, file: ReplayAssociatedFile): string {
  return `${sourceUrl.slice(0, sourceUrl.lastIndexOf('/') + 1)}${encodeURIComponent(file.name)}`
}