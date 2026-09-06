'use client'

import { useCallback, useEffect, useRef, useState } from 'react'
import { Loader2, Play, RotateCw } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { consoleApi } from './api'
import type { ReplayCollectionJob, ReplayCollectionStatus } from './types'

const STATUS_VARIANT: Record<ReplayCollectionStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  QUEUED: 'secondary',
  RUNNING: 'default',
  COMPLETED: 'outline',
  FAILED: 'destructive',
}

function utcDate(daysAgo: number): string {
  const date = new Date()
  date.setUTCDate(date.getUTCDate() - daysAgo)
  return date.toISOString().slice(0, 10)
}

export function ReplayCollectionTab({
  active = true,
  canRun,
  forceLoading,
  onError,
}: {
  active?: boolean
  canRun: boolean
  forceLoading: boolean
  onError: (message: string) => void
}) {
  const [jobs, setJobs] = useState<ReplayCollectionJob[]>([])
  const [startDate, setStartDate] = useState(() => utcDate(1))
  const [endDate, setEndDate] = useState(() => utcDate(1))
  const [userLimit, setUserLimit] = useState('')
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [starting, setStarting] = useState(false)
  const hasLoaded = useRef(false)
  const wasActive = useRef(false)

  const load = useCallback(async (silent = false) => {
    if (silent) setRefreshing(true)
    else setLoading(true)
    try {
      setJobs(await consoleApi.listReplayCollectionJobs())
    } catch {
      onError('Failed to load replay collection jobs.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [onError])

  useEffect(() => {
    if (forceLoading || !active) {
      wasActive.current = false
      return
    }
    if (!wasActive.current) {
      void load(hasLoaded.current)
      hasLoaded.current = true
    }
    wasActive.current = true
  }, [active, forceLoading, load])

  const jobActive = jobs.some((job) => job.status === 'QUEUED' || job.status === 'RUNNING')
  useEffect(() => {
    if (!jobActive) return
    const timer = window.setInterval(() => void load(true), 3000)
    return () => window.clearInterval(timer)
  }, [jobActive, load])

  const start = async () => {
    setStarting(true)
    try {
      await consoleApi.startReplayCollection(
        startDate,
        endDate,
        userLimit ? Number.parseInt(userLimit, 10) : undefined,
      )
      await load(true)
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to start replay collection.')
    } finally {
      setStarting(false)
    }
  }

  const showLoading = forceLoading || loading
  const invalidUserLimit = userLimit !== '' && Number.parseInt(userLimit, 10) < 1

  return (
    <div className="flex flex-col gap-4">
      {canRun && (
        <>
          <div className="flex flex-col gap-3 border-b pb-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="flex flex-wrap items-end gap-2">
            <label className="grid gap-1 text-xs text-muted-foreground">
              Start date (UTC)
              <Input
                type="date"
                className="h-8 w-40 text-foreground"
                value={startDate}
                max={utcDate(0)}
                onChange={(event) => setStartDate(event.target.value)}
              />
            </label>
            <label className="grid gap-1 text-xs text-muted-foreground">
              User limit (optional)
              <Input
                type="number"
                className="h-8 w-36 text-foreground"
                value={userLimit}
                min={1}
                step={1}
                placeholder="All users"
                onChange={(event) => setUserLimit(event.target.value)}
              />
            </label>
            <label className="grid gap-1 text-xs text-muted-foreground">
              End date (UTC)
              <Input
                type="date"
                className="h-8 w-40 text-foreground"
                value={endDate}
                min={startDate}
                max={utcDate(0)}
                onChange={(event) => setEndDate(event.target.value)}
              />
            </label>
            <Button
              size="sm"
              className="h-8"
              disabled={starting || jobActive || !startDate || !endDate || endDate < startDate || invalidUserLimit}
              onClick={start}
            >
              {starting ? <Loader2 className="animate-spin" /> : <Play />}
              Run
            </Button>
          </div>
            <Button
              variant="ghost"
              size="sm"
              className="self-end"
              disabled={showLoading || refreshing}
              onClick={() => void load(true)}
            >
              <RotateCw className={refreshing ? 'animate-spin' : ''} />
              Refresh
            </Button>
          </div>

          <div className="overflow-x-auto rounded-lg border">
        <Table className="min-w-[900px] table-fixed">
          <TableHeader>
            <TableRow className="bg-muted/40">
              <TableHead className="w-28">Status</TableHead>
              <TableHead className="w-48">Date range</TableHead>
              <TableHead className="w-32">Target</TableHead>
              <TableHead className="w-36">Directories</TableHead>
              <TableHead className="w-20">Limit</TableHead>
              <TableHead className="w-28">Discovered</TableHead>
              <TableHead className="w-24">Imported</TableHead>
              <TableHead className="w-24">Skipped</TableHead>
              <TableHead className="w-20">Failed</TableHead>
              <TableHead>Started</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {showLoading && Array.from({ length: 5 }).map((_, index) => (
              <TableRow key={index}>
                {Array.from({ length: 10 }).map((__, cell) => (
                  <TableCell key={cell}><Skeleton className="h-4 w-4/5" /></TableCell>
                ))}
              </TableRow>
            ))}
            {!showLoading && jobs.map((job) => (
              <TableRow key={job.id}>
                <TableCell><Badge variant={STATUS_VARIANT[job.status]}>{job.status}</Badge></TableCell>
                <TableCell className="font-mono text-xs">
                  {job.startDate === job.endDate ? job.startDate : `${job.startDate} to ${job.endDate}`}
                </TableCell>
                <TableCell className="font-mono text-xs">{job.targetPlayerId ?? 'All users'}</TableCell>
                <TableCell>{job.directoriesScanned.toLocaleString()} / {job.directoriesDiscovered.toLocaleString()}</TableCell>
                <TableCell>{job.userLimit?.toLocaleString() ?? 'All'}</TableCell>
                <TableCell>{job.filesDiscovered.toLocaleString()}</TableCell>
                <TableCell>{job.filesImported.toLocaleString()}</TableCell>
                <TableCell>{job.filesSkipped.toLocaleString()}</TableCell>
                <TableCell className={job.failures ? 'text-destructive' : undefined}>{job.failures.toLocaleString()}</TableCell>
                <TableCell className="text-xs text-muted-foreground">
                  {job.startedAt ? new Date(job.startedAt).toLocaleString() : job.triggerType}
                </TableCell>
              </TableRow>
            ))}
            {!showLoading && jobs.length === 0 && (
              <TableRow><TableCell colSpan={10} className="h-24 text-center text-muted-foreground">No collection jobs</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
          </div>
        </>
      )}
    </div>
  )
}