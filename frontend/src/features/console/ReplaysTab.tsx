'use client'

import { ExternalLink } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
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
    headClassName: 'w-[170px]',
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
    searchKey: 'reporterName',
  },
  {
    key: 'players',
    label: 'Players',
    sortKey: 'players',
    value: playerNames,
    cellClassName: 'truncate',
    searchKey: 'player',
  },
  {
    key: 'mapName',
    label: 'Map',
    sortKey: 'mapName',
    value: (replay) => replay.mapName ?? '',
    render: (replay) => replay.mapName ?? '—',
    cellClassName: 'truncate',
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
  },
  {
    key: 'startCash',
    label: 'Start cash',
    sortKey: 'startCash',
    initialSortDirection: 'desc',
    value: (replay) => replay.startCash?.toLocaleString() ?? '',
    defaultVisible: false,
  },
  {
    key: 'gentoolVersion',
    label: 'GenTool',
    sortKey: 'gentoolVersion',
    value: (replay) => replay.gentoolVersion ?? '',
    defaultVisible: false,
  },
  {
    key: 'gameVersion',
    label: 'Game version',
    sortKey: 'gameVersion',
    value: (replay) => replay.gameVersion ?? '',
    cellClassName: 'truncate',
    defaultVisible: false,
  },
  {
    key: 'windowsCompat',
    label: 'Windows',
    sortKey: 'windowsCompat',
    value: (replay) => replay.windowsCompat ?? '',
    defaultVisible: false,
  },
  {
    key: 'repInfoInUse',
    label: 'RepInfo',
    sortKey: 'repInfoInUse',
    value: (replay) => replay.repInfoInUse ?? '',
    defaultVisible: false,
  },
  {
    key: 'replaySize',
    label: 'Replay size',
    sortKey: 'replaySize',
    initialSortDirection: 'desc',
    value: (replay) => formatBytes(replay.replaySizeBytes),
    defaultVisible: false,
  },
  {
    key: 'sourceDate',
    label: 'Source date',
    sortKey: 'sourceDate',
    initialSortDirection: 'desc',
    value: (replay) => replay.sourceDate,
    defaultVisible: false,
  },
  {
    key: 'collectedAt',
    label: 'Collected',
    sortKey: 'collectedAt',
    initialSortDirection: 'desc',
    value: (replay) => replay.collectedAt,
    render: (replay) => formatTime(replay.collectedAt),
    defaultVisible: false,
  },
]

export function ReplaysTab({
  active = true,
  forceLoading,
  onError,
}: {
  active?: boolean
  forceLoading: boolean
  onError: (message: string) => void
}) {
  return (
    <DataTab<Replay>
      columns={REPLAY_COLUMNS}
      columnWidthsKey="replays"
      defaultSortKey="matchAt"
      defaultSortDirection="desc"
      active={active}
      columnSelection
      rowKey={(replay) => replay.id}
      load={(page, size, q, field, sort, direction) => consoleApi.listReplays(page, size, q, field, sort, direction)}
      expand={(replay) => <ReplayDetails replay={replay} />}
      emptyLabel="No replays collected."
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